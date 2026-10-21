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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class GeoSpatialArea {

  private final String name;
  private final List<GeoSpatialBoundary> boundaries;
  private final List<GeoSpatialBoundary> insideBoundaries;
  private final List<GeoSpatialBoundary> doNotEnterBoundaries;

  private GeoSpatialArea(String name, List<GeoSpatialBoundary> boundaries) {
    this.name = requireName(name);
    this.boundaries = List.copyOf(boundaries);
    if (this.boundaries.isEmpty()) {
      throw new IllegalArgumentException("area must contain at least one boundary");
    }

    List<GeoSpatialBoundary> inside = new ArrayList<>();
    List<GeoSpatialBoundary> excluded = new ArrayList<>();
    Set<String> boundaryNames = new HashSet<>();
    for (GeoSpatialBoundary boundary : this.boundaries) {
      if (!boundaryNames.add(boundary.name())) {
        throw new IllegalArgumentException("duplicate boundary name: " + boundary.name());
      }
      if (boundary.type() == GeoSpatialBoundaryType.INSIDE) {
        inside.add(boundary);
      } else {
        excluded.add(boundary);
      }
    }
    insideBoundaries = List.copyOf(inside);
    doNotEnterBoundaries = List.copyOf(excluded);
  }

  public static Builder builder(String name) {
    return new Builder(name);
  }

  public String name() {
    return name;
  }

  public List<GeoSpatialBoundary> boundaries() {
    return boundaries;
  }

  public Optional<GeoSpatialBoundary> boundary(String boundaryName) {
    Objects.requireNonNull(boundaryName, "boundaryName must not be null");
    return boundaries.stream().filter(boundary -> boundary.name().equals(boundaryName)).findFirst();
  }

  public boolean allows(GeoPoint point) {
    Objects.requireNonNull(point, "point must not be null");
    if (!coversInsideBoundaries(point)) {
      return false;
    }
    return doNotEnterBoundaries.stream().noneMatch(boundary -> boundary.covers(point));
  }

  public GeoValidationResult validate(GeoRoute route) {
    return GeoRouteValidator.validate(this, route);
  }

  List<GeoSpatialBoundary> insideBoundaries() {
    return insideBoundaries;
  }

  List<GeoSpatialBoundary> doNotEnterBoundaries() {
    return doNotEnterBoundaries;
  }

  boolean coversInsideBoundaries(GeoPoint point) {
    return insideBoundaries.isEmpty()
        || insideBoundaries.stream().anyMatch(boundary -> boundary.covers(point));
  }

  private static String requireName(String value) {
    Objects.requireNonNull(value, "name must not be null");
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return trimmed;
  }

  public static final class Builder {

    private final String name;
    private final List<GeoSpatialBoundary> boundaries;

    private Builder(String name) {
      this.name = requireName(name);
      boundaries = new ArrayList<>();
    }

    public Builder add(GeoSpatialBoundary boundary) {
      boundaries.add(Objects.requireNonNull(boundary, "boundary must not be null"));
      return this;
    }

    public Builder addAll(Collection<GeoSpatialBoundary> boundaries) {
      Objects.requireNonNull(boundaries, "boundaries must not be null");
      boundaries.forEach(this::add);
      return this;
    }

    public GeoSpatialArea build() {
      return new GeoSpatialArea(name, boundaries);
    }
  }
}
