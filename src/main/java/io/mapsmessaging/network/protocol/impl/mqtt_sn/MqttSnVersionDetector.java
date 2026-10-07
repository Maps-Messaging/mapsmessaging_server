/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import java.nio.ByteBuffer;

/**
 * Distinguishes MQTT-SN CONNECT packets without assuming that the two versions
 * share control-packet identifiers or CONNECT field positions.
 *
 * <p>Only a CONNECT can establish a version for a new endpoint. Other packets
 * require a version already associated with the endpoint or explicit gateway
 * discovery handling. In particular, the v2 CONNECT identifier (0x01) is the
 * v1.2 SEARCHGW identifier.</p>
 */
public final class MqttSnVersionDetector {

  public enum Version { V1_2, V2_0, UNKNOWN }

  private MqttSnVersionDetector() {
  }

  public static Version detect(ByteBuffer wire) {
    ByteBuffer buffer = wire.asReadOnlyBuffer();
    int available = buffer.remaining();
    if (available < 2) {
      return Version.UNKNOWN;
    }
    int base = buffer.position();
    int firstLength = Byte.toUnsignedInt(buffer.get(base));
    int headerLength = firstLength == 1 ? 4 : 2;
    if (available < headerLength) {
      return Version.UNKNOWN;
    }
    int declaredLength = firstLength == 1
        ? (Byte.toUnsignedInt(buffer.get(base + 1)) << 8)
            | Byte.toUnsignedInt(buffer.get(base + 2))
        : firstLength;
    if (declaredLength != available || declaredLength < headerLength) {
      return Version.UNKNOWN;
    }
    int type = Byte.toUnsignedInt(buffer.get(base + headerLength - 1));
    int payload = base + headerLength;
    int remaining = available - headerLength;
    if (type == 0x04 && remaining >= 4) {
      int flags = Byte.toUnsignedInt(buffer.get(payload));
      int protocolId = Byte.toUnsignedInt(buffer.get(payload + 1));
      return (flags & 0xF3) == 0 && protocolId == 1 ? Version.V1_2 : Version.UNKNOWN;
    }
    if (type == 0x01 && remaining >= 8) {
      int flags = Byte.toUnsignedInt(buffer.get(payload));
      int packetIdentifier = (Byte.toUnsignedInt(buffer.get(payload + 1)) << 8)
          | Byte.toUnsignedInt(buffer.get(payload + 2));
      int protocolId = Byte.toUnsignedInt(buffer.get(payload + 3));
      return (flags & 0x98) == 0 && packetIdentifier != 0 && protocolId == 2
          ? Version.V2_0 : Version.UNKNOWN;
    }
    return Version.UNKNOWN;
  }
}
