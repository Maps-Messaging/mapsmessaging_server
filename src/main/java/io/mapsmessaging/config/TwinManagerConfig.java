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

package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.TwinManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.TwinPublishConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor
public class TwinManagerConfig extends TwinManagerConfigDTO implements Config, ConfigManager {

  private TwinManagerConfig(ConfigurationProperties properties) {
    this.heartbeatTimeoutMillis = properties.getLongProperty("heartbeatTimeoutMillis", heartbeatTimeoutMillis);
    this.staleTimeoutMillis = properties.getLongProperty("staleTimeoutMillis", staleTimeoutMillis);
    this.retentionTimeoutMillis = properties.getLongProperty("retentionTimeoutMillis", retentionTimeoutMillis);
    this.removeExpiredTwins = properties.getBooleanProperty("removeExpiredTwins", removeExpiredTwins);
    this.defaultRootPath = properties.getProperty("defaultRootPath", defaultRootPath);
    if(properties.containsKey("tak")){
      ConfigurationProperties takProps = (ConfigurationProperties) properties.get("tak");
      TakProtocolDTO takProtocolDTO = new TakProtocolDTO();
      takProtocolDTO.setHostname(takProps.getProperty("hostname", null));
      takProtocolDTO.setPort(takProps.getIntProperty("port", takProtocolDTO.getPort()));
      takProtocolDTO.setSharedConnection(takProps.getBooleanProperty("sharedConnection", takProtocolDTO.isSharedConnection()));
      takProtocolDTO.setTopic(takProps.getProperty("topic", null));
      this.tak = takProtocolDTO;
    }
    if(properties.containsKey("publish")){
      ConfigurationProperties publishProps = (ConfigurationProperties) properties.get("publish");
      TwinPublishConfigDTO publishConfig = new TwinPublishConfigDTO();
      publishConfig.setEnabled(publishProps.getBooleanProperty("enabled", publishConfig.isEnabled()));
      publishConfig.setTopicTemplate(publishProps.getProperty("topicTemplate", publishConfig.getTopicTemplate()));
      this.publish = publishConfig;
    }
  }

  public static TwinManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(TwinManagerConfig.class);
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new TwinManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "TwinManager";
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();

    props.put("heartbeatTimeoutMillis", this.heartbeatTimeoutMillis);
    props.put("staleTimeoutMillis", this.staleTimeoutMillis);
    props.put("retentionTimeoutMillis", this.retentionTimeoutMillis);
    props.put("removeExpiredTwins", this.removeExpiredTwins);
    props.put("defaultRootPath", this.defaultRootPath);

    return props;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof TwinManagerConfigDTO newConfig)) {
      return false;
    }

    boolean hasChanged = false;

    if (this.heartbeatTimeoutMillis != newConfig.getHeartbeatTimeoutMillis()) {
      this.heartbeatTimeoutMillis = newConfig.getHeartbeatTimeoutMillis();
      hasChanged = true;
    }

    if (this.staleTimeoutMillis != newConfig.getStaleTimeoutMillis()) {
      this.staleTimeoutMillis = newConfig.getStaleTimeoutMillis();
      hasChanged = true;
    }

    if (this.retentionTimeoutMillis != newConfig.getRetentionTimeoutMillis()) {
      this.retentionTimeoutMillis = newConfig.getRetentionTimeoutMillis();
      hasChanged = true;
    }

    if (this.removeExpiredTwins != newConfig.isRemoveExpiredTwins()) {
      this.removeExpiredTwins = newConfig.isRemoveExpiredTwins();
      hasChanged = true;
    }

    String newDefaultRootPath = newConfig.getDefaultRootPath();
    if (newDefaultRootPath == null) {
      newDefaultRootPath = "/";
    }

    if (this.defaultRootPath == null || !this.defaultRootPath.equals(newDefaultRootPath)) {
      this.defaultRootPath = newDefaultRootPath;
      hasChanged = true;
    }

    return hasChanged;
  }
}