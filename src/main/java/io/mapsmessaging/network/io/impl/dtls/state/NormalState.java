/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class NormalState extends State {

  public NormalState(StateEngine stateEngine) {
    super(stateEngine);
  }

  @Override
  int outbound(Packet packet) throws IOException {
    int packetBufferSize = stateEngine.getSslEngine().getSession().getPacketBufferSize();
    ByteBuffer appNet = ByteBuffer.allocate(packetBufferSize);
    stateEngine.getSslEngine().wrap(packet.getRawBuffer(), appNet);
    appNet.flip();
    Packet p = new Packet(appNet);
    p.setFromAddress(stateEngine.getClientId());
    return stateEngine.send(p);
  }

  @Override
  int inbound(Packet packet) throws SSLException {
    int applicationBufferSize = stateEngine.getSslEngine().getSession().getApplicationBufferSize();
    Packet application = new Packet(Math.max(1, applicationBufferSize), false);

    while (packet.hasRemaining()) {
      SSLEngineResult result = stateEngine.getSslEngine().unwrap(
          packet.getRawBuffer(),
          application.getRawBuffer());

      if (result.getStatus() == Status.BUFFER_OVERFLOW) {
        application = growApplicationBuffer(application);
        continue;
      }

      if (result.getStatus() == Status.BUFFER_UNDERFLOW
          || result.getStatus() == Status.CLOSED) {
        break;
      }

      if (result.getStatus() == Status.OK
          && result.bytesConsumed() == 0
          && result.bytesProduced() == 0) {
        break;
      }
    }

    if (application.position() > 0) {
      application.flip();
      application.setFromAddress(packet.getFromAddress());
      stateEngine.pushToInBoundQueue(application);
    }

    return packet.position();
  }

  private Packet growApplicationBuffer(Packet current) {
    int nextCapacity = Math.max(
        current.capacity() * 2,
        stateEngine.getSslEngine().getSession().getApplicationBufferSize());
    Packet expanded = new Packet(nextCapacity, false);
    current.flip();
    expanded.put(current);
    return expanded;
  }
}
