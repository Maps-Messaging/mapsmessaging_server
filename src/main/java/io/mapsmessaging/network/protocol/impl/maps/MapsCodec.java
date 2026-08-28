/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class MapsCodec {

  private MapsCodec() {
  }

  public static void putString(ByteBuffer buffer, String value) {
    if (value == null) {
      buffer.putInt(-1);
      return;
    }
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    buffer.putInt(bytes.length);
    buffer.put(bytes);
  }

  public static String getString(ByteBuffer buffer) throws IOException {
    require(buffer, Integer.BYTES);
    int length = buffer.getInt();
    if (length == -1) {
      return null;
    }
    if (length < 0 || length > buffer.remaining()) {
      throw new IOException("Invalid MAPS string length " + length);
    }
    byte[] bytes = new byte[length];
    buffer.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public static int stringSize(String value) {
    return Integer.BYTES + (value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length);
  }

  public static void require(ByteBuffer buffer, int count) throws IOException {
    if (count < 0 || buffer.remaining() < count) {
      throw new IOException("Malformed MAPS frame");
    }
  }
}
