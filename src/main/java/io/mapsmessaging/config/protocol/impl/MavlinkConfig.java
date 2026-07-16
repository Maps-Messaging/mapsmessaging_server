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
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkAcceptedSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MavlinkConfig extends MavlinkConfigDTO implements Config {

  public MavlinkConfig(ConfigurationProperties config) {
    setType("mavlink");
    ProtocolConfigFactory.unpack(config, this);
    this.dialectName = config.getProperty("dialectName", dialectName);
    this.idleSessionTimeout = config.getLongProperty("idleSessionTimeout", idleSessionTimeout);
    this.maximumSessionExpiry = config.getIntProperty("maximumSessionExpiry", maximumSessionExpiry);
    this.advertiseInterval = config.getIntProperty("advertiseInterval", advertiseInterval);
    this.systemId = readOptionalInteger(config.get("systemId"));
    this.componentId = readOptionalInteger(config.get("componentId"));
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
    this.outboundTopicName = config.getProperty("outboundTopicName", outboundTopicName);
    this.qualityOfService = config.getIntProperty("qualityOfService", qualityOfService);
    this.storeOffline = config.getBooleanProperty("storeOffline", storeOffline);
    this.tlogDirectory = config.getProperty("tlogDirectory", tlogDirectory);
    this.acceptedMessageIds = readIntegerList(config.get("acceptedMessageIds"));
    this.rejectedMessageIds = readIntegerList(config.get("rejectedMessageIds"));
    this.acceptedSources = readKnownSources(config.get("acceptedSources"));
    this.heartbeatIntervalSeconds = config.getIntProperty("heartbeatIntervalSeconds", heartbeatIntervalSeconds);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof MavlinkConfigDTO newConfig) {
      if (!Objects.equals(dialectName, newConfig.getDialectName())) {
        dialectName = newConfig.getDialectName();
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
      if (!Objects.equals(systemId, newConfig.getSystemId())) {
        systemId = newConfig.getSystemId();
        hasChanged = true;
      }
      if (!Objects.equals(componentId, newConfig.getComponentId())) {
        componentId = newConfig.getComponentId();
        hasChanged = true;
      }
      if (heartbeatIntervalSeconds != newConfig.getHeartbeatIntervalSeconds()) {
        heartbeatIntervalSeconds = newConfig.getHeartbeatIntervalSeconds();
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
      if (!Objects.equals(statusTopicNameTemplate, newConfig.getStatusTopicNameTemplate())) {
        statusTopicNameTemplate = newConfig.getStatusTopicNameTemplate();
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
      if (!Objects.equals(acceptedSources, newConfig.getAcceptedSources())) {
        acceptedSources = copyKnownSources(newConfig.getAcceptedSources());
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
      if (!Objects.equals(outboundTopicName, newConfig.getOutboundTopicName())) {
        outboundTopicName = newConfig.getOutboundTopicName();
        hasChanged = true;
      }
      if (qualityOfService != newConfig.getQualityOfService()) {
        qualityOfService = newConfig.getQualityOfService();
        hasChanged = true;
      }
      if (storeOffline != newConfig.isStoreOffline()) {
        storeOffline = newConfig.isStoreOffline();
        hasChanged = true;
      }
      if (!Objects.equals(tlogDirectory, newConfig.getTlogDirectory())) {
        tlogDirectory = newConfig.getTlogDirectory();
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
    properties.put("dialectName", dialectName);
    properties.put("idleSessionTimeout", idleSessionTimeout);
    properties.put("maximumSessionExpiry", maximumSessionExpiry);
    properties.put("advertiseInterval", advertiseInterval);
    putOptional(properties, "systemId", systemId);
    putOptional(properties, "componentId", componentId);
    properties.put("maxInFlightEvents", maxInFlightEvents);
    properties.put("topicNameTemplate", topicNameTemplate);
    properties.put("statusTopicNameTemplate", statusTopicNameTemplate);
    properties.put("parseToJson", parseToJson);
    properties.put("forwardUrls", forwardUrls);
    properties.put("forwardRawFrames", forwardRawFrames);
    properties.put("forwardRejectedRawFrames", forwardRejectedRawFrames);
    properties.put("dropIfTargetEqualsSource", dropIfTargetEqualsSource);
    properties.put("dedupWindowMillis", dedupWindowMillis);
    properties.put("acceptedMessageIds", new ArrayList<>(acceptedMessageIds));
    properties.put("rejectedMessageIds", new ArrayList<>(rejectedMessageIds));
    properties.put("acceptedSources", writeKnownSources(acceptedSources));
    properties.put("rejectUnknownSources", rejectUnknownSources);
    properties.put("rejectedFrameNamespace", rejectedFrameNamespace);
    properties.put("includeRejectedFrameMetadata", includeRejectedFrameMetadata);
    properties.put("outboundTopicName", outboundTopicName);
    properties.put("qualityOfService", qualityOfService);
    properties.put("storeOffline", storeOffline);
    putOptional(properties, "tlogDirectory", tlogDirectory);
    return properties;
  }

  private Integer readOptionalInteger(Object raw) {
    if (raw == null) {
      return null;
    }
    return toInteger(raw);
  }

  private void putOptional(ConfigurationProperties properties, String key, Object value) {
    if (value != null) {
      properties.put(key, value);
    }
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

  private List<MavlinkAcceptedSourceDTO> readKnownSources(Object raw) {
    List<MavlinkAcceptedSourceDTO> result = new ArrayList<>();
    if (!(raw instanceof List<?> list)) {
      return result;
    }
    for (Object entry : list) {
      MavlinkAcceptedSourceDTO source = toKnownSource(entry);
      if (source != null) {
        result.add(source);
      }
    }
    return result;
  }

  private MavlinkAcceptedSourceDTO toKnownSource(Object raw) {
    if (raw instanceof MavlinkAcceptedSourceDTO source) {
      return copyKnownSource(source);
    }
    if (raw instanceof ConfigurationProperties properties) {
      MavlinkAcceptedSourceDTO source = new MavlinkAcceptedSourceDTO();
      source.setSystemId(properties.getIntProperty("systemId", 0));
      source.setComponentId(properties.getIntProperty("componentId", 0));
      source.setAcceptedMessageIds(readIntegerList(properties.get("acceptedMessageIds")));
      source.setRejectedMessageIds(readIntegerList(properties.get("rejectedMessageIds")));
      return source;
    }
    if (raw instanceof Map<?, ?> map) {
      MavlinkAcceptedSourceDTO source = new MavlinkAcceptedSourceDTO();
      source.setSystemId(defaultInteger(map.get("systemId")));
      source.setComponentId(defaultInteger(map.get("componentId")));
      source.setAcceptedMessageIds(readIntegerList(map.get("acceptedMessageIds")));
      source.setRejectedMessageIds(readIntegerList(map.get("rejectedMessageIds")));
      return source;
    }
    return null;
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

  private List<MavlinkAcceptedSourceDTO> copyKnownSources(List<MavlinkAcceptedSourceDTO> source) {
    List<MavlinkAcceptedSourceDTO> result = new ArrayList<>();
    if (source == null) {
      return result;
    }
    for (MavlinkAcceptedSourceDTO entry : source) {
      result.add(copyKnownSource(entry));
    }
    return result;
  }

  private MavlinkAcceptedSourceDTO copyKnownSource(MavlinkAcceptedSourceDTO source) {
    MavlinkAcceptedSourceDTO copy = new MavlinkAcceptedSourceDTO();
    copy.setSystemId(source.getSystemId());
    copy.setComponentId(source.getComponentId());
    copy.setAcceptedMessageIds(copyIntegerList(source.getAcceptedMessageIds()));
    copy.setRejectedMessageIds(copyIntegerList(source.getRejectedMessageIds()));
    return copy;
  }

  private List<ConfigurationProperties> writeKnownSources(List<MavlinkAcceptedSourceDTO> sources) {
    List<ConfigurationProperties> result = new ArrayList<>();
    if (sources == null) {
      return result;
    }
    for (MavlinkAcceptedSourceDTO source : sources) {
      ConfigurationProperties properties = new ConfigurationProperties();
      properties.put("systemId", source.getSystemId());
      properties.put("componentId", source.getComponentId());
      properties.put("acceptedMessageIds", new ArrayList<>(source.getAcceptedMessageIds()));
      properties.put("rejectedMessageIds", new ArrayList<>(source.getRejectedMessageIds()));
      result.add(properties);
    }
    return result;
  }
}