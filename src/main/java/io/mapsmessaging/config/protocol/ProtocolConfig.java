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

package io.mapsmessaging.config.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.protocol.impl.*;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)

@JsonSubTypes({
  @JsonSubTypes.Type(value = AmqpConfig.class, name = "amqp"),
  @JsonSubTypes.Type(value = CoapConfig.class, name = "coap"),
  @JsonSubTypes.Type(value = LoRaProtocolConfig.class, name = "lora"),
  @JsonSubTypes.Type(value = MqttConfig.class, name = "mqtt"),
  @JsonSubTypes.Type(value = MqttSnConfig.class, name = "mqtt-sn"),
  @JsonSubTypes.Type(value = MqttV5Config.class, name = "mqttV5"),
  @JsonSubTypes.Type(value = NmeaConfig.class, name = "nmea"),
  @JsonSubTypes.Type(value = SemtechConfig.class, name = "semtech"),
  @JsonSubTypes.Type(value = StompConfig.class, name = "stomp"),
  @JsonSubTypes.Type(value = WebSocketConfig.class, name = "websocket")
})
@Schema(
    description = "Abstract base class for all schema configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
      @DiscriminatorMapping(value = "amqp", schema = AmqpConfig.class),
      @DiscriminatorMapping(value = "coap", schema = CoapConfig.class),
      @DiscriminatorMapping(value = "lora", schema = LoRaProtocolConfig.class),
      @DiscriminatorMapping(value = "mqtt", schema = MqttConfig.class),
      @DiscriminatorMapping(value = "mqtt-sn", schema = MqttSnConfig.class),
      @DiscriminatorMapping(value = "mqttV5", schema = MqttV5Config.class),
      @DiscriminatorMapping(value = "nmea", schema = NmeaConfig.class),
      @DiscriminatorMapping(value = "semtech", schema = SemtechConfig.class),
      @DiscriminatorMapping(value = "stomp", schema = StompConfig.class),
      @DiscriminatorMapping(value = "websocket", schema = WebSocketConfig.class)
    })
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public abstract class ProtocolConfig extends Config {

  private static final String REMOTE_AUTH_CONFIG ="remoteAuthConfig";
  private ConnectionAuthConfig remoteAuthConfig;
  private String type;

  public ProtocolConfig(ConfigurationProperties config) {
    if (config.getProperty(REMOTE_AUTH_CONFIG) != null) {
      remoteAuthConfig = new ConnectionAuthConfig((ConfigurationProperties) config.get(REMOTE_AUTH_CONFIG));
    }
  }

  public boolean update(ProtocolConfig newConfig) {
    boolean hasChanged = false;
    if (remoteAuthConfig != null && newConfig.getRemoteAuthConfig() != null) {
      hasChanged = remoteAuthConfig.update(newConfig.remoteAuthConfig);
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    if (remoteAuthConfig != null) {
      config.put(REMOTE_AUTH_CONFIG, remoteAuthConfig.toConfigurationProperties());
    }
    return config;
  }
}
