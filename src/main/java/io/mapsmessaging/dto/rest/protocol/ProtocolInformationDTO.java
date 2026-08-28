/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.dto.rest.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.protocol.impl.*;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AmqpProtocolInformation.class, name = "amqp"),
    @JsonSubTypes.Type(value = CoapProtocolInformation.class, name = "coap"),
    @JsonSubTypes.Type(value = LoraProtocolInformation.class, name = "lora"),
    @JsonSubTypes.Type(value = MapsProtocolInformation.class, name = "maps"),
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
    @JsonSubTypes.Type(value = N2kProtocolInformation.class, name = "n2k"),
})
@Schema(title = "Protocol Information", description = "Provides detailed information about the protocol and session", discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "amqp", schema = AmqpProtocolInformation.class),
        @DiscriminatorMapping(value = "coap", schema = CoapProtocolInformation.class),
        @DiscriminatorMapping(value = "lora", schema = LoraProtocolInformation.class),
        @DiscriminatorMapping(value = "maps", schema = MapsProtocolInformation.class),
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
        @DiscriminatorMapping(value = "n2k", schema = N2kProtocolInformation.class)
    }, requiredProperties = {"type"})
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class ProtocolInformationDTO {
  @Schema(description = "Type of the protocol", allowableValues = {"amqp", "coap", "lora", "maps", "mqtt", "mqtt-sn", "mqttV5", "NMEA-0183", "semtech", "stomp", "rest", "extension", "orbcomm", "mavlink", "n2k", "satellite"})
  protected String type;
  private String sessionId;
  private long timeout;
  private long keepAlive;
  private String messageTransformationName;
  private Map<String, String> selectorMapping;
  private Map<String, String> destinationTransformationMapping;
}
