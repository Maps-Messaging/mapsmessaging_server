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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class GeoSpatialAreaRegistry {

  private final Map<String, GeoSpatialArea> areas;

  private GeoSpatialAreaRegistry(Map<String, GeoSpatialArea> areas) {
    this.areas = Map.copyOf(areas);
    if (this.areas.isEmpty()) {
      throw new IllegalArgumentException("registry must contain at least one area");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public Collection<GeoSpatialArea> areas() {
    return areas.values();
  }

  public Optional<GeoSpatialArea> find(String name) {
    return Optional.ofNullable(areas.get(requireName(name)));
  }

  public GeoSpatialArea require(String name) {
    String checkedName = requireName(name);
    GeoSpatialArea area = areas.get(checkedName);
    if (area == null) {
      throw new IllegalArgumentException("Unknown geospatial area: " + checkedName);
    }
    return area;
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

    private final Map<String, GeoSpatialArea> areas;

    private Builder() {
      areas = new LinkedHashMap<>();
    }

    public Builder add(GeoSpatialArea area) {
      Objects.requireNonNull(area, "area must not be null");
      if (areas.putIfAbsent(area.name(), area) != null) {
        throw new IllegalArgumentException("Duplicate geospatial area: " + area.name());
      }
      return this;
    }

    public Builder addAll(Collection<GeoSpatialArea> areas) {
      Objects.requireNonNull(areas, "areas must not be null");
      areas.forEach(this::add);
      return this;
    }

    public GeoSpatialAreaRegistry build() {
      return new GeoSpatialAreaRegistry(areas);
    }
  }
}
