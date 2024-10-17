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


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AuthManagerConfig.class, name = "authManager"),
    @JsonSubTypes.Type(value = DestinationManagerConfig.class, name = "destinationManagerConfig"),
    @JsonSubTypes.Type(value = DiscoveryManagerConfig.class, name = "discoveryManager"),
    @JsonSubTypes.Type(value = DeviceManagerConfig.class, name = "deviceManagerConfig"),
    @JsonSubTypes.Type(value = JolokiaConfig.class, name = "jolokiaConfig"),
    @JsonSubTypes.Type(value = LoRaDeviceManagerConfig.class, name = "loRaDeviceManagerConfig"),
    @JsonSubTypes.Type(value = MessageDaemonConfig.class, name = "messageDaemon"),
    @JsonSubTypes.Type(value = NetworkConnectionManagerConfig.class, name = "networkConnectionManagerConfig"),
    @JsonSubTypes.Type(value = NetworkManagerConfig.class, name = "networkManagerConfig"),
    @JsonSubTypes.Type(value = RestApiManagerConfig.class, name = "restApiConfig"),
    @JsonSubTypes.Type(value = RoutingManagerConfig.class, name = "routingConfig"),
    @JsonSubTypes.Type(value = TenantManagementConfig.class, name = "tenantConfig"),
})
@Schema(
    description = "Abstract base class for all server configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "authManager", schema = AuthManagerConfig.class),
        @DiscriminatorMapping(value = "destinationManagerConfig", schema = DestinationManagerConfig.class),
        @DiscriminatorMapping(value = "discoveryManager", schema = DiscoveryManagerConfig.class),
        @DiscriminatorMapping(value = "deviceManagerConfig", schema = DeviceManagerConfig.class),
        @DiscriminatorMapping(value = "jolokiaConfig", schema = JolokiaConfig.class),
        @DiscriminatorMapping(value = "loRaDeviceManagerConfig", schema = LoRaDeviceManagerConfig.class),
        @DiscriminatorMapping(value = "messageDaemon", schema = MessageDaemonConfig.class),
        @DiscriminatorMapping(value = "networkConnectionManagerConfig", schema = NetworkConnectionManagerConfig.class),
        @DiscriminatorMapping(value = "networkManagerConfig", schema = NetworkManagerConfig.class),
        @DiscriminatorMapping(value = "restApiConfig", schema = RestApiManagerConfig.class),
        @DiscriminatorMapping(value = "routingConfig", schema = RoutingManagerConfig.class),
        @DiscriminatorMapping(value = "tenantConfig", schema = TenantManagementConfig.class)
    })

public abstract class ManagementConfig {

  public abstract boolean update(ManagementConfig config);

  public abstract ConfigurationProperties toConfigurationProperties();

  protected long parseBufferSize(String size) {
    size = size.trim().toUpperCase();
    if (size.endsWith("K")) {
      return Long.parseLong(size.substring(0, size.length() - 1)) * 1024;
    } else if (size.endsWith("M")) {
      return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024;
    } else if (size.endsWith("G")) {
      return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024 * 1024;
    } else {
      return Long.parseLong(size);
    }
  }

  protected String formatBufferSize(long size) {
    if (size >= 1024 * 1024 * 1024) {
      return (size / (1024 * 1024 * 1024)) + "G";
    } else if (size >= 1024 * 1024) {
      return (size / (1024 * 1024)) + "M";
    } else if (size >= 1024) {
      return (size / 1024) + "K";
    } else {
      return Long.toString(size);
    }
  }

  protected boolean updateMap(Map<String, Object> currentMap, Map<String, Object> newMap) {
    boolean hasChanged = false;

    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
      String key = entry.getKey();
      Object newValue = entry.getValue();

      if (!Objects.equals(currentMap.get(key), newValue)) {
        currentMap.put(key, newValue);
        hasChanged = true;
      }
    }

    return hasChanged;
  }
}
