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

package io.mapsmessaging.config.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class UdpConfig extends EndPointConfig {

  private long packetReuseTimeout;
  private long idleSessionTimeout;
  private long hmacHostLookupCacheExpiry;
  private List<HmacConfig> hmacConfigList;

  public UdpConfig(ConfigurationProperties config) {
    super(config);
    packetReuseTimeout = config.getLongProperty("packetReuseTimeout", 1000L);
    idleSessionTimeout = config.getLongProperty("idleSessionTimeout", 600);
    hmacHostLookupCacheExpiry = config.getLongProperty("HmacHostLookupCacheExpiry", 600);
    Object t = config.get("nodeConfiguration");
    if (t != null) {
      hmacConfigList = loadNodeConfig((List<ConfigurationProperties>) t);
    } else {
      hmacConfigList = new ArrayList<>();
    }
  }

  private List<HmacConfig> loadNodeConfig(List<ConfigurationProperties> nodes) {
    List<HmacConfig> list = new ArrayList<>();
    for (ConfigurationProperties node : nodes) {
      list.add(new HmacConfig(node));
    }
    return list;
  }

  public boolean update(UdpConfig newConfig) {
    boolean hasChanged = super.update(newConfig);
    if (packetReuseTimeout != newConfig.packetReuseTimeout) {
      packetReuseTimeout = newConfig.packetReuseTimeout;
      hasChanged = true;
    }
    if (idleSessionTimeout != newConfig.idleSessionTimeout) {
      idleSessionTimeout = newConfig.idleSessionTimeout;
      hasChanged = true;
    }
    if (hmacHostLookupCacheExpiry != newConfig.hmacHostLookupCacheExpiry) {
      hmacHostLookupCacheExpiry = newConfig.hmacHostLookupCacheExpiry;
      hasChanged = true;
    }

    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = super.toConfigurationProperties();
    config.put("packetReuseTimeout", packetReuseTimeout);
    config.put("idleSessionTimeout", idleSessionTimeout);
    config.put("HmacHostLookupCacheExpiry", hmacHostLookupCacheExpiry);

    List<ConfigurationProperties> maps = new ArrayList<>();
    for (HmacConfig hmacConfig : hmacConfigList) {
      maps.add(hmacConfig.toConfigurationProperties());
    }
    config.put("nodeConfiguration", maps);
    return config;
  }
}
