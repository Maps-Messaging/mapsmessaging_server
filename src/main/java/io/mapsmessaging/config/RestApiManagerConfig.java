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

package io.mapsmessaging.config;

import io.mapsmessaging.config.network.impl.TlsConfig;
import io.mapsmessaging.config.rest.StaticConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.RestApiManagerConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.rest.handler.CorsHeaderManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class RestApiManagerConfig extends RestApiManagerConfigDTO implements Config, ConfigManager {

  public static RestApiManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(RestApiManagerConfig.class);
  }

  private RestApiManagerConfig(ConfigurationProperties properties) {
    this.enabled = properties.getBooleanProperty("enabled", true);
    this.enableAuthentication = properties.getBooleanProperty("enableAuthentication", true);
    this.hostnames = properties.getProperty("hostnames", "0.0.0.0");
    this.port = properties.getIntProperty("port", 8080);
    this.enableSwagger = properties.getBooleanProperty("enableSwagger", true);
    this.enableSwaggerUI = properties.getBooleanProperty("enableSwaggerUI", true);
    this.enableUserManagement = properties.getBooleanProperty("enableUserManagement", true);
    this.enableSchemaManagement = properties.getBooleanProperty("enableSchemaManagement", true);
    this.enableInterfaceManagement = properties.getBooleanProperty("enableInterfaceManagement", true);
    this.enableDestinationManagement = properties.getBooleanProperty("enableDestinationManagement", true);

    this.maxEventsPerDestination = properties.getIntProperty("maxEventsPerDestination", 10);
    this.maxThreads = properties.getIntProperty("maxThreads",10);
    this.minThreads = properties.getIntProperty("minThreads",5);
    this.threadQueueLimit  = properties.getIntProperty("threadQueueLimit ",100);
    this.selectorThreads = properties.getIntProperty("selectorThreads ",2);

    this.enableCache = properties.getBooleanProperty("enableCache", true);
    this.cacheLifetime = properties.getLongProperty("cacheLifetime", 10000L);
    this.cacheCleanup = properties.getLongProperty("cacheCleanup", 5000L);
    this.enableDestinationManagement = properties.getBooleanProperty("enableDestinationManagement", true);
    this.enableWadlEndPoint = properties.getBooleanProperty("enableWadlEndPoint", false);
    this.inactiveTimeout = properties.getIntProperty("inactiveTimeout", 180000);

    if (properties.containsKey("tls")) {
      this.tlsConfig = new TlsConfig((ConfigurationProperties) properties.get("tls"));
    }

    if (properties.containsKey("static")) {
      this.staticConfig = new StaticConfig((ConfigurationProperties) properties.get("static"));
    }

    corsHeaders = CorsHeaderManager.getInstance().getCorsHeaders();
    if(properties.containsKey("corsHeaders")) {
      Map<String, Object> corsHeadersProp = ((ConfigurationProperties) properties.get("corsHeaders")).getMap();
      for(Map.Entry<String, Object> entry : corsHeadersProp.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue().toString();
        corsHeaders.addHeader(key, value);
      }
    }
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof RestApiManagerConfigDTO)) {
      return false;
    }

    RestApiManagerConfigDTO newConfig = (RestApiManagerConfigDTO) config;
    boolean hasChanged = false;

    if (this.enabled != newConfig.isEnabled()) {
      this.enabled = newConfig.isEnabled();
      hasChanged = true;
    }
    if (this.enableAuthentication != newConfig.isEnableAuthentication()) {
      this.enableAuthentication = newConfig.isEnableAuthentication();
      hasChanged = true;
    }
    if (!this.hostnames.equals(newConfig.getHostnames())) {
      this.hostnames = newConfig.getHostnames();
      hasChanged = true;
    }
    if (this.port != newConfig.getPort()) {
      this.port = newConfig.getPort();
      hasChanged = true;
    }
    if (this.inactiveTimeout != newConfig.getInactiveTimeout()) {
      this.inactiveTimeout = newConfig.getInactiveTimeout();
      hasChanged = true;
    }
    if (this.enableSwagger != newConfig.isEnableSwagger()) {
      this.enableSwagger = newConfig.isEnableSwagger();
      hasChanged = true;
    }
    if (this.enableSwaggerUI != newConfig.isEnableSwaggerUI()) {
      this.enableSwaggerUI = newConfig.isEnableSwaggerUI();
      hasChanged = true;
    }
    if (this.enableWadlEndPoint != newConfig.isEnableWadlEndPoint()) {
      this.enableWadlEndPoint = newConfig.isEnableWadlEndPoint();
      hasChanged = true;
    }
    if (this.enableUserManagement != newConfig.isEnableUserManagement()) {
      this.enableUserManagement = newConfig.isEnableUserManagement();
      hasChanged = true;
    }
    if (this.enableSchemaManagement != newConfig.isEnableSchemaManagement()) {
      this.enableSchemaManagement = newConfig.isEnableSchemaManagement();
      hasChanged = true;
    }
    if (this.enableInterfaceManagement != newConfig.isEnableInterfaceManagement()) {
      this.enableInterfaceManagement = newConfig.isEnableInterfaceManagement();
      hasChanged = true;
    }
    if (this.enableDestinationManagement != newConfig.isEnableDestinationManagement()) {
      this.enableDestinationManagement = newConfig.isEnableDestinationManagement();
      hasChanged = true;
    }
    if (this.tlsConfig != null && !this.tlsConfig.equals(newConfig.getTlsConfig())) {
      this.tlsConfig = newConfig.getTlsConfig();
      hasChanged = true;
    }
    if (this.staticConfig != null && !this.staticConfig.equals(newConfig.getStaticConfig())) {
      this.staticConfig = newConfig.getStaticConfig();
      hasChanged = true;
    }
    if(!corsHeaders.equals(newConfig.getCorsHeaders())) {
      corsHeaders = newConfig.getCorsHeaders();
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", this.enabled);
    properties.put("enableAuthentication", this.enableAuthentication);
    properties.put("hostnames", this.hostnames);
    properties.put("port", this.port);
    properties.put("enableSwagger", this.enableSwagger);
    properties.put("enableSwaggerUI", this.enableSwaggerUI);
    properties.put("enableUserManagement", this.enableUserManagement);
    properties.put("enableSchemaManagement", this.enableSchemaManagement);
    properties.put("enableInterfaceManagement", this.enableInterfaceManagement);
    properties.put("enableDestinationManagement", this.enableDestinationManagement);
    properties.put("inactiveTimeout", this.inactiveTimeout);
    properties.put("enableWadlEndPoint", this.enableWadlEndPoint);
    properties.put("maxEventsPerDestination", this.maxEventsPerDestination);

    if (this.tlsConfig != null) {
      properties.put("tls", this.tlsConfig.toConfigurationProperties());
    }

    if (this.staticConfig != null) {
      properties.put("static", this.staticConfig.toConfigurationProperties());
    }
    Map<String, Object> corsHeadersProp = new HashMap<>(CorsHeaderManager.getInstance().getCorsHeaders().getHeaders());
    properties.put("corsHeaders", corsHeadersProp);
    return properties;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new RestApiManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }


  @Override
  public String getName() {
    return "RestApi";
  }
}
