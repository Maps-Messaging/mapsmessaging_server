/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.ws.rs.core.EntityTag;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class TwinResourceEtag {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  private TwinResourceEtag() {}

  static EntityTag of(Object value) {
    try {
      byte[] canonical = OBJECT_MAPPER.writeValueAsBytes(value);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return new EntityTag(HexFormat.of().formatHex(digest.digest(canonical)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to calculate resource ETag", exception);
    }
  }

  static EntityTag ofBytes(byte[] value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return new EntityTag(HexFormat.of().formatHex(digest.digest(value == null ? new byte[0] : value)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  static EntityTag ofText(String value) {
    return ofBytes((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
  }
}
