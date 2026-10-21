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

final class GeoPolygon {

  private final GeoRing shell;
  private final List<GeoRing> holes;

  GeoPolygon(GeoRing shell, List<GeoRing> holes) {
    this.shell = Objects.requireNonNull(shell, "shell must not be null");
    this.holes = List.copyOf(Objects.requireNonNull(holes, "holes must not be null"));
  }

  GeoLocation locate(GeoPoint point) {
    GeoLocation shellLocation = shell.locate(point);
    if (shellLocation != GeoLocation.INSIDE) {
      return shellLocation;
    }

    for (GeoRing hole : holes) {
      GeoLocation holeLocation = hole.locate(point);
      if (holeLocation == GeoLocation.BOUNDARY) {
        return GeoLocation.BOUNDARY;
      }
      if (holeLocation == GeoLocation.INSIDE) {
        return GeoLocation.OUTSIDE;
      }
    }
    return GeoLocation.INSIDE;
  }

  void collectBoundaryIntersections(GeoPoint start, GeoPoint end, List<Double> parameters) {
    shell.collectBoundaryIntersections(start, end, parameters);
    for (GeoRing hole : holes) {
      hole.collectBoundaryIntersections(start, end, parameters);
    }
  }
}
