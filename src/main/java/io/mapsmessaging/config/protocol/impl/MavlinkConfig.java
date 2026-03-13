/* * * Copyright [ 2020 - 2024 ] Matthew Buckton * Copyright [ 2024 - 2026 ] MapsMessaging B.V. * * Licensed under the Apache License, Version 2.0 with the Commons Clause * (the "License"); you may not use this file except in compliance with the License. * You may obtain a copy of the License at: * * http://www.apache.org/licenses/LICENSE-2.0 * https://commonsclause.com/ * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */ package io.mapsmessaging.config.protocol.impl; import io.mapsmessaging.config.Config; import io.mapsmessaging.configuration.ConfigurationProperties; import io.mapsmessaging.dto.rest.config.BaseConfigDTO; import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO; import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO; import java.util.ArrayList; import java.util.List; import java.util.Map; import java.util.Objects; public class MavlinkConfig extends MavlinkConfigDTO implements Config {
  public MavlinkConfig(ConfigurationProperties config) {
    setType("mavlink");
    ProtocolConfigFactory.unpack(config, this);
    this.fullyQualifiedPathToDialectXml = config.getProperty("fullyQualifiedPathToDialectXml", fullyQualifiedPathToDialectXml);
    this.idleSessionTimeout = config.getLongProperty("idleSessionTimeout", idleSessionTimeout);
    this.maximumSessionExpiry = config.getIntProperty("maximumSessionExpiry", maximumSessionExpiry);
    this.advertiseInterval = config.getIntProperty("advertiseInterval", advertiseInterval);
    this.maxInFlightEvents = config.getIntProperty("maxInFlightEvents", maxInFlightEvents);
    this.topicNameTemplate = config.getProperty("topicNameTemplate", topicNameTemplate);
    this.statusTopicNameTemplate = config.getProperty("statusTopicNameTemplate", statusTopicNameTemplate);
    this.parseToJson = config.getBooleanProperty("parseToJson", parseToJson);
    this.forwardUrls = config.getProperty("forwardUrls", forwardUrls);
    this.forwardRawFrames = config.getBooleanProperty("forwardRawFrames", forwardRawFrames);
    this.forwardRejectedRawFrames = config.getBooleanProperty("forwardRejectedRawFrames", forwardRejectedRawFrames);
    this.dropIfTargetEqualsSource = config.getBooleanProperty("dropIfTargetEqualsSource", dropIfTargetEqualsSource);
    this.dedupWindowMillis = config.getIntProperty("dedupWindowMillis", dedupWindowMillis);
    this.rejectUnknownSources = config.getBooleanProperty("rejectUnknownSources", rejectUnknownSources);
    this.rejectedFrameNamespace = config.getProperty("rejectedFrameNamespace", rejectedFrameNamespace);
    this.includeRejectedFrameMetadata = config.getBooleanProperty("includeRejectedFrameMetadata", includeRejectedFrameMetadata);
    this.acceptedMessageIds = readIntegerList(config.get("acceptedMessageIds"));
    this.rejectedMessageIds = readIntegerList(config.get("rejectedMessageIds"));
    this.knownSources = readKnownSources(config.get("knownSources"));
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof MavlinkConfigDTO newConfig) {
      if (!Objects.equals(fullyQualifiedPathToDialectXml, newConfig.getFullyQualifiedPathToDialectXml())) {
        fullyQualifiedPathToDialectXml = newConfig.getFullyQualifiedPathToDialectXml();
        hasChanged = true;
      }
      if (idleSessionTimeout != newConfig.getIdleSessionTimeout()) {
        idleSessionTimeout = newConfig.getIdleSessionTimeout();
        hasChanged = true;
      }
      if (maximumSessionExpiry != newConfig.getMaximumSessionExpiry()) {
        maximumSessionExpiry = newConfig.getMaximumSessionExpiry();
        hasChanged = true;
      }
      if (advertiseInterval != newConfig.getAdvertiseInterval()) {
        advertiseInterval = newConfig.getAdvertiseInterval();
        hasChanged = true;
      }
      if (maxInFlightEvents != newConfig.getMaxInFlightEvents()) {
        maxInFlightEvents = newConfig.getMaxInFlightEvents();
        hasChanged = true;
      }
      if (!Objects.equals(topicNameTemplate, newConfig.getTopicNameTemplate())) {
        topicNameTemplate = newConfig.getTopicNameTemplate();
        hasChanged = true;
      }
      if (parseToJson != newConfig.isParseToJson()) {
        parseToJson = newConfig.isParseToJson();
        hasChanged = true;
      }
      if (!Objects.equals(forwardUrls, newConfig.getForwardUrls())) {
        forwardUrls = newConfig.getForwardUrls();
        hasChanged = true;
      }
      if (forwardRawFrames != newConfig.isForwardRawFrames()) {
        forwardRawFrames = newConfig.isForwardRawFrames();
        hasChanged = true;
      }
      if (forwardRejectedRawFrames != newConfig.isForwardRejectedRawFrames()) {
        forwardRejectedRawFrames = newConfig.isForwardRejectedRawFrames();
        hasChanged = true;
      }
      if (dropIfTargetEqualsSource != newConfig.isDropIfTargetEqualsSource()) {
        dropIfTargetEqualsSource = newConfig.isDropIfTargetEqualsSource();
        hasChanged = true;
      }
      if (dedupWindowMillis != newConfig.getDedupWindowMillis()) {
        dedupWindowMillis = newConfig.getDedupWindowMillis();
        hasChanged = true;
      }
      if (!Objects.equals(acceptedMessageIds, newConfig.getAcceptedMessageIds())) {
        acceptedMessageIds = copyIntegerList(newConfig.getAcceptedMessageIds());
        hasChanged = true;
      }
      if (!Objects.equals(rejectedMessageIds, newConfig.getRejectedMessageIds())) {
        rejectedMessageIds = copyIntegerList(newConfig.getRejectedMessageIds());
        hasChanged = true;
      }
      if (!Objects.equals(knownSources, newConfig.getKnownSources())) {
        knownSources = copyKnownSources(newConfig.getKnownSources());
        hasChanged = true;
      }
      if (rejectUnknownSources != newConfig.isRejectUnknownSources()) {
        rejectUnknownSources = newConfig.isRejectUnknownSources();
        hasChanged = true;
      }
      if (!Objects.equals(rejectedFrameNamespace, newConfig.getRejectedFrameNamespace())) {
        rejectedFrameNamespace = newConfig.getRejectedFrameNamespace();
        hasChanged = true;
      }
      if (includeRejectedFrameMetadata != newConfig.isIncludeRejectedFrameMetadata()) {
        includeRejectedFrameMetadata = newConfig.isIncludeRejectedFrameMetadata();
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
    properties.put("fullyQualifiedPathToDialectXml", fullyQualifiedPathToDialectXml);
    properties.put("idleSessionTimeout", idleSessionTimeout);
    properties.put("maximumSessionExpiry", maximumSessionExpiry);
    properties.put("advertiseInterval", advertiseInterval);
    properties.put("maxInFlightEvents", maxInFlightEvents);
    properties.put("topicNameTemplate", topicNameTemplate);
    properties.put("parseToJson", parseToJson);
    properties.put("forwardUrls", forwardUrls);
    properties.put("forwardRawFrames", forwardRawFrames);
    properties.put("forwardRejectedRawFrames", forwardRejectedRawFrames);
    properties.put("dropIfTargetEqualsSource", dropIfTargetEqualsSource);
    properties.put("dedupWindowMillis", dedupWindowMillis);
    properties.put("acceptedMessageIds", new ArrayList<>(acceptedMessageIds));
    properties.put("rejectedMessageIds", new ArrayList<>(rejectedMessageIds));
    properties.put("knownSources", writeKnownSources(knownSources));
    properties.put("rejectUnknownSources", rejectUnknownSources);
    properties.put("rejectedFrameNamespace", rejectedFrameNamespace);
    properties.put("includeRejectedFrameMetadata", includeRejectedFrameMetadata);
    return properties;
  }

  private List<Integer> readIntegerList(Object raw) {
    List<Integer> result = new ArrayList<>();
    if (raw == null) {
      return result;
    }
    if (raw instanceof List<?> list) {
      for (Object entry : list) {
        Integer value = toInteger(entry);
        if (value != null) {
          result.add(value);
        }
      }
    }
    if (raw instanceof String value && !value.isBlank()) {
      String[] split = value.split(",");
      for (String token : split) {
        Integer integerValue = toInteger(token.trim());
        if (integerValue != null) {
          result.add(integerValue);
        }
      }
    }
    return result;
  }

  private Integer toInteger(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Integer integerValue) {
      return integerValue;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String stringValue && !stringValue.isBlank()) {
      return Integer.parseInt(stringValue.trim());
    }
    return null;
  }

  private List<MavlinkKnownSourceDTO> readKnownSources(Object raw) {
    List<MavlinkKnownSourceDTO> result = new ArrayList<>();
    if (!(raw instanceof List<?> list)) {
      return result;
    }
    for (Object entry : list) {
      MavlinkKnownSourceDTO source = toKnownSource(entry);
      if (source != null) {
        result.add(source);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private MavlinkKnownSourceDTO toKnownSource(Object raw) {
    if (raw instanceof MavlinkKnownSourceDTO source) {
      return copyKnownSource(source);
    }
    if (raw instanceof ConfigurationProperties properties) {
      MavlinkKnownSourceDTO source = new MavlinkKnownSourceDTO();
      source.setName(properties.getProperty("name", ""));
      source.setDescription(properties.getProperty("description", ""));
      source.setSystemId(properties.getIntProperty("systemId", 0));
      source.setComponentId(properties.getIntProperty("componentId", 0));
      source.setAcceptedMessageIds(readIntegerList(properties.get("acceptedMessageIds")));
      source.setRejectedMessageIds(readIntegerList(properties.get("rejectedMessageIds")));
      return source;
    }
    if (raw instanceof Map<?, ?> map) {
      MavlinkKnownSourceDTO source = new MavlinkKnownSourceDTO();
      source.setName(readString(map.get("name")));
      source.setDescription(readString(map.get("description")));
      source.setSystemId(defaultInteger(map.get("systemId")));
      source.setComponentId(defaultInteger(map.get("componentId")));
      source.setAcceptedMessageIds(readIntegerList(map.get("acceptedMessageIds")));
      source.setRejectedMessageIds(readIntegerList(map.get("rejectedMessageIds")));
      return source;
    }
    return null;
  }

  private String readString(Object value) {
    return value == null ? "" : value.toString();
  }

  private int defaultInteger(Object value) {
    Integer integerValue = toInteger(value);
    return integerValue == null ? 0 : integerValue;
  }

  private List<Integer> copyIntegerList(List<Integer> source) {
    if (source == null) {
      return new ArrayList<>();
    }
    return new ArrayList<>(source);
  }

  private List<MavlinkKnownSourceDTO> copyKnownSources(List<MavlinkKnownSourceDTO> source) {
    List<MavlinkKnownSourceDTO> result = new ArrayList<>();
    if (source == null) {
      return result;
    }
    for (MavlinkKnownSourceDTO entry : source) {
      result.add(copyKnownSource(entry));
    }
    return result;
  }

  private MavlinkKnownSourceDTO copyKnownSource(MavlinkKnownSourceDTO source) {
    MavlinkKnownSourceDTO copy = new MavlinkKnownSourceDTO();
    copy.setName(source.getName());
    copy.setDescription(source.getDescription());
    copy.setSystemId(source.getSystemId());
    copy.setComponentId(source.getComponentId());
    copy.setAcceptedMessageIds(copyIntegerList(source.getAcceptedMessageIds()));
    copy.setRejectedMessageIds(copyIntegerList(source.getRejectedMessageIds()));
    return copy;
  }

  private List<ConfigurationProperties> writeKnownSources(List<MavlinkKnownSourceDTO> sources) {
    List<ConfigurationProperties> result = new ArrayList<>();
    if (sources == null) {
      return result;
    }
    for (MavlinkKnownSourceDTO source : sources) {
      ConfigurationProperties properties = new ConfigurationProperties();
      properties.put("name", source.getName());
      properties.put("description", source.getDescription());
      properties.put("systemId", source.getSystemId());
      properties.put("componentId", source.getComponentId());
      properties.put("acceptedMessageIds", new ArrayList<>(source.getAcceptedMessageIds()));
      properties.put("rejectedMessageIds", new ArrayList<>(source.getRejectedMessageIds()));
      result.add(properties);
    }
    return result;
  }
}