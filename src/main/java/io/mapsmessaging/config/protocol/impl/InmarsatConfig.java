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
import io.mapsmessaging.dto.rest.config.protocol.impl.InmarsatDTO;

import java.util.Objects;

public class InmarsatConfig extends InmarsatDTO implements Config {

  public InmarsatConfig(ConfigurationProperties config) {
    setType("inmarsat");
    ProtocolConfigFactory.unpack(config, this);

    baseUrl = config.getProperty("baseUrl", "https://api-sandbox.inmarsat.com/v1/");
    pollInterval = config.getIntProperty("pollInterval", 15);
    httpRequestTimeout = config.getIntProperty("httpRequestTimeoutSec", 30);
    maxInflightEventsPerDevice = config.getIntProperty("maxInflightEventsPerDevice", 2);
    outboundNamespaceRoot = config.getProperty("outboundNamespaceRoot", "");
    outboundBroadcast = config.getProperty("outboundBroadcast", "");

    if(pollInterval<10){
      pollInterval = 10;
    }
    mailboxId = (config.getProperty("mailboxId"));
    mailboxPassword = (config.getProperty("mailboxPassword"));
    namespace = (config.getProperty("namespaceRoot"));
  }


  @Override
  public boolean update(BaseConfigDTO cfg) {
    boolean changed = false;
    if (cfg instanceof InmarsatDTO dto) {
      changed = ProtocolConfigFactory.update(this, dto);

      if (!Objects.equals(baseUrl, dto.getBaseUrl())) {
        baseUrl = dto.getBaseUrl();
        changed = true;
      }

      if (pollInterval != dto.getPollInterval()) {
        pollInterval = dto.getPollInterval();
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
      if (!outboundNamespaceRoot.equals(dto.getOutboundNamespaceRoot())) {
        outboundNamespaceRoot = dto.getOutboundNamespaceRoot();
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
      if(!namespace.equals(dto.getNamespace())) {
        namespace = dto.getNamespace();
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
    properties.put("pollInterval", pollInterval);
    properties.put("httpRequestTimeoutSec", httpRequestTimeout);
    properties.put("maxInflightEventsPerDevice", maxInflightEventsPerDevice);
    properties.put("outboundNamespaceRoot", outboundNamespaceRoot);
    properties.put("outboundBroadcast", outboundBroadcast);
    properties.put("mailboxId", mailboxId);
    properties.put("mailboxPassword", mailboxPassword);
    properties.put("namespaceRoot", namespace);
    return properties;
  }
}
