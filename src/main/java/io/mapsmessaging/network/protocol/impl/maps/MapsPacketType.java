/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps;

import java.io.IOException;

public enum MapsPacketType {
  CONNECT(0x01),
  CONNACK(0x02),
  PING(0x03),
  PONG(0x04),
  DISCONNECT(0x05),

  PUBLISH(0x20),
  PUBACK(0x21),

  SUBSCRIBE(0x40),
  SUBACK(0x41),
  UNSUBSCRIBE(0x42),
  UNSUBACK(0x43);

  private final int value;

  MapsPacketType(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }

  public static MapsPacketType fromValue(int value) throws IOException {
    for (MapsPacketType type : values()) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IOException("Unknown MAPS packet type " + value);
  }
}
