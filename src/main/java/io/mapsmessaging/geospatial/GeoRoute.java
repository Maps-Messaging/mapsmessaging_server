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

import java.util.List;
import java.util.Objects;

public record GeoRoute(List<GeoPoint> points, boolean closed) {

  public GeoRoute {
    points = List.copyOf(Objects.requireNonNull(points, "points must not be null"));
    for (GeoPoint point : points) {
      Objects.requireNonNull(point, "route points must not contain null values");
    }
  }

  public GeoRoute(List<GeoPoint> points) {
    this(points, false);
  }
}
