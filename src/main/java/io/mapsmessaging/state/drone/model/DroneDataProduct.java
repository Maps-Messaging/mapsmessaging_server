/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */

package io.mapsmessaging.state.drone.model;

import java.net.URI;
import java.util.Map;

public record DroneDataProduct(
    String identifier,
    String description,
    URI uri,
    Map<String, Object> productType,
    Map<String, Object> conformsTo) {

  public DroneDataProduct {
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("data product identifier must not be blank");
    }
    if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
      throw new IllegalArgumentException("data product URI must contain a scheme and host");
    }
    String scheme = uri.getScheme().toLowerCase(java.util.Locale.ROOT);
    if (!scheme.equals("rtsp") && !scheme.equals("rtsps")) {
      throw new IllegalArgumentException("data product URI scheme must be rtsp or rtsps");
    }
    productType = productType == null ? Map.of() : Map.copyOf(productType);
    conformsTo = conformsTo == null ? Map.of() : Map.copyOf(conformsTo);
  }
}
