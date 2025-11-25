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

package io.mapsmessaging.config.network;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.auth.SaslConfig;
import io.mapsmessaging.config.network.impl.*;
import io.mapsmessaging.config.protocol.impl.*;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EndPointConfigFactory {

  public static boolean update(EndPointServerConfigDTO orig, EndPointServerConfigDTO upd) {
    boolean hasChanged = false;

      if (!orig.getName().equals(upd.getName())) {
        orig.setName(upd.getName());
        hasChanged = true;
      }
      if (!orig.getAuthenticationRealm().equals(upd.getAuthenticationRealm())) {
        orig.setAuthenticationRealm(upd.getAuthenticationRealm());
        hasChanged = true;
      }
      if (orig.getBacklog() != upd.getBacklog()) {
        orig.setBacklog(upd.getBacklog());
        hasChanged = true;
      }
      if (orig.getSelectorTaskWait() != upd.getSelectorTaskWait()) {
        orig.setSelectorTaskWait( upd.getSelectorTaskWait() );
        hasChanged = true;
      }

      if(orig.getEndPointConfig() != null && ((Config)orig.getEndPointConfig() ).update(upd.getEndPointConfig())){
        hasChanged = true;
      }
      if(orig.getSaslConfig() != null && ((Config)orig.getSaslConfig() ).update(upd.getSaslConfig())){
        hasChanged = true;
      }

    return hasChanged;
  }

  public static void pack(ConfigurationProperties config, EndPointServerConfigDTO server){
    config.put("name", server.getName());
    config.put("url", server.getUrl());
    config.put("backlog", server.getBacklog());
    config.put("selectorTaskWait", server.getSelectorTaskWait());
    config.put("auth", server.getAuthenticationRealm());
    config.put("protocols", packProtocolConfig(server)); // ToDo - Convert to Configuration props
    if (server.getEndPointConfig() instanceof Config) {
      config.put("endPoint", ((Config) server.getEndPointConfig()).toConfigurationProperties());
    }
    if(server.getSaslConfig() != null) {
      config.put("saslConfig", ((Config)server.getSaslConfig()).toConfigurationProperties());
    }
  }

  public static void unpack(ConfigurationProperties config, EndPointServerConfigDTO server) {
    server.setName(config.getProperty("name"));
    server.setUrl(config.getProperty("url"));
    server.setBacklog(config.getIntProperty("backlog", 100));
    server.setSelectorTaskWait(config.getIntProperty("taskWait", 10));
    server.setAuthenticationRealm(config.getProperty("auth", ""));

    server.setEndPointConfig( server.getUrl() != null ? createEndPointConfig(server.getUrl(), config) : null);

    ConfigurationProperties saslConfiguration = (ConfigurationProperties) config.get("sasl");
    if (saslConfiguration != null) {
      server.setSaslConfig(new SaslConfig(saslConfiguration));
    }

    if(config.containsKey("protocol")) {
      loadDefaultProtocols(config, server, config.getProperty("protocol"));
    }
    else if(config.containsKey("protocols")) {
      List<ConfigurationProperties> protocolConfig = (List<ConfigurationProperties>) config.get("protocols");
      loadSpecificProtocols(server, protocolConfig);
    }
    else{
      loadDefaultProtocols(config, server,"loop");
    }
  }

  private static void loadSpecificProtocols(EndPointServerConfigDTO server, List<ConfigurationProperties> protocolConfig) {
    List<ProtocolConfigDTO> protocolConfigs = new ArrayList<>();
    for(ConfigurationProperties prop : protocolConfig) {
      String type = prop.getProperty("type");
      protocolConfigs.add(createProtocolConfig(type, prop));
    }
    server.setProtocolConfigs(protocolConfigs);
  }

  private static void loadDefaultProtocols(ConfigurationProperties config, EndPointServerConfigDTO server, String protocol) {
    if (protocol != null && !protocol.isEmpty()) {
      String[] protocolArray = protocol.split(",");
      List<String> protocolList = new ArrayList<>();
      for (String protocolName : protocolArray) {
        if (protocolName.equalsIgnoreCase("all")) {
          protocolList.clear();
          if (server.getEndPointConfig() instanceof UdpConfig) {
            protocolList.add("coap");
            protocolList.add("mqtt-sn");
          } else {
            protocolList.add("amqp");
            protocolList.add("mqtt");
            protocolList.add("stomp");
            protocolList.add("nats");
            protocolList.add("ws");
          }
          break;
        } else {
          if(protocolName.equalsIgnoreCase("nmea")) {
            protocolName = "NMEA-0183";
          }
          protocolList.add(protocolName);
        }
      }

      List<ProtocolConfigDTO> protocolConfigs = new ArrayList<>();
      for (String protocolName : protocolList) {
        ProtocolConfigDTO protocolConfig = createProtocolConfig(protocolName.trim(), config);
        protocolConfigs.add(protocolConfig);
      }
      server.setProtocolConfigs(protocolConfigs);
    }
  }

  private static List<ConfigurationProperties> packProtocolConfig(EndPointServerConfigDTO server) {
    List<ConfigurationProperties> protocolConfigs = new ArrayList<>();
    for(ProtocolConfigDTO protocolConfigDTO : server.getProtocolConfigs()){
      if (protocolConfigDTO instanceof Config protocolConfig) {
        protocolConfigs.add(protocolConfig.toConfigurationProperties());
      }
    }
    return protocolConfigs;
  }
  private static EndPointConfigDTO createEndPointConfig(String url, ConfigurationProperties properties) {
    String u = url.toLowerCase();
    if (u.startsWith("tcp") || u.startsWith("ws")) return new TcpConfig(properties);
    if (u.startsWith("ssl") || u.startsWith("wss")) return new TlsConfig(properties);
    if (u.startsWith("udp") || u.startsWith("hmac") || u.startsWith("lora")) return new UdpConfig(properties);
    if (u.startsWith("dtls")) return new DtlsConfig(properties);
    if (u.startsWith("serial")) return new SerialConfig(properties);
    if (u.startsWith("satellite")) return new EndPointConfigDTO(); // placeholder
    return null;
  }

  private static ProtocolConfigDTO createProtocolConfig(String protocol, ConfigurationProperties config) {
    String p = protocol.toLowerCase(Locale.ROOT);
    return switch (p) {
      case "mqtt-v5" -> new MqttV5Config(config);
      case "mqtt-v3" -> new MqttConfig(config);
      case "mqtt" -> new MqttV5Config(config);
      case "amqp" -> new AmqpConfig(config);
      case "stomp" -> new StompConfig(config);
      case "nats" -> new NatsConfig(config);
      case "semtech" -> new SemtechConfig(config);
      case "mqtt-sn" -> new MqttSnConfig(config);
      case "coap" -> new CoapConfig(config);
      case "nmea-0183" -> new NmeaConfig(config);
      case "lora" -> new LoRaProtocolConfig(config);
      case "echo" -> new EchoProtocolConfig(config);
      case "stogi" -> new StoGiConfig(config);
      case "satellite" -> new SatelliteConfig(config);
      case "ws", "wss" -> new WebSocketConfig(config);
      default -> new ExtensionConfig(protocol, config);
    };
  }

  private EndPointConfigFactory() {}
}
