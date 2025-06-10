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
    if (server.getEndPointConfig() != null) {
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
    else{
      List<ConfigurationProperties> protocolConfig = (List<ConfigurationProperties>) config.get("protocols");
      loadSpecificProtocols(server, protocolConfig);
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
        if (protocol.equalsIgnoreCase("all")) {
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
        } else {
          protocolList.add(protocolName);
        }
      }

      List<ProtocolConfigDTO> protocolConfigs = new ArrayList<>();
      for (String protocolName : protocolList) {
        ProtocolConfigDTO protocolConfig = createProtocolConfig(protocolName.trim(), config);
        if (protocolConfig != null) {
          protocolConfigs.add(protocolConfig);
        }
      }
      server.setProtocolConfigs(protocolConfigs);
    }
  }

  private static List<ConfigurationProperties> packProtocolConfig(EndPointServerConfigDTO server) {
    List<ConfigurationProperties> protocolConfigs = new ArrayList<>();
    for(ProtocolConfigDTO protocolConfigDTO : server.getProtocolConfigs()){
      if(protocolConfigDTO instanceof Config){
        protocolConfigs.add  (((Config)protocolConfigDTO).toConfigurationProperties());
      }
    }
    return protocolConfigs;
  }

  private static EndPointConfigDTO createEndPointConfig(String url, ConfigurationProperties properties) {
    if (url.toLowerCase().startsWith("tcp") || url.toLowerCase().startsWith("ws")) {
      return new TcpConfig(properties);
    } else if (url.toLowerCase().startsWith("ssl") || url.toLowerCase().startsWith("wss")) {
      return new TlsConfig(properties);
    } else if (url.toLowerCase().startsWith("udp")
        || url.toLowerCase().startsWith("hmac")
        || url.toLowerCase().startsWith("lora")) {
      return new UdpConfig(properties);
    } else if (url.toLowerCase().startsWith("dtls")) {
      return new DtlsConfig(properties);
    } else if (url.toLowerCase().startsWith("serial")) {
      return new SerialConfig(properties);
    }
    return null;
  }

  private static ProtocolConfigDTO createProtocolConfig(String protocol, ConfigurationProperties config) {
    if (protocol.equalsIgnoreCase("mqtt")) {
      return new MqttV5Config(config);
    } else if (protocol.equalsIgnoreCase("amqp")) {
      return new AmqpConfig(config);
    } else if (protocol.equalsIgnoreCase("stomp")) {
      return new StompConfig(config);
    } else if (protocol.equalsIgnoreCase("nats")) {
      return new NatsConfig(config);
    } else if (protocol.equalsIgnoreCase("semtech")) {
      return new SemtechConfig(config);
    } else if (protocol.equalsIgnoreCase("mqtt-sn")) {
      return new MqttSnConfig(config);
    } else if (protocol.equalsIgnoreCase("coap")) {
      return new CoapConfig(config);
    } else if (protocol.equalsIgnoreCase("NMEA-0183")) {
      return new NmeaConfig(config);
    } else if (protocol.equalsIgnoreCase("lora")) {
      return new LoRaProtocolConfig(config);
    } else if (protocol.equalsIgnoreCase("LoRa_Gateway")) {
      return new LoraGatewayConfig(config);
    } else if (protocol.equalsIgnoreCase("echo")) {
      return new EchoProtocolConfig(config);
    }
    else if(protocol.equalsIgnoreCase("ws") ||
        protocol.equalsIgnoreCase("wss")){
      return new WebSocketConfig(config);
    }
    return new ExtensionConfig(config);
  }

  private EndPointConfigFactory() {}
}
