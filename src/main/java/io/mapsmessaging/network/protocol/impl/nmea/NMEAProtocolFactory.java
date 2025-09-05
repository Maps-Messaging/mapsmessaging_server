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

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.tcp.TCPEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class NMEAProtocolFactory extends ProtocolImplFactory {

  public NMEAProtocolFactory() {
    super("NMEA-0183", "NMEA Gateway as per https://en.wikipedia.org/wiki/NMEA_0183", new NMEAProtocolDetection());
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    if (endPoint instanceof TCPEndPoint) {
      Packet packet = new Packet(ByteBuffer.allocate(256));
      packet.put("?WATCH={\"enable\":true,\"nmea\":true}".getBytes(StandardCharsets.UTF_8));
      packet.flip();
      endPoint.sendPacket(packet);
      return build(endPoint, packet);
    }
    return null;

  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    build(endPoint, packet);
  }

  private Protocol build(EndPoint endPoint, Packet packet) throws IOException {
    try {
      return new NMEAProtocol(endPoint, packet);
    } catch (LoginException e) {
      // Ignore since it should just work
    }
    return null;
  }

  @Override
  public String getTransportType() {
    return "serial";
  }

}