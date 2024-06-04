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

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.auth.SaslConfig;
import io.mapsmessaging.config.protocol.*;
import io.mapsmessaging.config.protocol.LoRaConfig;
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
public class EndPointServerConfig extends Config {

  private String name;
  private String url;
  private EndPointConfig endPointConfig;
  private SaslConfig saslConfig;
  private List<ProtocolConfig> protocolConfigs;
  private String protocols;
  private String authenticationRealm;
  private int backlog;
  private int selectorTaskWait;

  public EndPointServerConfig(ConfigurationProperties config) {
    this.name = config.getProperty("name");
    this.url = config.getProperty("url");
    this.backlog = config.getIntProperty("backlog", 100);
    this.selectorTaskWait = config.getIntProperty("taskWait", 10);
    this.authenticationRealm = config.getProperty("auth", "");
    endPointConfig = url != null ? createEndPointConfig(url, config) : null;
    protocolConfigs = new ArrayList<>();

    ConfigurationProperties saslConfiguration = (ConfigurationProperties) config.get("sasl");
    if (saslConfiguration != null) {
      saslConfig = new SaslConfig(saslConfiguration);
    }

    protocols = config.getProperty("protocol");
    if (protocols != null && !protocols.isEmpty()) {
      String[] protocolArray = protocols.split(",");
      for (String protocol : protocolArray) {
        ProtocolConfig protocolConfig = createProtocolConfig(protocol, config);
        if (protocolConfig != null) {
          protocolConfigs.add(protocolConfig);
        }
      }
    }
  }

  public ProtocolConfig getProtocolConfig(String protocol) {
    return protocolConfigs.stream()
        .filter(protocolConfig -> protocolConfig.getType().equalsIgnoreCase(protocol))
        .findFirst()
        .orElse(null);
  }

  private EndPointConfig createEndPointConfig(String url, ConfigurationProperties properties) {
    if (url.toLowerCase().startsWith("tcp") || url.toLowerCase().startsWith("ws")) {
      return new TcpConfig(properties);
    } else if (url.toLowerCase().startsWith("ssl") || url.toLowerCase().startsWith("wss")) {
      return new TlsConfig(properties);
    } else if (url.toLowerCase().startsWith("udp") || url.toLowerCase().startsWith("hmac")) {
      return new UdpConfig(properties);
    } else if (url.toLowerCase().startsWith("dtls")) {
      return new DtlsConfig(properties);
    } else if (url.toLowerCase().startsWith("serial") || url.toLowerCase().startsWith("lora")) {
      return new SerialConfig(properties);
    }
    return null;
  }

  public ProtocolConfig createProtocolConfig(String protocol, ConfigurationProperties config) {
    if (protocol.equalsIgnoreCase("mqtt")) {
      return new MqttV5Config(config);
    } else if (protocol.equalsIgnoreCase("amqp")) {
      return new AmqpConfig(config);
    } else if (protocol.equalsIgnoreCase("stomp")) {
      return new StompConfig(config);
    } else if (protocol.equalsIgnoreCase("semtech")) {
      return new SemtechConfig(config);
    } else if (protocol.equalsIgnoreCase("mqtt-sn")) {
      return new MqttSnConfig(config);
    } else if (protocol.equalsIgnoreCase("coap")) {
      return new CoapConfig(config);
    } else if (protocol.equalsIgnoreCase("nmea")) {
      return new NmeaConfig(config);
    } else if (protocol.equalsIgnoreCase("lora")) {
      return new LoRaConfig(config);
    }
    return null;
  }

  public boolean update(EndPointServerConfig newConfig) {
    boolean hasChanged = false;
    if (!this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }
    if (!this.authenticationRealm.equals(newConfig.getAuthenticationRealm())) {
      this.authenticationRealm = newConfig.getAuthenticationRealm();
      hasChanged = true;
    }
    if (this.backlog != newConfig.getBacklog()) {
      this.backlog = newConfig.getBacklog();
      hasChanged = true;
    }
    if (this.selectorTaskWait != newConfig.getSelectorTaskWait()) {
      this.selectorTaskWait = newConfig.getSelectorTaskWait();
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties protocolMap = new ConfigurationProperties();
    for (ProtocolConfig protocolConfig : protocolConfigs) {
      protocolMap.put(protocolConfig.getName(), protocolConfig.toConfigurationProperties());
    }
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("name", this.name);
    config.put("url", this.url);
    config.put("endPoint", this.endPointConfig);
    config.put("protocols", protocols);
    config.put("backlog", this.backlog);
    config.put("selectorTaskWait", this.selectorTaskWait);
    config.put("auth", this.authenticationRealm);
    config.put("data", protocolMap);
    return config;
  }
}
