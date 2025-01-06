/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol.impl.apache_pulsar;

import java.io.IOException;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.NoOpDetection;

/**
 * This class is a proof of concept on how the server could connect to other
 * messaging servers that do not support open messaging protocols but do have
 * a java API that can be used to connect, subscribe and publish.
 *
 * This is not production ready, its a POC.
 */
public class PulsarProtocolFactory extends ProtocolImplFactory {

  public PulsarProtocolFactory(){
    super("pulsar", "Provides a connection an apache Pulsar server", new NoOpDetection());
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    ProtocolImpl protocol = new PulsarProtocol(endPoint);
    protocol.connect(sessionId, username, password);
    return protocol;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {
    // We don't accept incoming pulsar client connections
  }
}
