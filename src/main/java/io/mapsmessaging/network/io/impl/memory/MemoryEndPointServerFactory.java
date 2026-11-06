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

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemoryConfigDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;

import java.io.IOException;

public abstract class MemoryEndPointServerFactory implements EndPointServerFactory {
  private final String type;

  protected MemoryEndPointServerFactory(String type) {
    this.type = type;
  }

  @Override
  public EndPointServer instance(EndPointURL url, SelectorLoadManager selectors, AcceptHandler accept, EndPointServerConfigDTO config, EndPointManagerJMX management) throws IOException {
    if (selectors == null || !(config.getEndPointConfig() instanceof MemoryConfigDTO memory) || !type.equals(memory.getType()) || !type.equals(url.getProtocol())) {
      throw new IOException("Memory listener requires matching memory configuration and selector threads");
    }
    return new MemoryEndPointServer(accept, url, config, selectors, management);
  }

  @Override
  public String getName() {
    return type;
  }

  @Override
  public String getDescription() {
    return type + " managed memory listener factory";
  }

  @Override
  public boolean active() {
    return true;
  }
}
