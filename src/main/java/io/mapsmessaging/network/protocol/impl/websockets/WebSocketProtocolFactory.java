/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.websockets;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.ByteArrayDetection;

import java.io.IOException;

public class WebSocketProtocolFactory extends ProtocolImplFactory {

  public WebSocketProtocolFactory() {
    super("WS", "WebSocket encapsulation layer as per https://tools.ietf.org/html/rfc6455", new ByteArrayDetection("GET".getBytes(), 0));
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;

  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new WebSocketProtocol(endPoint, packet);
  }

  @Override
  public String getTransportType() {
    return "tcp";
  }

}
