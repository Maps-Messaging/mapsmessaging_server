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

package io.mapsmessaging.dto.rest.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.protocol.impl.*;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AmqpProtocolInformation.class, name = "amqp"),
    @JsonSubTypes.Type(value = CoapProtocolInformation.class, name = "coap"),
    @JsonSubTypes.Type(value = LoraProtocolInformation.class, name = "lora"),
    @JsonSubTypes.Type(value = MqttProtocolInformation.class, name = "mqtt"),
    @JsonSubTypes.Type(value = MqttSnProtocolInformation.class, name = "mqtt-sn"),
    @JsonSubTypes.Type(value = MqttV5ProtocolInformation.class, name = "mqttV5"),
    @JsonSubTypes.Type(value = NmeaProtocolInformation.class, name = "NMEA-0183"),
    @JsonSubTypes.Type(value = SemtechProtocolInformation.class, name = "semtech"),
    @JsonSubTypes.Type(value = StompProtocolInformation.class, name = "stomp"),
    @JsonSubTypes.Type(value = RestProtocolInformation.class, name = "rest"),
    @JsonSubTypes.Type(value = ExtensionProtocolInformation.class, name = "extension"),
    @JsonSubTypes.Type(value = SatelliteProtocolInformation.class, name = "orbcomm"),
    @JsonSubTypes.Type(value = SatelliteDeviceProtocolInformation.class, name = "satellite"),
})
@Schema(
    title = "Protocol Information",
    description = "Provides detailed information about the protocol and session",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "amqp", schema = AmqpProtocolInformation.class),
        @DiscriminatorMapping(value = "coap", schema = CoapProtocolInformation.class),
        @DiscriminatorMapping(value = "lora", schema = LoraProtocolInformation.class),
        @DiscriminatorMapping(value = "mqtt", schema = MqttProtocolInformation.class),
        @DiscriminatorMapping(value = "mqtt-sn", schema = MqttSnProtocolInformation.class),
        @DiscriminatorMapping(value = "mqttV5", schema = MqttV5ProtocolInformation.class),
        @DiscriminatorMapping(value = "NMEA-0183", schema = NmeaProtocolInformation.class),
        @DiscriminatorMapping(value = "semtech", schema = SemtechProtocolInformation.class),
        @DiscriminatorMapping(value = "stomp", schema = StompProtocolInformation.class),
        @DiscriminatorMapping(value = "rest", schema = RestProtocolInformation.class),
        @DiscriminatorMapping(value = "extension", schema = ExtensionProtocolInformation.class),
        @DiscriminatorMapping(value = "orbcomm", schema = SatelliteProtocolInformation.class),
        @DiscriminatorMapping(value = "satellite", schema = SatelliteDeviceProtocolInformation.class),

    },
    requiredProperties = {"type"}
)
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class ProtocolInformationDTO {

  @Schema(description = "Type of the protocol", allowableValues = {
      "amqp", "coap", "lora", "mqtt", "mqtt-sn", "mqttV5", "NMEA-0183", "semtech", "stomp", "rest", "extension", "orbcomm", "satellite"
  })
  protected String type;

  @Schema(
      description = "Unique identifier of the session",
      example = "session-12345"
  )
  private String sessionId;

  @Schema(
      description = "Timeout in milliseconds before the protocol session is considered inactive",
      example = "30000"
  )
  private long timeout;

  @Schema(
      description = "Keep-alive interval in milliseconds for protocol connections",
      example = "15000"
  )
  private long keepAlive;

  @Schema(
      description = "Name of the message transformation applied to this protocol",
      example = "default-transformation"
  )
  private String messageTransformationName;

  @Schema(
      description = "Mapping of selectors to protocol-specific expressions",
      example = "{\"temperature\": \"> 20\", \"status\": \"active\"}"
  )
  private Map<String, String> selectorMapping;

  @Schema(
      description = "Mapping of destinations to transformation names",
      example = "{\"alerts\": \"alert-transform\", \"telemetry\": \"telemetry-transform\"}"
  )
  private Map<String, String> destinationTransformationMapping;
}
