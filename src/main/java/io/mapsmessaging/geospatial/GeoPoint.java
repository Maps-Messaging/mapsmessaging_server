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

public record GeoPoint(double latitude, double longitude) {

  public GeoPoint {
    if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
      throw new IllegalArgumentException("latitude must be finite and between -90 and 90 degrees");
    }
    if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
      throw new IllegalArgumentException("longitude must be finite and between -180 and 180 degrees");
    }
  }
}
