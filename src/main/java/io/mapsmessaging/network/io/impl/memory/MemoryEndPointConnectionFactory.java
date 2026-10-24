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

package io.mapsmessaging.network.io.impl.memory;

import io.mapsmessaging.dto.rest.config.network.impl.MemoryConfigDTO;
import io.mapsmessaging.memory.MemoryClient;
import io.mapsmessaging.memory.MemoryConnection;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectedCallback;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;

import java.io.IOException;
import java.util.List;

public abstract class MemoryEndPointConnectionFactory implements EndPointConnectionFactory {
  private final String type;

  protected MemoryEndPointConnectionFactory(String type) {
    this.type = type;
  }

  @Override
  public EndPoint connect(EndPointURL url, SelectorLoadManager selectors, EndPointConnectedCallback callback, EndPointServerStatus status, List<String> jmxPath) throws IOException {
    if (selectors == null || !(status.getConfig().getEndPointConfig() instanceof MemoryConfigDTO config) || !type.equals(config.getType())) {
      throw new IOException("Memory endpoint requires matching memory configuration and selector threads");
    }
    MemoryConnection connection = MemoryClient.connect(MemoryEndpointSettings.client(url, config));
    MemoryEndPoint endpoint = null;
    try {
      endpoint = new MemoryEndPoint(generateID(), connection, selectors.allocate(), status, jmxPath, config.getReadinessPollMicros());
      callback.connected(endpoint);
      return endpoint;
    } catch (IOException | RuntimeException failure) {
      try {
        if (endpoint == null) {
          connection.close();
        } else {
          endpoint.close();
        }
      } catch (IOException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  @Override
  public String getName() {
    return type;
  }

  @Override
  public String getDescription() {
    return type + " managed memory connection factory";
  }
}
