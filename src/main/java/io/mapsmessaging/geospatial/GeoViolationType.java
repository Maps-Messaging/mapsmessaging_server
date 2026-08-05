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

public enum GeoViolationType {
  EMPTY_ROUTE,
  POINT_OUTSIDE_INSIDE_BOUNDARIES,
  POINT_IN_DO_NOT_ENTER_BOUNDARY,
  SEGMENT_OUTSIDE_INSIDE_BOUNDARIES,
  SEGMENT_INTERSECTS_DO_NOT_ENTER_BOUNDARY
}
