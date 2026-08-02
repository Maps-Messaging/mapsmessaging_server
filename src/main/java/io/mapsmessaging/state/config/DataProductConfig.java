/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */

package io.mapsmessaging.state.config;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataProductConfig {

  @Schema(description = "Optional stable identifier. A deterministic UUID is generated when omitted.")
  private String identifier;

  @Schema(description = "Human-readable product description.")
  private String description;

  @Schema(description = "External data product URI.", example = "rtsp://drone01/videofeed01")
  private String uri;

  @Schema(description = "Flexible enumeration describing the product type.")
  private Map<String, Object> productType = new LinkedHashMap<>();

  @Schema(description = "Flexible enumeration describing the standard to which the product conforms.")
  private Map<String, Object> conformsTo = new LinkedHashMap<>();
}
