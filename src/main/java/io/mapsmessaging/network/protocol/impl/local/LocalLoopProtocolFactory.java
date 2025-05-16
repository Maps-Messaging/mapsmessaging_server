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

package io.mapsmessaging.network.protocol.impl.local;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.NoOpDetection;

import java.io.IOException;

public class LocalLoopProtocolFactory extends ProtocolImplFactory {

  public LocalLoopProtocolFactory() {
    super("loop", "Provides a connection to the messaging engine with the need for a network connection", new NoOpDetection());
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    Protocol protocol = new LocalLoopProtocol(endPoint);
    protocol.connect(sessionId, username, password);
    return protocol;
  }

  @Override
  public String getTransportType() {
    return "loop";
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {
    // We do not create an endpoint based on an incoming connection, since this is a local looped connection
  }
}
