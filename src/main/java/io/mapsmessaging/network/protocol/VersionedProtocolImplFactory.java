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

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class VersionedProtocolImplFactory extends ProtocolImplFactory {

  private static final AtomicLong index = new AtomicLong(0);

  private final String transportType;
  private final List<ProtocolImplFactory> protocolFactories;

  protected VersionedProtocolImplFactory(String name, String description, List<ProtocolImplFactory> factories) {
    super(name, description, null);
    this.protocolFactories = factories;
    transportType = factories.getFirst().getTransportType();
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    int idx = (int) (index.incrementAndGet() % protocolFactories.size());
    return protocolFactories.get(idx).connect(endPoint, sessionId, username, password);
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {
    throw new IOException("Not applicable for inter server connections");
  }

  @Override
  public String getTransportType() {
    return transportType;
  }
}
