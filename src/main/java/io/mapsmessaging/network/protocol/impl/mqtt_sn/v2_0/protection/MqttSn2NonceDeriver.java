/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** CSD01 3.17.3: SHA-256 of the serialized envelope prefix, left-truncated. */
public final class MqttSn2NonceDeriver {
  private MqttSn2NonceDeriver() {}

  public static byte[] derive(byte[] authenticatedPrefix, int size) {
    if (size != 12 && size != 13) throw new IllegalArgumentException("Invalid AEAD nonce size");
    try {
      return Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(authenticatedPrefix), size);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JDK does not support SHA-256", e);
    }
  }
}
