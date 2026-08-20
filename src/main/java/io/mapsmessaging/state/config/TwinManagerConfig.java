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
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.state.config.capability.*;
import io.mapsmessaging.state.config.geospatial.GeoSpatialConfigSupport;
import io.mapsmessaging.state.config.n2k.N2KAisConfig;
import io.mapsmessaging.state.config.n2k.N2KAisConfigDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import java.io.IOException;
import java.util.*;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TwinManagerConfig extends TwinManagerConfigDTO implements Config, ConfigManager {

  private static final String STATE_ADAPTERS_CONFIG_KEY = "stateAdapters";

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

    if (properties.containsKey("geospatial")) {
      this.geospatial = GeoSpatialConfigSupport.parse(properties.get("geospatial"));
    }

    if (properties.containsKey("publish")) {
      ConfigurationProperties publishProps = (ConfigurationProperties) properties.get("publish");
      TwinPublishConfigDTO publishConfig = new TwinPublishConfigDTO();
      publishConfig.setEnabled(publishProps.getBooleanProperty("enabled", publishConfig.isEnabled()));
      publishConfig.setTopicTemplate(publishProps.getProperty("topicTemplate", publishConfig.getTopicTemplate()));
      publishConfig.setPublishRateMs(publishProps.getLongProperty("publishRateMs", publishConfig.getPublishRateMs()));
      this.publish = publishConfig;
    }

    if (properties.containsKey("droneInfo")) {
      this.droneInfo = parseDroneInfos(properties.get("droneInfo"));
    }

    if (properties.containsKey("mavlink")) {
      this.mavlink = parseMavlinkConfigs(properties.get("mavlink"));
    }

    if (properties.containsKey(STATE_ADAPTERS_CONFIG_KEY)) {
      parseAdapterConfigs(properties.get(STATE_ADAPTERS_CONFIG_KEY));
    }

    n2KTwinConfig = new N2KTwinConfig();
    if (properties.containsKey("n2k")) {
      ConfigurationProperties n2kProps = (ConfigurationProperties) properties.get("n2k");
      n2KTwinConfig.setEnable(n2kProps.getBooleanProperty("enabled", n2KTwinConfig.isEnable()));
      n2KTwinConfig.setTopic(n2kProps.getProperty("topic", n2KTwinConfig.getTopic()));
      n2KTwinConfig.setName(n2kProps.getProperty("name", n2KTwinConfig.getName()));
      n2KTwinConfig.setVehicleClass(n2kProps.getProperty("vehicleClass", n2KTwinConfig.getVehicleClass()));
      n2KTwinConfig.setPublishMavlinkDrones(n2kProps.getBooleanProperty("publishMavlinkDrones", n2KTwinConfig.isPublishMavlinkDrones()));
      if (n2kProps.containsKey("ais")) {
        Object aisConfig = n2kProps.get("ais");
        if (aisConfig instanceof ConfigurationProperties aisProperties) {
          n2KTwinConfig.setAis(new N2KAisConfig(aisProperties));
        } else if (aisConfig instanceof N2KAisConfigDTO aisDto) {
          n2KTwinConfig.setAis(aisDto);
        }
      }
    } else {
      n2KTwinConfig.setEnable(false);
      n2KTwinConfig.setPublishMavlinkDrones(false);
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

    if (this.geospatial != null && this.geospatial.getAreas() != null && !this.geospatial.getAreas().isEmpty()) {
      props.put("geospatial", GeoSpatialConfigSupport.toConfigurationProperties(this.geospatial));
    }

    if (this.publish != null) {
      ConfigurationProperties publishProps = new ConfigurationProperties();
      publishProps.put("enabled", this.publish.isEnabled());
      publishProps.put("topicTemplate", this.publish.getTopicTemplate());
      publishProps.put("publishRateMs", this.publish.getPublishRateMs());
      props.put("publish", publishProps);
    }

    if (this.droneInfo != null && !this.droneInfo.isEmpty()) {
      props.put("droneInfo", toDroneInfoConfigurationProperties(this.droneInfo));
    }

    if (this.mavlink != null && !this.mavlink.isEmpty()) {
      props.put("mavlink", toMavlinkConfigurationProperties(this.mavlink));
    }

    if (this.adapterConfig != null && !this.adapterConfig.isEmpty()) {
      props.put(STATE_ADAPTERS_CONFIG_KEY, new ConfigurationProperties(new LinkedHashMap<>(this.adapterConfig)));
    }

    if (shouldWriteN2kConfiguration()) {
      ConfigurationProperties n2kProps = new ConfigurationProperties();
      n2kProps.put("enabled", this.n2KTwinConfig.isEnable());
      n2kProps.put("topic", this.n2KTwinConfig.getTopic());
      n2kProps.put("name", this.n2KTwinConfig.getName());
      n2kProps.put("vehicleClass", this.n2KTwinConfig.getVehicleClass());
      n2kProps.put("publishMavlinkDrones", this.n2KTwinConfig.isPublishMavlinkDrones());

      if (n2KTwinConfig.getAis() != null) {
        n2kProps.put("ais", N2KAisConfig.toConfigurationProperties(n2KTwinConfig.getAis()));
      }
      props.put("n2k", n2kProps);
    }

    return props;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof TwinManagerConfigDTO newConfig)) {
      return false;
    }

    boolean hasChanged = false;

    if (n2KTwinConfig.isPublishMavlinkDrones() != newConfig.n2KTwinConfig.isPublishMavlinkDrones()) {
      n2KTwinConfig.setPublishMavlinkDrones(newConfig.n2KTwinConfig.isPublishMavlinkDrones());
      hasChanged = true;
    }

    if (n2KTwinConfig.getAis() == null && newConfig.n2KTwinConfig.getAis() != null) {
      n2KTwinConfig.setAis(newConfig.getN2KTwinConfig().getAis());
      hasChanged = true;
    } else if (n2KTwinConfig.getAis() != null && !n2KTwinConfig.getAis().equals(newConfig.n2KTwinConfig.getAis())) {
      n2KTwinConfig.setAis(newConfig.n2KTwinConfig.getAis());
      hasChanged = true;
    }

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

    if (!Objects.equals(this.geospatial, newConfig.getGeospatial())) {
      this.geospatial = newConfig.getGeospatial();
      hasChanged = true;
    }

    if (this.publish != newConfig.getPublish()) {
      this.publish = newConfig.getPublish();
      hasChanged = true;
    }

    if (this.droneInfo != newConfig.getDroneInfo()) {
      this.droneInfo = newConfig.getDroneInfo();
      hasChanged = true;
    }

    if (this.mavlink != newConfig.getMavlink()) {
      this.mavlink = newConfig.getMavlink();
      hasChanged = true;
    }

    if (this.droneInfo != newConfig.getDroneInfo()) {
      this.droneInfo = newConfig.getDroneInfo();
      hasChanged = true;
    }

    if (newConfig.getAdapterConfig() != null && !this.adapterConfig.equals(newConfig.getAdapterConfig())) {
      this.adapterConfig = new LinkedHashMap<>(newConfig.getAdapterConfig());
      hasChanged = true;
    }

    return hasChanged;
  }

  private void parseAdapterConfigs(Object value) {
    if (value instanceof ConfigurationProperties properties) {
      for (var entry : properties.entrySet()) {
        if (entry.getValue() instanceof ConfigurationProperties adapterProperties) {
          this.adapterConfig.put(entry.getKey(), adapterProperties);
        }
      }
    } else if (value instanceof Map<?, ?> entries) {
      for (var entry : entries.entrySet()) {
        if (entry.getKey() instanceof String adapterName && entry.getValue() instanceof ConfigurationProperties adapterProperties) {
          this.adapterConfig.put(adapterName, adapterProperties);
        }
      }
    }
  }

  private List<MavlinkTwinConfigDTO> parseMavlinkConfigs(Object value) {
    List<MavlinkTwinConfigDTO> configs = new ArrayList<>();

    if (value instanceof ConfigurationProperties entry) {
      configs.add(parseMavlinkConfig(entry));
    } else if (value instanceof List<?> list) {
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

  private List<DroneInfoDTO> parseDroneInfos(Object properties) {
    List<DroneInfoDTO> droneInfos = new ArrayList<>();

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

  private DroneInfoDTO parseDroneInfo(ConfigurationProperties properties) {
    DroneInfoDTO droneInfo = new DroneInfoDTO();

    String uuidString = properties.getProperty("uuid", null);
    if (uuidString != null) {
      droneInfo.setUuid(UUID.fromString(uuidString));
    }
    droneInfo.setBatteryCapacityAh(properties.getDoubleProperty("batteryCapacityAh", droneInfo.getBatteryCapacityAh()));
    droneInfo.setBatteryCapacityHours(properties.getDoubleProperty("batteryCapacityHours", droneInfo.getBatteryCapacityHours()));
    droneInfo.setName(properties.getProperty("name", droneInfo.getName()));
    droneInfo.setModelName(properties.getProperty("modelName", droneInfo.getModelName()));
    droneInfo.setGeospatialArea(properties.getProperty("geospatialArea", droneInfo.getGeospatialArea()));
    droneInfo.setRangeMeters(readOptionalPositiveDouble(properties, "rangeMeters"));
    droneInfo.setSurveyRadiusMeters(readOptionalPositiveDouble(properties, "surveyRadiusMeters"));
    droneInfo.setMessageEncoding(parseMessageEncoding(properties.getProperty("messageEncoding", null), droneInfo.getMessageEncoding()));
    StopActionEnum legacyStopAction = parseTerminalAction(properties.getProperty("stopAction", null), droneInfo.getCancelAction());
    droneInfo.setCancelAction(parseTerminalAction(properties.getProperty("cancelAction", null), legacyStopAction));
    droneInfo.setMissionEndAction(parseTerminalAction(properties.getProperty("missionEndAction", null), droneInfo.getMissionEndAction()));
    droneInfo.setMissionTimeoutAction(parseTerminalAction(properties.getProperty("missionTimeoutAction", null), droneInfo.getMissionTimeoutAction()));
    if (properties.get("description") instanceof ConfigurationProperties descriptionProperties) {
      droneInfo.setDescription(descriptionProperties.getMap());
    } else if (properties.get("description") instanceof Map<?, ?> descriptionMap) {
      droneInfo.setDescription(toStringObjectMap(descriptionMap));
    }

    droneInfo.setCapabilities(parseTaskCapabilities(properties.get("capabilities")));
    return droneInfo;
  }

  private Double readOptionalPositiveDouble(ConfigurationProperties properties, String propertyName) {
    if (!properties.containsKey(propertyName)) {
      return null;
    }
    double value = properties.getDoubleProperty(propertyName, 0.0d);
    return Double.isFinite(value) && value > 0.0d ? value : null;
  }

  private MessageEncodingEnum parseMessageEncoding(String value, MessageEncodingEnum defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    return MessageEncodingEnum.valueOf(value.trim().toUpperCase(Locale.ROOT));
  }

  private StopActionEnum parseTerminalAction(String value, StopActionEnum defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    return StopActionEnum.valueOf(value.trim().toUpperCase(Locale.ROOT));
  }

  private List<ConfigurationProperties> toDroneInfoConfigurationProperties(List<DroneInfoDTO> droneInfos) {
    List<ConfigurationProperties> values = new ArrayList<>();

    for (DroneInfoDTO droneInfo : droneInfos) {
      ConfigurationProperties properties = new ConfigurationProperties();
      properties.put("name", droneInfo.getName());

      if (droneInfo.getUuid() != null) {
        properties.put("uuid", droneInfo.getUuid().toString());
      }

      if (droneInfo.getModelName() != null) {
        properties.put("modelName", droneInfo.getModelName());
      }

      if (droneInfo.getGeospatialArea() != null) {
        properties.put("geospatialArea", droneInfo.getGeospatialArea());
      }

      if (droneInfo.getRangeMeters() != null && droneInfo.getRangeMeters() > 0.0d) {
        properties.put("rangeMeters", droneInfo.getRangeMeters());
      }

      if (droneInfo.getSurveyRadiusMeters() != null && droneInfo.getSurveyRadiusMeters() > 0.0d) {
        properties.put("surveyRadiusMeters", droneInfo.getSurveyRadiusMeters());
      }

      if (droneInfo.getMessageEncoding() != null) {
        properties.put("messageEncoding", droneInfo.getMessageEncoding().name());
      }

      if (droneInfo.getCancelAction() != null) {
        properties.put("cancelAction", droneInfo.getCancelAction().name());
      }

      if (droneInfo.getMissionEndAction() != null) {
        properties.put("missionEndAction", droneInfo.getMissionEndAction().name());
      }

      if (droneInfo.getMissionTimeoutAction() != null) {
        properties.put("missionTimeoutAction", droneInfo.getMissionTimeoutAction().name());
      }

      properties.put("batteryCapacityAh", droneInfo.getBatteryCapacityAh());
      properties.put("batteryCapacityHours", droneInfo.getBatteryCapacityHours());

      if (droneInfo.getDescription() != null && !droneInfo.getDescription().isEmpty()) {
        properties.put("description", new ConfigurationProperties(new LinkedHashMap<>(droneInfo.getDescription())));
      }

      if (droneInfo.getCapabilities() != null) {
        properties.put("capabilities", toTaskCapabilitiesConfigurationProperties(droneInfo.getCapabilities()));
      }

      values.add(properties);
    }

    return values;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> toStringObjectMap(Map<?, ?> source) {
    Map<String, Object> target = new LinkedHashMap<>();

    for (Map.Entry<?, ?> entry : source.entrySet()) {
      if (entry.getKey() instanceof String key) {
        target.put(key, entry.getValue());
      }
    }

    return target;
  }

  private TaskCapabilities parseTaskCapabilities(Object value) {
    TaskCapabilities capabilities = new TaskCapabilities();

    if (value instanceof ConfigurationProperties configurationProperties) {
      capabilities.setTasks(parseTaskCapabilityList(configurationProperties.get("tasks")));

      if (capabilities.getTasks().isEmpty()) {
        capabilities.setTasks(parseTaskCapabilityList(configurationProperties.get("task_capabilities")));
      }

      capabilities.setTaskConditionsMode(parseTaskConditionMode(configurationProperties.getProperty("task_conditions_mode", null), capabilities.getTaskConditionsMode()));
      capabilities.setTaskConditionsTemplate(parseTaskTemplateMode(configurationProperties.getProperty("task_conditions_template", null), capabilities.getTaskConditionsTemplate()));
    } else if (value instanceof List<?> list) {
      capabilities.setTasks(parseTaskCapabilityList(list));
    }

    return capabilities;
  }

  private ConfigurationProperties toTaskCapabilitiesConfigurationProperties(TaskCapabilities capabilities) {
    ConfigurationProperties properties = new ConfigurationProperties();

    if (capabilities.getTasks() != null && !capabilities.getTasks().isEmpty()) {
      properties.put("tasks", toTaskCapabilityConfigurationProperties(capabilities.getTasks()));
    }

    if (capabilities.getTaskConditionsMode() != null) {
      properties.put("task_conditions_mode", capabilities.getTaskConditionsMode().name());
    }

    if (capabilities.getTaskConditionsTemplate() != null) {
      properties.put("task_conditions_template", capabilities.getTaskConditionsTemplate().name());
    }

    return properties;
  }

  private List<ConfigurationProperties> toTaskCapabilityConfigurationProperties(List<TaskCapability> taskCapabilities) {
    List<ConfigurationProperties> values = new ArrayList<>();

    for (TaskCapability taskCapability : taskCapabilities) {
      ConfigurationProperties properties = new ConfigurationProperties();

      if (taskCapability.getTaskType() != null) {
        properties.put("task_type", taskCapability.getTaskType().name());
      }

      if (taskCapability.getSpecialization() != null) {
        properties.put("task_specialization", taskCapability.getSpecialization().name());
      }

      if (taskCapability.getAuthorities() != null && taskCapability.getAuthorities().length > 0) {
        properties.put("authorities", toTaskAuthoritiesConfigurationProperties(taskCapability.getAuthorities()));
      }

      values.add(properties);
    }

    return values;
  }

  private List<ConfigurationProperties> toTaskAuthoritiesConfigurationProperties(Authorities[] authorities) {
    List<ConfigurationProperties> values = new ArrayList<>();

    for (Authorities authority : authorities) {
      if (authority != null && authority.getGuid() != null) {
        ConfigurationProperties properties = new ConfigurationProperties();
        properties.put("guid", authority.getGuid().toString());
        values.add(properties);
      }
    }

    return values;
  }

  private List<TaskCapability> parseTaskCapabilityList(Object value) {
    List<TaskCapability> taskCapabilities = new ArrayList<>();

    if (value instanceof ConfigurationProperties configurationProperties) {
      taskCapabilities.add(parseTaskCapability(configurationProperties));
    } else if (value instanceof List<?> list) {
      for (Object entry : list) {
        if (entry instanceof ConfigurationProperties configurationProperties) {
          taskCapabilities.add(parseTaskCapability(configurationProperties));
        }
      }
    }

    return taskCapabilities;
  }

  private TaskCapability parseTaskCapability(ConfigurationProperties properties) {
    TaskCapability taskCapability = new TaskCapability();

    taskCapability.setTaskType(parsePlanTaskType(properties.getProperty("task_type", null), taskCapability.getTaskType()));
    taskCapability.setSpecialization(parseTaskSpecialization(properties.getProperty("task_specialization", null), taskCapability.getSpecialization()));
    taskCapability.setAuthorities(parseTaskAuthorities(properties.get("authorities")));
    return taskCapability;
  }

  private Authorities[] parseTaskAuthorities(Object authorities) {
    if (authorities == null) {
      return new Authorities[0];
    }

    List<Authorities> authoritiesList = new ArrayList<>();
    if (authorities instanceof ConfigurationProperties configurationProperties) {
      parseAuthority(authoritiesList, configurationProperties);
    } else if (authorities instanceof List<?>) {
      for (Object entry : (List<?>) authorities) {
        if (entry instanceof ConfigurationProperties configurationProperties) {
          parseAuthority(authoritiesList, configurationProperties);
        }
      }
    }

    return authoritiesList.toArray(new Authorities[0]);
  }

  private void parseAuthority(List<Authorities> authoritiesList, ConfigurationProperties configurationProperties) {
    String guid = configurationProperties.getProperty("guid", null);
    if (guid != null) {
      authoritiesList.add(new Authorities(UUID.fromString(guid)));
    }
  }

  private PlanTaskType parsePlanTaskType(String value, PlanTaskType defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    return PlanTaskType.fromConfigurationValue(value);
  }

  private TaskSpecialization parseTaskSpecialization(String value, TaskSpecialization defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    String normalisedValue = removePrefix(value, "TaskSpecializationEnum_");
    return TaskSpecialization.valueOf(normalisedValue);
  }

  private TaskConditionMode parseTaskConditionMode(String value, TaskConditionMode defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    String normalisedValue = removePrefix(value, "TaskConditionModeEnum_");
    return TaskConditionMode.valueOf(normalisedValue);
  }

  private TaskTemplateMode parseTaskTemplateMode(String value, TaskTemplateMode defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    String normalisedValue = removePrefix(value, "TaskTemplateModeEnum_");
    return TaskTemplateMode.valueOf(normalisedValue);
  }

  private String removePrefix(String value, String prefix) {
    if (value.startsWith(prefix)) {
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

  private boolean shouldWriteN2kConfiguration() {
    if (this.n2KTwinConfig == null) {
      return false;
    }
    if (this.n2KTwinConfig.isEnable() || this.n2KTwinConfig.isPublishMavlinkDrones()) {
      return true;
    }
    if (this.n2KTwinConfig.getName() != null || this.n2KTwinConfig.getVehicleClass() != null) {
      return true;
    }
    return this.n2KTwinConfig.getTopic() != null && !"/canbus0/n2k/json/#".equals(this.n2KTwinConfig.getTopic());
  }
}

