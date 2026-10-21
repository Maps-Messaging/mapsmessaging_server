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

/**
 * Loads polygonal GeoJSON boundaries and validates waypoint routes against named operating areas.
 * Multiple {@code INSIDE} boundaries form a permitted union, while every {@code DO_NOT_ENTER}
 * boundary is an exclusion. Coordinates use GeoJSON longitude/latitude order on input and decimal
 * latitude/longitude values in the Java API.
 *
 * <p>The validator treats route segments as straight lines in CRS84 coordinate space. It is intended
 * for local operating regions and does not handle routes crossing the antimeridian.
 */
package io.mapsmessaging.geospatial;
