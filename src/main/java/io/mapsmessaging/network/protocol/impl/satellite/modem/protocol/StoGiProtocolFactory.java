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

package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.NoOpDetection;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class StoGiProtocolFactory extends ProtocolImplFactory {

  public StoGiProtocolFactory() {
    super("stogi", "OrbComm OGWS protocol", new NoOpDetection());
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return build(endPoint, new Packet(ByteBuffer.allocate(1024)));
  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    build(endPoint, packet);
  }

  private Protocol build(EndPoint endPoint, Packet packet) throws IOException {
    try {
      return new StoGiProtocol(endPoint, packet);
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