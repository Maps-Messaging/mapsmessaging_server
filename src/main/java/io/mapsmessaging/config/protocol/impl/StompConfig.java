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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.StompConfigDTO;

public class StompConfig extends StompConfigDTO implements Config {

  public StompConfig(ConfigurationProperties config) {
    setType("stomp");
    ProtocolConfigFactory.unpack(config, this);

    maxBufferSize = config.getIntProperty("maximumBufferSize", maxBufferSize);
    maxReceive = config.getIntProperty("maximumReceive", maxReceive);
    base64EncodeBinary = config.getBooleanProperty("base64EncodeBinary", base64EncodeBinary);
    heartbeatCanSendMillis =
        config.getIntProperty("heartbeatCanSendMillis", heartbeatCanSendMillis);
    heartbeatWantsReceiveMillis =
        config.getIntProperty("heartbeatWantsReceiveMillis", heartbeatWantsReceiveMillis);
    heartbeatToleranceMillis =
        config.getIntProperty("heartbeatToleranceMillis", heartbeatToleranceMillis);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof StompConfigDTO newConfig) {
      if (maxBufferSize != newConfig.getMaxBufferSize()) {
        maxBufferSize = newConfig.getMaxBufferSize();
        hasChanged = true;
      }
      if (maxReceive != newConfig.getMaxReceive()) {
        maxReceive = newConfig.getMaxReceive();
        hasChanged = true;
      }
      if (base64EncodeBinary != newConfig.isBase64EncodeBinary()) {
        base64EncodeBinary = newConfig.isBase64EncodeBinary();
        hasChanged = true;
      }
      if (heartbeatCanSendMillis != newConfig.getHeartbeatCanSendMillis()) {
        heartbeatCanSendMillis = newConfig.getHeartbeatCanSendMillis();
        hasChanged = true;
      }
      if (heartbeatWantsReceiveMillis != newConfig.getHeartbeatWantsReceiveMillis()) {
        heartbeatWantsReceiveMillis = newConfig.getHeartbeatWantsReceiveMillis();
        hasChanged = true;
      }
      if (heartbeatToleranceMillis != newConfig.getHeartbeatToleranceMillis()) {
        heartbeatToleranceMillis = newConfig.getHeartbeatToleranceMillis();
        hasChanged = true;
      }
      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("maximumBufferSize", maxBufferSize);
    properties.put("maximumReceive", maxReceive);
    properties.put("base64EncodeBinary", base64EncodeBinary);
    properties.put("heartbeatCanSendMillis", heartbeatCanSendMillis);
    properties.put("heartbeatWantsReceiveMillis", heartbeatWantsReceiveMillis);
    properties.put("heartbeatToleranceMillis", heartbeatToleranceMillis);
    return properties;
  }
}
