/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 */

package io.mapsmessaging.geospatial;

import java.util.Objects;

public record GeoViolation(
    GeoViolationType type,
    String areaName,
    String boundaryName,
    Integer pointIndex,
    Integer segmentStartIndex,
    Integer segmentEndIndex,
    boolean closingSegment,
    String reason) {

  public GeoViolation {
    type = Objects.requireNonNull(type, "type must not be null");
    areaName = Objects.requireNonNull(areaName, "areaName must not be null");
    reason = Objects.requireNonNull(reason, "reason must not be null");
  }
}
