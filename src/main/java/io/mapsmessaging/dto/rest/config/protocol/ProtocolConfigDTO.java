/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.dto.rest.config.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.*;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AmqpConfigDTO.class, name = "amqp"),
    @JsonSubTypes.Type(value = CoapConfigDTO.class, name = "coap"),
    @JsonSubTypes.Type(value = LoRaProtocolConfigDTO.class, name = "lora"),
    @JsonSubTypes.Type(value = MqttConfigDTO.class, name = "mqtt"),
    @JsonSubTypes.Type(value = MqttSnConfigDTO.class, name = "mqtt-sn"),
    @JsonSubTypes.Type(value = MqttV5ConfigDTO.class, name = "mqttV5"),
    @JsonSubTypes.Type(value = NmeaConfigDTO.class, name = "nmea"),
    @JsonSubTypes.Type(value = SemtechConfigDTO.class, name = "semtech"),
    @JsonSubTypes.Type(value = StompConfigDTO.class, name = "stomp"),
    @JsonSubTypes.Type(value = WebSocketConfigDTO.class, name = "websocket")
})
@Schema(
    description = "Abstract base class for all protocol configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "amqp", schema = AmqpConfigDTO.class),
        @DiscriminatorMapping(value = "coap", schema = CoapConfigDTO.class),
        @DiscriminatorMapping(value = "lora", schema = LoRaProtocolConfigDTO.class),
        @DiscriminatorMapping(value = "mqtt", schema = MqttConfigDTO.class),
        @DiscriminatorMapping(value = "mqtt-sn", schema = MqttSnConfigDTO.class),
        @DiscriminatorMapping(value = "mqttV5", schema = MqttV5ConfigDTO.class),
        @DiscriminatorMapping(value = "nmea", schema = NmeaConfigDTO.class),
        @DiscriminatorMapping(value = "semtech", schema = SemtechConfigDTO.class),
        @DiscriminatorMapping(value = "stomp", schema = StompConfigDTO.class),
        @DiscriminatorMapping(value = "websocket", schema = WebSocketConfigDTO.class)
    },
    requiredProperties = {"type"}
)
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public abstract class ProtocolConfigDTO extends BaseConfigDTO {

  @Schema(description = "Type of the protocol configuration", allowableValues = {
      "amqp", "coap", "lora", "mqtt", "mqtt-sn", "mqttV5", "nmea", "semtech", "stomp", "websocket"
  })
  protected String type;

  @Schema(description = "Remote authentication configuration for the protocol")
  protected ConnectionAuthConfigDTO remoteAuthConfig;
}
