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

import io.mapsmessaging.config.protocol.MqttV5Config;
import io.mapsmessaging.config.protocol.ProtocolConfig;
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
public class EndPointServerConfig extends EndPointConfig {

  private String name;
  private String url;
  private EndPointConfig endPointConfig;
  private List<ProtocolConfig> protocolConfigs;

  public EndPointServerConfig(ConfigurationProperties config) {
    super(config);
    this.name = config.getProperty("name");
    endPointConfig = new EndPointConfig(config);

    String protocols = config.getProperty("protocol");
    protocolConfigs = new ArrayList<>();
    String[] protocolArray = protocols.split(",");
    for (String protocol : protocolArray) {
      if (protocol.equalsIgnoreCase("mqtt")) {
        protocolConfigs.add(new MqttV5Config(config));
      } else if (protocol.equalsIgnoreCase("amqp")) {
      } else if (protocol.equalsIgnoreCase("stomp")) {
      } else if (protocol.equalsIgnoreCase("semtech")) {
      } else if (protocol.equalsIgnoreCase("mqtt-sn")) {
      } else if (protocol.equalsIgnoreCase("coap")) {
      } else if (protocol.equalsIgnoreCase("nmea")) {
      }
    }
  }

  public boolean update(EndPointServerConfig newConfig) {
    boolean hasChanged = false;
    if (!this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }

    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties protocols = new ConfigurationProperties();
    for(ProtocolConfig protocolConfig : protocolConfigs) {
      protocols.put(protocolConfig.getName(), protocolConfig.toConfigurationProperties());
    }
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("name", this.name);
    config.put("url", this.url);
    config.put("endPoint", this.endPointConfig);
    config.put("protocols", protocols);
    return config;
  }
}
