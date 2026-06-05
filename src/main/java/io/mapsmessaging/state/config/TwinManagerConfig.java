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

package io.mapsmessaging.state.config;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.VehicleClass;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.state.config.capability.*;
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

    if(properties.containsKey("droneInfo")) {
      this.droneInfo = parseDroneInfos(properties.get("droneInfo"));
    }

    if (properties.containsKey("mavlink")) {
      this.mavlink = parseMavlinkConfigs(properties.get("mavlink"));
    }

    if(properties.containsKey("stanag")) {
      ConfigurationProperties stanagProps = (ConfigurationProperties) properties.get("stanag");
      this.stanagConfig = new StanagConfig();
      stanagConfig.setChatTopic(stanagProps.getProperty("chatTopic", null));
      stanagConfig.setTaskTopic(stanagProps.getProperty("taskTopic", null));
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
    if (properties.containsKey("knownSources")) {
      config.setKnownSources(parseKnownSources(properties.get("knownSources")));
    }

    return config;
  }

  private List<MavlinkKnownSourceDTO> parseKnownSources(Object value) {
    List<MavlinkKnownSourceDTO> knownSources = new ArrayList<>();

    if (!(value instanceof List<?> entries)) {
      if (value instanceof ConfigurationProperties sourceProps) {
        knownSources.add(parseKnownSource(sourceProps));
      }
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


  private List<DroneInfo> parseDroneInfos(Object properties) {
    List<DroneInfo> droneInfos = new ArrayList<>();

    if (!(properties instanceof List<?> entries)) {
      if (properties instanceof ConfigurationProperties sourceProps) {
        droneInfos.add(parseDroneInfo(sourceProps));
      }
      return droneInfos;
    }

    for (Object entry : entries) {
      if (entry instanceof ConfigurationProperties sourceProps) {
        droneInfos.add(parseDroneInfo(sourceProps));
      }
    }
    return droneInfos;
  }

  private DroneInfo parseDroneInfo(ConfigurationProperties properties) {
    DroneInfo droneInfo = new DroneInfo();
    droneInfo.setName(properties.getProperty("name", droneInfo.getName()));
    droneInfo.setDescription( ((ConfigurationProperties)properties.get("description")).getMap());
    droneInfo.setCapabilities(parseTaskCapabilities(properties.get("capabilities")));
    return droneInfo;
  }



  @SuppressWarnings("unchecked")
  private TaskCapabilities parseTaskCapabilities(Object value) {
    TaskCapabilities capabilities = new TaskCapabilities();

    if(value instanceof ConfigurationProperties configurationProperties) {
      capabilities.setTasks(parseTaskCapabilityList(configurationProperties.get("tasks")));

      if(capabilities.getTasks().isEmpty()) {
        capabilities.setTasks(parseTaskCapabilityList(configurationProperties.get("task_capabilities")));
      }

      capabilities.setTaskConditionsMode(
          parseTaskConditionMode(
              configurationProperties.getProperty("task_conditions_mode", null),
              capabilities.getTaskConditionsMode()
          )
      );

      capabilities.setTaskConditionsTemplate(
          parseTaskTemplateMode(
              configurationProperties.getProperty("task_conditions_template", null),
              capabilities.getTaskConditionsTemplate()
          )
      );
    } else if(value instanceof List<?> list) {
      capabilities.setTasks(parseTaskCapabilityList(list));
    }

    return capabilities;
  }

  private List<TaskCapability> parseTaskCapabilityList(Object value) {
    List<TaskCapability> taskCapabilities = new ArrayList<>();

    if(value instanceof ConfigurationProperties configurationProperties) {
      taskCapabilities.add(parseTaskCapability(configurationProperties));
    } else if(value instanceof List<?> list) {
      for(Object entry : list) {
        if(entry instanceof ConfigurationProperties configurationProperties) {
          taskCapabilities.add(parseTaskCapability(configurationProperties));
        }
      }
    }

    return taskCapabilities;
  }

  private TaskCapability parseTaskCapability(ConfigurationProperties properties) {
    TaskCapability taskCapability = new TaskCapability();

    taskCapability.setTaskType(
        parsePlanTaskType(
            properties.getProperty("task_type", null),
            taskCapability.getTaskType()
        )
    );

    taskCapability.setSpecialization(
        parseTaskSpecialization(
            properties.getProperty("task_specialization", null),
            taskCapability.getSpecialization()
        )
    );

    return taskCapability;
  }

  private PlanTaskType parsePlanTaskType(String value, PlanTaskType defaultValue) {
    if(value == null || value.isBlank()) {
      return defaultValue;
    }

    String normalisedValue = removePrefix(value, "PlanTaskTypeEnum_");
    return PlanTaskType.valueOf(normalisedValue);
  }

  private TaskSpecialization parseTaskSpecialization(String value, TaskSpecialization defaultValue) {
    if(value == null || value.isBlank()) {
      return defaultValue;
    }

    String normalisedValue = removePrefix(value, "TaskSpecializationEnum_");
    return TaskSpecialization.valueOf(normalisedValue);
  }

  private TaskConditionMode parseTaskConditionMode(String value, TaskConditionMode defaultValue) {
    if(value == null || value.isBlank()) {
      return defaultValue;
    }

    String normalisedValue = removePrefix(value, "TaskConditionModeEnum_");
    return TaskConditionMode.valueOf(normalisedValue);
  }

  private TaskTemplateMode parseTaskTemplateMode(String value, TaskTemplateMode defaultValue) {
    if(value == null || value.isBlank()) {
      return defaultValue;
    }

    String normalisedValue = removePrefix(value, "TaskTemplateModeEnum_");
    return TaskTemplateMode.valueOf(normalisedValue);
  }

  private String removePrefix(String value, String prefix) {
    if(value.startsWith(prefix)) {
      return value.substring(prefix.length());
    }

    return value;
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