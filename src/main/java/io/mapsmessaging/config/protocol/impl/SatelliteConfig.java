/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;

import java.util.Objects;

public class SatelliteConfig extends SatelliteConfigDTO implements Config {

  public SatelliteConfig(ConfigurationProperties config) {
    setType("satellite");
    ProtocolConfigFactory.unpack(config, this);

    baseUrl = config.getProperty("baseUrl", "");
    incomingMessagePollInterval = config.getIntProperty("incomingMessagePollInterval", 15);
    outgoingMessagePollInterval = config.getIntProperty("outgoingMessagePollInterval", 60);
    httpRequestTimeout = config.getIntProperty("httpRequestTimeoutSec", 30);
    maxInflightEventsPerDevice = config.getIntProperty("maxInflightEventsPerDevice", 2);

    outboundBroadcast = config.getProperty("outboundBroadcast", "");
    commonInboundPublishRoot = config.getProperty("commonInboundPublishRoot", "/{deviceId}/common/in/{sin}/{min}");
    commonOutboundPublishRoot = config.getProperty("commonOutboundPublishRoot", "/{deviceId}/common/out/#");
    mapsInboundPublishRoot = config.getProperty("mapsInboundPublishRoot", "/{deviceId}/maps/in/{namespace}/#");
    mapsOutboundPublishRoot = config.getProperty("mapsOutboundPublishRoot", "/{deviceId}/maps/out/{namespace}/#");

    deviceInfoUpdateMinutes = config.getIntProperty("deviceInfoUpdateMinutes", 15);
    maxBufferSize = config.getIntProperty("maxBufferSize", 4000);
    compressionCutoffSize = config.getIntProperty("compressionCutoffSize", 128);
    messageLifeTimeInMinutes = config.getIntProperty("messageLifeTimeInMinutes", 10);
    sharedSecret = config.getProperty("sharedSecret", "");
    sendHighPriorityMessages = config.getBooleanProperty("sendHighPriorityMessages", false);
    sinNumber = config.getIntProperty("sinNumber", 147);

    if(incomingMessagePollInterval <10){
      incomingMessagePollInterval = 10;
    }
    if(outgoingMessagePollInterval <15){
      outgoingMessagePollInterval = 15;
    }

    if(deviceInfoUpdateMinutes < 10){
      deviceInfoUpdateMinutes = 15;
    }
    // This is optional and used for ViaSat
    mailboxId = config.getProperty("mailboxId", "");
    mailboxPassword = config.getProperty("mailboxPassword", "");
  }


  @Override
  public boolean update(BaseConfigDTO cfg) {
    boolean changed = false;
    if (cfg instanceof SatelliteConfigDTO dto) {
      changed = ProtocolConfigFactory.update(this, dto);

      if (!Objects.equals(baseUrl, dto.getBaseUrl())) {
        baseUrl = dto.getBaseUrl();
        changed = true;
      }
      if(sendHighPriorityMessages != dto.isSendHighPriorityMessages()) {
        sendHighPriorityMessages = dto.isSendHighPriorityMessages();
        changed = true;
      }
      if (incomingMessagePollInterval != dto.getIncomingMessagePollInterval()) {
        incomingMessagePollInterval = dto.getIncomingMessagePollInterval();
        changed = true;
      }
      if (outgoingMessagePollInterval != dto.getOutgoingMessagePollInterval()) {
        outgoingMessagePollInterval = dto.getOutgoingMessagePollInterval();
        changed = true;
      }
      if (httpRequestTimeout != dto.getHttpRequestTimeout()) {
        httpRequestTimeout = dto.getHttpRequestTimeout();
        changed = true;
      }
      if(maxInflightEventsPerDevice != dto.getMaxInflightEventsPerDevice()){
        maxInflightEventsPerDevice = dto.getMaxInflightEventsPerDevice();
        changed = true;
      }
      if (!outboundBroadcast.equals(dto.getOutboundBroadcast())) {
        outboundBroadcast = dto.getOutboundBroadcast();
        changed = true;
      }
      if(!mailboxId.equals(dto.getMailboxId())) {
        mailboxId = dto.getMailboxId();
        changed = true;
      }
      if(!mailboxPassword.equals(dto.getMailboxPassword())) {
        mailboxPassword = dto.getMailboxPassword();
        changed = true;
      }
      if (!commonInboundPublishRoot.equals(dto.getCommonInboundPublishRoot())) {
        commonInboundPublishRoot = dto.getCommonInboundPublishRoot();
        changed = true;
      }

      if (!commonOutboundPublishRoot.equals(dto.getCommonOutboundPublishRoot())) {
        commonOutboundPublishRoot = dto.getCommonOutboundPublishRoot();
        changed = true;
      }

      if (!mapsInboundPublishRoot.equals(dto.getMapsInboundPublishRoot())) {
        mapsInboundPublishRoot = dto.getMapsInboundPublishRoot();
        changed = true;
      }

      if (!mapsOutboundPublishRoot.equals(dto.getMapsOutboundPublishRoot())) {
        mapsOutboundPublishRoot = dto.getMapsOutboundPublishRoot();
        changed = true;
      }
      if(deviceInfoUpdateMinutes != dto.getDeviceInfoUpdateMinutes()) {
        deviceInfoUpdateMinutes = dto.getDeviceInfoUpdateMinutes();
      }
      if(maxBufferSize != dto.getMaxBufferSize()) {
        maxBufferSize = dto.getMaxBufferSize();
        changed = true;
      }
      if(compressionCutoffSize != dto.getCompressionCutoffSize()) {
        compressionCutoffSize = dto.getCompressionCutoffSize();
        changed = true;
      }
      if(messageLifeTimeInMinutes != dto.getMessageLifeTimeInMinutes()) {
        messageLifeTimeInMinutes = dto.getMessageLifeTimeInMinutes();
        changed = true;
      }
      if(!sharedSecret.equals(dto.getSharedSecret())) {
        sharedSecret = dto.getSharedSecret();
        changed = true;
      }
      if(sinNumber != dto.getSinNumber()){
        sinNumber = dto.getSinNumber();
        changed = true;
      }
    }
    return changed;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("baseUrl", baseUrl);
    properties.put("incomingMessagePollInterval", incomingMessagePollInterval);
    properties.put("outgoingMessagePollInterval", outgoingMessagePollInterval);
    properties.put("httpRequestTimeoutSec", httpRequestTimeout);
    properties.put("maxInflightEventsPerDevice", maxInflightEventsPerDevice);
    properties.put("mailboxId", mailboxId);
    properties.put("mailboxPassword", mailboxPassword);
    properties.put("deviceInfoUpdateMinutes", deviceInfoUpdateMinutes);
    properties.put("maxBufferSize", maxBufferSize);
    properties.put("compressionCutoffSize", compressionCutoffSize);
    properties.put("messageLifeTimeInMinutes", messageLifeTimeInMinutes);
    properties.put("sharedSecret", sharedSecret);
    properties.put("sendHighPriorityMessages", sendHighPriorityMessages);
    properties.put("sinNumber", sinNumber);
    properties.put("outboundBroadcast", outboundBroadcast);
    properties.put("commonInboundPublishRoot", commonInboundPublishRoot);
    properties.put("commonOutboundPublishRoot", commonOutboundPublishRoot);
    properties.put("mapsInboundPublishRoot", mapsInboundPublishRoot);
    properties.put("mapsOutboundPublishRoot", mapsOutboundPublishRoot);
    return properties;
  }
}
