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

import java.io.IOException;

public class GeoJsonBoundaryException extends IOException {

  private static final long serialVersionUID = 1L;

  public GeoJsonBoundaryException(String message) {
    super(message);
  }

  public GeoJsonBoundaryException(String message, Throwable cause) {
    super(message, cause);
  }
}
