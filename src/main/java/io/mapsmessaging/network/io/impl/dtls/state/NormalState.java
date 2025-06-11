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
    ByteBuffer appNet = ByteBuffer.allocate(32768);
    SSLEngineResult r = stateEngine.getSslEngine().wrap(packet.getRawBuffer(), appNet);
    appNet.flip();
    Packet p = new Packet(appNet);
    p.setFromAddress(stateEngine.getClientId());
    return stateEngine.send(p);
  }

  @Override
  int inbound(Packet packet) throws SSLException {
    Packet networkOut = new Packet(2048, false);
    SSLEngineResult rs;
    do {
      rs = stateEngine.getSslEngine().unwrap(packet.getRawBuffer(), networkOut.getRawBuffer());
    } while (rs.getStatus() == Status.OK && packet.hasRemaining());

    if (rs.getStatus() == Status.OK) {
      networkOut.flip();
      networkOut.setFromAddress(packet.getFromAddress());
      stateEngine.pushToInBoundQueue(networkOut);
    }
    return packet.position();
  }
}
