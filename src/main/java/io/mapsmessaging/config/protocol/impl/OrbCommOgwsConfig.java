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
import io.mapsmessaging.dto.rest.config.protocol.impl.OrbCommOgwsDTO;

public class OrbCommOgwsConfig extends OrbCommOgwsDTO implements Config {

  public OrbCommOgwsConfig(ConfigurationProperties config) {
    setType("ogws");
    ProtocolConfigFactory.unpack(config, this);
    baseUrl = config.getProperty("baseUrl", "https://ogws.orbcomm.com/api/v1.0");
    clientId = config.getProperty("clientId");
    clientSecret = config.getProperty("clientSecret");
    pollInterval = config.getIntProperty("pollInterval", 10);
    httpRequestTimeout = config.getIntProperty("httpRequestTimeoutSec", 10);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean result = false;
    if (config instanceof OrbCommOgwsDTO){
      OrbCommOgwsDTO orbCommOgwsDTO = (OrbCommOgwsDTO)config;
      result = ProtocolConfigFactory.update(this, orbCommOgwsDTO);

      if(!clientId.equalsIgnoreCase(orbCommOgwsDTO.getClientId()) ){
        clientId = orbCommOgwsDTO.getClientId();
        result = true;
      }
      if(!clientSecret.equalsIgnoreCase(orbCommOgwsDTO.getClientSecret()) ){
        clientSecret = orbCommOgwsDTO.getClientSecret();
        result = true;
      }
      if(!baseUrl.equalsIgnoreCase(orbCommOgwsDTO.getBaseUrl()) ){
        baseUrl = orbCommOgwsDTO.getBaseUrl();
        result = true;
      }
      if(pollInterval != orbCommOgwsDTO.getPollInterval() ){
        pollInterval = orbCommOgwsDTO.getPollInterval();
        result = true;
      }
      if(httpRequestTimeout != orbCommOgwsDTO.getHttpRequestTimeout() ){
        httpRequestTimeout = orbCommOgwsDTO.getHttpRequestTimeout();
        result = true;
      }
    }
    return result;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("baseUrl", baseUrl);
    properties.put("clientId", clientId);
    properties.put("clientSecret", clientSecret);
    properties.put("pollInterval", pollInterval);
    properties.put("httpRequestTimeoutSec", httpRequestTimeout);
    return properties;
  }
}

