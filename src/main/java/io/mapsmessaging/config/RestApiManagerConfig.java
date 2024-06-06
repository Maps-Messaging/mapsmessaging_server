/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

import io.mapsmessaging.config.network.TlsConfig;
import io.mapsmessaging.config.rest.StaticConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper=true)
@Schema(description = "Rest API Configuration")
public class RestApiManagerConfig extends Config {

  private boolean enabled;
  private boolean enableAuthentication;
  private String hostnames;
  private int port;
  private boolean enableSwagger;
  private boolean enableSwaggerUI;
  private boolean enableUserManagement;
  private boolean enableSchemaManagement;
  private boolean enableInterfaceManagement;
  private boolean enableDestinationManagement;
  private TlsConfig tlsConfig;
  private StaticConfig staticConfig;

  public static RestApiManagerConfig getInstance(){
    return new RestApiManagerConfig( ConfigurationManager.getInstance().getProperties("RestApi"));
  }

  private RestApiManagerConfig(ConfigurationProperties properties) {
    enabled = properties.getBooleanProperty("enabled", true);
    enableAuthentication = properties.getBooleanProperty("enableAuthentication", true);
    hostnames = properties.getProperty("hostnames", "0.0.0.0");
    port = properties.getIntProperty("port", 8080);
    enableSwagger = properties.getBooleanProperty("enableSwagger", true);
    enableSwaggerUI = properties.getBooleanProperty("enableSwaggerUI", true);
    enableUserManagement = properties.getBooleanProperty("enableUserManagement", true);
    enableSchemaManagement = properties.getBooleanProperty("enableSchemaManagement", true);
    enableInterfaceManagement = properties.getBooleanProperty("enableInterfaceManagement", true);
    enableDestinationManagement = properties.getBooleanProperty("enableDestinationManagement", true);

    if (properties.containsKey("tls")) {
      tlsConfig = new TlsConfig((ConfigurationProperties) properties.get("tls"));
    }

    if (properties.containsKey("static")) {
      staticConfig = new StaticConfig((ConfigurationProperties) properties.get("static"));
    }
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", enabled);
    properties.put("enableAuthentication", enableAuthentication);
    properties.put("hostnames", hostnames);
    properties.put("port", port);
    properties.put("enableSwagger", enableSwagger);
    properties.put("enableSwaggerUI", enableSwaggerUI);
    properties.put("enableUserManagement", enableUserManagement);
    properties.put("enableSchemaManagement", enableSchemaManagement);
    properties.put("enableInterfaceManagement", enableInterfaceManagement);
    properties.put("enableDestinationManagement", enableDestinationManagement);

    if (tlsConfig != null) {
      properties.put("ssl", tlsConfig.toConfigurationProperties());
    }

    if (staticConfig != null) {
      properties.put("static", staticConfig.toConfigurationProperties());
    }

    return properties;
  }
}
