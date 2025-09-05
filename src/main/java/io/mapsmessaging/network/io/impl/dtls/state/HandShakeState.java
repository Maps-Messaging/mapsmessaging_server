/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.impl.dtls.state;

import io.mapsmessaging.network.io.Packet;

import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

public class HandShakeState extends State {

  public HandShakeState(StateEngine stateEngine) {
    super(stateEngine);
  }

  @Override
  int outbound(Packet packet) {
    return 0;
  }

  @Override
  int inbound(Packet packet) throws IOException {
    handshake(packet);
    while (stateEngine.getSslEngine().getHandshakeStatus() != NEED_UNWRAP) {
      handshake(packet);
      if (stateEngine.getSslEngine().getHandshakeStatus() == NOT_HANDSHAKING) {
        stateEngine.setCurrentState(new NormalState(stateEngine));
        stateEngine.handshakeComplete();
        return 0;
      }
    }
    return 0;
  }

  void handshake(Packet packet) throws IOException {
    SSLEngineResult.HandshakeStatus hs = stateEngine.getSslEngine().getHandshakeStatus();
    if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
        hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN) {
      ByteBuffer iApp = ByteBuffer.allocate(1024);
      SSLEngineResult r = stateEngine.getSslEngine().unwrap(packet.getRawBuffer(), iApp);
      SSLEngineResult.Status rs = r.getStatus();
      hs = r.getHandshakeStatus();
      switch (rs) {
        case OK:
          // No action required
          break;

        case BUFFER_OVERFLOW:
          throw new IOException("Buffer overflow: incorrect client maximum fragment size");

        case BUFFER_UNDERFLOW:
          if (hs != NOT_HANDSHAKING) {
            throw new IOException("Buffer underflow: incorrect client maximum fragment size");
          }
          // Ignore this packet if not handshaking
          break;

        case CLOSED:
          throw new IOException("SSL engine closed, handshake status is " + hs);

        default:
          throw new IOException("Unexpected result: " + rs);
      }

      if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
        return;
      }
    }
    processPackets(hs);
  }

  private void processPackets(SSLEngineResult.HandshakeStatus hs) throws IOException {
    while (hs == NEED_WRAP) {
      List<Packet> packets = new ArrayList<>();
      produceHandshakePackets(packets);
      for (Packet p : packets) {
        p.setFromAddress(stateEngine.getClientId());
        stateEngine.send(p);
        p.clear();
      }
      hs = stateEngine.getSslEngine().getHandshakeStatus();
    }

    if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
      runDelegatedTasks();
    } else if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
      throw new IOException("Unexpected status, SSLEngine.getHandshakeStatus() shouldn't return FINISHED");
    }
  }

  // produce handshake packets
  boolean produceHandshakePackets(List<Packet> packets) throws IOException {
    ByteBuffer oNet = ByteBuffer.allocate(32768);
    ByteBuffer oApp = ByteBuffer.allocate(0);
    SSLEngineResult r = stateEngine.getSslEngine().wrap(oApp, oNet);
    oNet.flip();

    SSLEngineResult.Status rs = r.getStatus();
    SSLEngineResult.HandshakeStatus hs = r.getHandshakeStatus();
    if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
      // the client maximum fragment size config does not work?
      throw new IOException("Buffer overflow: incorrect server maximum fragment size");
    } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
      // bad packet, or the client maximum fragment size
      // config does not work?
      if (hs != NOT_HANDSHAKING) {
        throw new IOException("Buffer underflow: incorrect server maximum fragment size");
      } // otherwise, ignore this packet
    } else if (rs == SSLEngineResult.Status.CLOSED) {
      throw new IOException("SSLEngine has closed");
    } else if (rs != SSLEngineResult.Status.OK) {
      throw new IOException("Can't reach here, result is " + rs);
    }

    // SSLEngineResult.Status.OK:
    if (oNet.hasRemaining()) {
      Packet packet = new Packet(oNet);
      packets.add(packet);
    }
    if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
      return true;
    }
    processInnerLoop(hs);
    return false;
  }

  private void processInnerLoop(SSLEngineResult.HandshakeStatus hs) throws IOException {
    boolean endInnerLoop = false;
    SSLEngineResult.HandshakeStatus nhs = hs;
    while (!endInnerLoop) {
      if (nhs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
        runDelegatedTasks();
      } else if (nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
          nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN ||
          nhs == NOT_HANDSHAKING) {
        endInnerLoop = true;
      } else if (nhs == NEED_WRAP) {
        endInnerLoop = true;
      } else if (nhs == SSLEngineResult.HandshakeStatus.FINISHED) {
        throw new IOException("Unexpected status, SSLEngine.getHandshakeStatus() shouldn't return FINISHED");
      } else {
        throw new IOException("Can't reach here, handshake status is " + nhs);
      }
      nhs = stateEngine.getSslEngine().getHandshakeStatus();
    }
  }
}
