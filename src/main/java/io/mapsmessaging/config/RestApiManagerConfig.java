/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.config;

import io.mapsmessaging.config.network.impl.TlsConfig;
import io.mapsmessaging.config.rest.StaticConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.RestApiManagerConfigDTO;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

public class RestApiManagerConfig extends RestApiManagerConfigDTO implements Config {

  public static RestApiManagerConfig getInstance() {
    return new RestApiManagerConfig(ConfigurationManager.getInstance().getProperties("RestApi"));
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

    if (properties.containsKey("tls")) {
      this.tlsConfig = new TlsConfig((ConfigurationProperties) properties.get("tls"));
    }

    if (properties.containsKey("static")) {
      this.staticConfig = new StaticConfig((ConfigurationProperties) properties.get("static"));
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
    if (this.enableSwagger != newConfig.isEnableSwagger()) {
      this.enableSwagger = newConfig.isEnableSwagger();
      hasChanged = true;
    }
    if (this.enableSwaggerUI != newConfig.isEnableSwaggerUI()) {
      this.enableSwaggerUI = newConfig.isEnableSwaggerUI();
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

    if (this.tlsConfig != null) {
      properties.put("tls", this.tlsConfig.toConfigurationProperties());
    }

    if (this.staticConfig != null) {
      properties.put("static", this.staticConfig.toConfigurationProperties());
    }

    return properties;
  }
}
