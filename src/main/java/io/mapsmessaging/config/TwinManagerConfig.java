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
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.VehicleClass;
import io.mapsmessaging.dto.rest.config.twin.MavlinkTwinConfigDTO;
import io.mapsmessaging.dto.rest.config.twin.TwinPublishConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.state.mavlink.MavlinkStateSubscriptionMode;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TwinManagerConfig extends TwinManagerConfigDTO implements Config, ConfigManager {

  private TwinManagerConfig(ConfigurationProperties properties) {
    this.heartbeatTimeoutMillis = properties.getLongProperty("heartbeatTimeoutMillis", heartbeatTimeoutMillis);
    this.staleTimeoutMillis = properties.getLongProperty("staleTimeoutMillis", staleTimeoutMillis);
    this.retentionTimeoutMillis = properties.getLongProperty("retentionTimeoutMillis", retentionTimeoutMillis);
    this.removeExpiredTwins = properties.getBooleanProperty("removeExpiredTwins", removeExpiredTwins);
    this.defaultRootPath = properties.getProperty("defaultRootPath", defaultRootPath);

    if (properties.containsKey("tak")) {
      ConfigurationProperties takProps = (ConfigurationProperties) properties.get("tak");
      TakProtocolDTO takProtocolDTO = new TakProtocolDTO();
      takProtocolDTO.setHostname(takProps.getProperty("hostname", null));
      takProtocolDTO.setPort(takProps.getIntProperty("port", takProtocolDTO.getPort()));
      takProtocolDTO.setSharedConnection(takProps.getBooleanProperty("sharedConnection", takProtocolDTO.isSharedConnection()));
      takProtocolDTO.setTopic(takProps.getProperty("topic", null));
      this.tak = takProtocolDTO;
    }

    if (properties.containsKey("publish")) {
      ConfigurationProperties publishProps = (ConfigurationProperties) properties.get("publish");
      TwinPublishConfigDTO publishConfig = new TwinPublishConfigDTO();
      publishConfig.setEnabled(publishProps.getBooleanProperty("enabled", publishConfig.isEnabled()));
      publishConfig.setTopicTemplate(publishProps.getProperty("topicTemplate", publishConfig.getTopicTemplate()));
      this.publish = publishConfig;
    }

    if (properties.containsKey("mavlink")) {
      this.mavlink = parseMavlinkConfigs(properties.get("mavlink"));
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

    if (this.tak != null) {
      ConfigurationProperties takProps = new ConfigurationProperties();
      takProps.put("hostname", this.tak.getHostname());
      takProps.put("port", this.tak.getPort());
      takProps.put("sharedConnection", this.tak.isSharedConnection());
      takProps.put("topic", this.tak.getTopic());
      props.put("tak", takProps);
    }

    if (this.publish != null) {
      ConfigurationProperties publishProps = new ConfigurationProperties();
      publishProps.put("enabled", this.publish.isEnabled());
      publishProps.put("topicTemplate", this.publish.getTopicTemplate());
      props.put("publish", publishProps);
    }

    if (this.mavlink != null && !this.mavlink.isEmpty()) {
      props.put("mavlink", toMavlinkConfigurationProperties(this.mavlink));
    }

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

    if (this.tak != newConfig.getTak()) {
      this.tak = newConfig.getTak();
      hasChanged = true;
    }

    if (this.publish != newConfig.getPublish()) {
      this.publish = newConfig.getPublish();
      hasChanged = true;
    }

    if (this.mavlink != newConfig.getMavlink()) {
      this.mavlink = newConfig.getMavlink();
      hasChanged = true;
    }

    return hasChanged;
  }

  private List<MavlinkTwinConfigDTO> parseMavlinkConfigs(Object value) {
    List<MavlinkTwinConfigDTO> configs = new ArrayList<>();

    if (value instanceof ConfigurationProperties entry) {
      configs.add(parseMavlinkConfig(entry));
    }
    else if(value instanceof List<?> list) {
      for (Object entry : list) {
        if (entry instanceof ConfigurationProperties mavlinkProps) {
          configs.add(parseMavlinkConfig(mavlinkProps));
        }
      }
    }
    return configs;
  }

  private MavlinkTwinConfigDTO parseMavlinkConfig(ConfigurationProperties properties) {
    MavlinkTwinConfigDTO config = new MavlinkTwinConfigDTO();
    config.setName(properties.getProperty("name", config.getName()));
    config.setTopic(properties.getProperty("topic", config.getTopic()));
    config.setDialectName(properties.getProperty("dialectName", config.getDialectName()));
    config.setSubscriptionMode(parseSubscriptionMode(properties.getProperty("subscriptionMode", config.getSubscriptionMode().name())));

    if (properties.containsKey("knownSources")) {
      config.setKnownSources(parseKnownSources(properties.get("knownSources")));
    }

    return config;
  }

  private List<MavlinkKnownSourceDTO> parseKnownSources(Object value) {
    List<MavlinkKnownSourceDTO> knownSources = new ArrayList<>();

    if (!(value instanceof List<?> entries)) {
      return knownSources;
    }

    for (Object entry : entries) {
      if (entry instanceof ConfigurationProperties sourceProps) {
        knownSources.add(parseKnownSource(sourceProps));
      }
    }

    return knownSources;
  }

  private MavlinkKnownSourceDTO parseKnownSource(ConfigurationProperties properties) {
    MavlinkKnownSourceDTO knownSource = new MavlinkKnownSourceDTO();
    knownSource.setName(properties.getProperty("name", knownSource.getName()));
    knownSource.setDescription(properties.getProperty("description", knownSource.getDescription()));
    knownSource.setSystemId(properties.getIntProperty("systemId", knownSource.getSystemId()));
    knownSource.setComponentId(properties.getIntProperty("componentId", knownSource.getComponentId()));
    knownSource.setVehicleClass(parseVehicleClass(properties.getProperty("vehicleClass", null), knownSource.getVehicleClass()));

    return knownSource;
  }

  private MavlinkStateSubscriptionMode parseSubscriptionMode(String value) {
    if (value == null || value.isBlank()) {
      return MavlinkStateSubscriptionMode.TWIN;
    }

    return MavlinkStateSubscriptionMode.valueOf(value.trim().toUpperCase());
  }

  private VehicleClass parseVehicleClass(String value, VehicleClass defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    return VehicleClass.valueOf(value.trim().toUpperCase());
  }

  private List<ConfigurationProperties> toMavlinkConfigurationProperties(List<MavlinkTwinConfigDTO> configs) {
    List<ConfigurationProperties> values = new ArrayList<>();

    for (MavlinkTwinConfigDTO config : configs) {
      ConfigurationProperties properties = new ConfigurationProperties();
      properties.put("name", config.getName());
      properties.put("topic", config.getTopic());
      properties.put("dialectName", config.getDialectName());
      properties.put("subscriptionMode", config.getSubscriptionMode().name());

      if (config.getKnownSources() != null && !config.getKnownSources().isEmpty()) {
        properties.put("knownSources", toKnownSourceConfigurationProperties(config.getKnownSources()));
      }

      values.add(properties);
    }

    return values;
  }

  private List<ConfigurationProperties> toKnownSourceConfigurationProperties(List<MavlinkKnownSourceDTO> knownSources) {
    List<ConfigurationProperties> values = new ArrayList<>();

    for (MavlinkKnownSourceDTO knownSource : knownSources) {
      ConfigurationProperties properties = new ConfigurationProperties();
      properties.put("name", knownSource.getName());
      properties.put("description", knownSource.getDescription());
      properties.put("systemId", knownSource.getSystemId());
      properties.put("componentId", knownSource.getComponentId());

      if (knownSource.getVehicleClass() != null) {
        properties.put("vehicleClass", knownSource.getVehicleClass().name());
      }

      values.add(properties);
    }

    return values;
  }
}