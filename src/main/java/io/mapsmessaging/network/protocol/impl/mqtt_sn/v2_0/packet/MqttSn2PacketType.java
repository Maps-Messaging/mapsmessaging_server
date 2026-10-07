/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.util.HashMap;
import java.util.Map;

/**
 * MQTT-SN 2.0 control packet types, August 2026 CSD01, section 2.1.3.
 * These are NOT compatible with the MQTT-SN 1.2 control packet identifiers.
 */
public enum MqttSn2PacketType {
  CONNECT(0x01),
  CONNACK(0x02),
  PUBLISH(0x03),
  PUBACK(0x04),
  PUBREC(0x05),
  PUBREL(0x06),
  PUBCOMP(0x07),
  SUBSCRIBE(0x08),
  SUBACK(0x09),
  UNSUBSCRIBE(0x0A),
  UNSUBACK(0x0B),
  PINGREQ(0x0C),
  PINGRESP(0x0D),
  DISCONNECT(0x0E),
  AUTH(0x0F),
  REGISTER(0x10),
  REGACK(0x11),
  PUBWOS(0x12),
  SLEEPREQ(0x13),
  SLEEPRESP(0x14),
  WAKEUP(0x15),
  ADVERTISE(0x16),
  SEARCHGW(0x17),
  GWINFO(0x18),
  FORWARDER_ENCAPSULATION(0xFC),
  CONNECTION_ENCAPSULATION(0xFE),
  PROTECTION_ENCAPSULATION(0xFF);

  private static final Map<Integer, MqttSn2PacketType> TYPES = new HashMap<>();

  static {
    for (MqttSn2PacketType type : values()) {
      TYPES.put(type.code, type);
    }
  }

  private final int code;

  MqttSn2PacketType(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }

  public static MqttSn2PacketType fromCode(int code) {
    if (code < 0 || code > 255) {
      throw new IllegalArgumentException("MQTT-SN 2.0 packet type must be one octet: " + code);
    }
    MqttSn2PacketType type = TYPES.get(code);
    if (type == null) {
      throw new IllegalArgumentException("Reserved MQTT-SN 2.0 packet type: " + code);
    }
    return type;
  }
}
