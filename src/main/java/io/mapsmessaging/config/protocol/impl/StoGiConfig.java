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
import io.mapsmessaging.config.network.impl.SerialConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.StoGiConfigDTO;

public class StoGiConfig extends StoGiConfigDTO implements Config {

  public StoGiConfig(ConfigurationProperties config) {
    setType("stogi");
    ProtocolConfigFactory.unpack(config, this);
    serial = new SerialConfig(config);
    initialSetup = config.getProperty("initialSetup", "");
    incomingMessagePollInterval = config.getLongProperty("incomingMessagePollInterval", 10);
    outgoingMessagePollInterval = config.getLongProperty("outgoingMessagePollInterval", 60);
    modemResponseTimeout = config.getLongProperty("modemResponseTimeout", 5000);
    locationPollInterval = config.getLongProperty("locationPollInterval", 0);
    modemStatsTopic = config.getProperty("modemStatsTopic", "");
    maxBufferSize = config.getIntProperty("maxBufferSize", 4000);
    compressionCutoffSize = config.getIntProperty("compressionCutoffSize", 128);
    messageLifeTimeInMinutes = config.getIntProperty("messageLifeTimeInMinutes", 10);
    sharedSecret = config.getProperty("sharedSecret", "");
    sendHighPriorityMessages = config.getBooleanProperty("sendHighPriorityMessages", false);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean result = false;
    if (config instanceof StoGiConfigDTO orbCommConfig) {
      result = ProtocolConfigFactory.update(this, orbCommConfig);
      result = ((SerialConfig)serial).update(config) || result;
      if(!modemStatsTopic.equals(orbCommConfig.getModemStatsTopic())){
        modemStatsTopic = orbCommConfig.getModemStatsTopic();
        result = true;
      }
      if(!initialSetup.equalsIgnoreCase(orbCommConfig.getInitialSetup())){
        initialSetup = orbCommConfig.getInitialSetup();
        result = true;
      }
      if(incomingMessagePollInterval != orbCommConfig.getIncomingMessagePollInterval()){
        incomingMessagePollInterval = orbCommConfig.getIncomingMessagePollInterval();
        result = true;
      }
      if(outgoingMessagePollInterval != orbCommConfig.getOutgoingMessagePollInterval()){
        outgoingMessagePollInterval = orbCommConfig.getOutgoingMessagePollInterval();
        result = true;
      }
      if (modemResponseTimeout != orbCommConfig.getModemResponseTimeout()) {
        modemResponseTimeout = orbCommConfig.getModemResponseTimeout();
        result = true;
      }
      if(locationPollInterval != orbCommConfig.getLocationPollInterval()){
        locationPollInterval = orbCommConfig.getLocationPollInterval();
        result = true;
      }
      if(maxBufferSize != orbCommConfig.getMaxBufferSize()) {
        maxBufferSize = orbCommConfig.getMaxBufferSize();
        result = true;
      }
      if(compressionCutoffSize != orbCommConfig.getCompressionCutoffSize()) {
        compressionCutoffSize = orbCommConfig.getCompressionCutoffSize();
        result = true;
      }
      if(sendHighPriorityMessages != orbCommConfig.isSendHighPriorityMessages()) {
        sendHighPriorityMessages = orbCommConfig.isSendHighPriorityMessages();
        result = true;
      }
      if(messageLifeTimeInMinutes != orbCommConfig.getMessageLifeTimeInMinutes()) {
        messageLifeTimeInMinutes = orbCommConfig.getMessageLifeTimeInMinutes();
        result = true;
      }
      if(!sharedSecret.equals(orbCommConfig.getSharedSecret())) {
        sharedSecret = orbCommConfig.getSharedSecret();
        result = true;
      }
    }
    return result;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("initialSetup", initialSetup);
    if(serial instanceof SerialConfig serialConfig){
      properties.put("serial", serialConfig.toConfigurationProperties());
    }
    properties.put("incomingMessagePollInterval", incomingMessagePollInterval);
    properties.put("outgoingMessagePollInterval", outgoingMessagePollInterval);
    properties.put("modemResponseTimeout", modemResponseTimeout);
    properties.put("locationPollInterval", getLocationPollInterval());
    properties.put("maxBufferSize", maxBufferSize);
    properties.put("compressionCutoffSize", compressionCutoffSize);
    properties.put("messageLifeTimeInMinutes", messageLifeTimeInMinutes);
    properties.put("sharedSecret", sharedSecret);
    properties.put("sendHighPriorityMessages", sendHighPriorityMessages);
    return properties;
  }
}

