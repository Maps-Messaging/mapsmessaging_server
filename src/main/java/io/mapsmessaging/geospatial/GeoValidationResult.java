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
import java.util.Optional;

public record GeoValidationResult(boolean executable, List<GeoViolation> violations) {

  public GeoValidationResult {
    violations = List.copyOf(Objects.requireNonNull(violations, "violations must not be null"));
    if (executable != violations.isEmpty()) {
      throw new IllegalArgumentException("executable must be true only when there are no violations");
    }
  }

  public static GeoValidationResult valid() {
    return new GeoValidationResult(true, List.of());
  }

  public static GeoValidationResult rejected(List<GeoViolation> violations) {
    if (Objects.requireNonNull(violations, "violations must not be null").isEmpty()) {
      throw new IllegalArgumentException("rejected validation must contain at least one violation");
    }
    return new GeoValidationResult(false, violations);
  }

  public Optional<GeoViolation> primaryViolation() {
    return violations.stream().findFirst();
  }
}
