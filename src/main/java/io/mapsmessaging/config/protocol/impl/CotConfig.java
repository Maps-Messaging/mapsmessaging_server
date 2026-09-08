/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotConfigDTO;
import java.util.Objects;

public class CotConfig extends CotConfigDTO implements Config {

  public CotConfig(ConfigurationProperties config) {
    ProtocolConfigFactory.unpack(config, this);
    inboundTopicName = config.getProperty("inboundTopicName", inboundTopicName);
    outboundTopicName = config.getProperty("outboundTopicName", outboundTopicName);
    maximumEventSize = config.getIntProperty("maximumEventSize", maximumEventSize);
    maximumSessionExpiry = config.getIntProperty("maximumSessionExpiry", maximumSessionExpiry);
    qualityOfService = config.getIntProperty("qualityOfService", qualityOfService);
    storeOffline = config.getBooleanProperty("storeOffline", storeOffline);
    appendNewLine = config.getBooleanProperty("appendNewLine", appendNewLine);
    suppressEchoes = config.getBooleanProperty("suppressEchoes", suppressEchoes);
    echoOrigin = config.getProperty("echoOrigin", echoOrigin);
    maximumHopCount = config.getIntProperty("maximumHopCount", maximumHopCount);
    fingerprintCacheSize = config.getIntProperty("fingerprintCacheSize", fingerprintCacheSize);
    fingerprintCacheTtlSeconds = config.getIntProperty("fingerprintCacheTtlSeconds", fingerprintCacheTtlSeconds);
    clockSkewSeconds = config.getIntProperty("clockSkewSeconds", clockSkewSeconds);
    maximumTrackedUids = config.getIntProperty("maximumTrackedUids", maximumTrackedUids);
    inboundQueueDepth = config.getIntProperty("inboundQueueDepth", inboundQueueDepth);
    outboundQueueDepth = config.getIntProperty("outboundQueueDepth", outboundQueueDepth);
    maximumXmlDepth = config.getIntProperty("maximumXmlDepth", maximumXmlDepth);
    writeTimeoutSeconds = config.getIntProperty("writeTimeoutSeconds", writeTimeoutSeconds);
    if (config.get("presence") instanceof ConfigurationProperties presenceProperties) {
      presence = new CotPresenceConfig(presenceProperties);
    } else {
      presence = new CotPresenceConfig(new ConfigurationProperties());
    }
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof CotConfigDTO updated)) {
      return false;
    }
    boolean changed = ProtocolConfigFactory.update(this, updated);
    if (!Objects.equals(inboundTopicName, updated.getInboundTopicName())) {
      inboundTopicName = updated.getInboundTopicName();
      changed = true;
    }
    if (!Objects.equals(outboundTopicName, updated.getOutboundTopicName())) {
      outboundTopicName = updated.getOutboundTopicName();
      changed = true;
    }
    if (maximumEventSize != updated.getMaximumEventSize()) {
      maximumEventSize = updated.getMaximumEventSize();
      changed = true;
    }
    if (maximumSessionExpiry != updated.getMaximumSessionExpiry()) {
      maximumSessionExpiry = updated.getMaximumSessionExpiry();
      changed = true;
    }
    if (qualityOfService != updated.getQualityOfService()) {
      qualityOfService = updated.getQualityOfService();
      changed = true;
    }
    if (storeOffline != updated.isStoreOffline()) {
      storeOffline = updated.isStoreOffline();
      changed = true;
    }
    if (appendNewLine != updated.isAppendNewLine()) {
      appendNewLine = updated.isAppendNewLine();
      changed = true;
    }
    if (suppressEchoes != updated.isSuppressEchoes()) {
      suppressEchoes = updated.isSuppressEchoes();
      changed = true;
    }
    if (!Objects.equals(echoOrigin, updated.getEchoOrigin())) {
      echoOrigin = updated.getEchoOrigin();
      changed = true;
    }
    if (maximumHopCount != updated.getMaximumHopCount()) {
      maximumHopCount = updated.getMaximumHopCount();
      changed = true;
    }
    if (fingerprintCacheSize != updated.getFingerprintCacheSize()) {
      fingerprintCacheSize = updated.getFingerprintCacheSize();
      changed = true;
    }
    if (fingerprintCacheTtlSeconds != updated.getFingerprintCacheTtlSeconds()) {
      fingerprintCacheTtlSeconds = updated.getFingerprintCacheTtlSeconds();
      changed = true;
    }
    if (clockSkewSeconds != updated.getClockSkewSeconds()) {
      clockSkewSeconds = updated.getClockSkewSeconds();
      changed = true;
    }
    if (maximumTrackedUids != updated.getMaximumTrackedUids()) {
      maximumTrackedUids = updated.getMaximumTrackedUids();
      changed = true;
    }
    if (inboundQueueDepth != updated.getInboundQueueDepth()) {
      inboundQueueDepth = updated.getInboundQueueDepth();
      changed = true;
    }
    if (outboundQueueDepth != updated.getOutboundQueueDepth()) {
      outboundQueueDepth = updated.getOutboundQueueDepth();
      changed = true;
    }
    if (maximumXmlDepth != updated.getMaximumXmlDepth()) {
      maximumXmlDepth = updated.getMaximumXmlDepth();
      changed = true;
    }
    if (writeTimeoutSeconds != updated.getWriteTimeoutSeconds()) {
      writeTimeoutSeconds = updated.getWriteTimeoutSeconds();
      changed = true;
    }
    if (presence instanceof CotPresenceConfig presenceConfig) {
      changed |= presenceConfig.update(updated.getPresence());
    } else if (!Objects.equals(presence, updated.getPresence())) {
      presence = updated.getPresence();
      changed = true;
    }
    return changed;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("inboundTopicName", inboundTopicName);
    properties.put("outboundTopicName", outboundTopicName);
    properties.put("maximumEventSize", maximumEventSize);
    properties.put("maximumSessionExpiry", maximumSessionExpiry);
    properties.put("qualityOfService", qualityOfService);
    properties.put("storeOffline", storeOffline);
    properties.put("appendNewLine", appendNewLine);
    properties.put("suppressEchoes", suppressEchoes);
    properties.put("echoOrigin", echoOrigin);
    properties.put("maximumHopCount", maximumHopCount);
    properties.put("fingerprintCacheSize", fingerprintCacheSize);
    properties.put("fingerprintCacheTtlSeconds", fingerprintCacheTtlSeconds);
    properties.put("clockSkewSeconds", clockSkewSeconds);
    properties.put("maximumTrackedUids", maximumTrackedUids);
    properties.put("inboundQueueDepth", inboundQueueDepth);
    properties.put("outboundQueueDepth", outboundQueueDepth);
    properties.put("maximumXmlDepth", maximumXmlDepth);
    properties.put("writeTimeoutSeconds", writeTimeoutSeconds);
    if (presence instanceof CotPresenceConfig presenceConfig) {
      properties.put("presence", presenceConfig.toConfigurationProperties());
    }
    return properties;
  }
}
