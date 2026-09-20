package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoValidationResultCoverageSweepTest {
  @Test
  void validAndRejectedFactoriesEnforceResultInvariant() {
    GeoValidationResult valid = GeoValidationResult.valid();
    assertTrue(valid.executable());
    assertTrue(valid.violations().isEmpty());
    assertTrue(valid.primaryViolation().isEmpty());

    GeoViolation violation = new GeoViolation(
        GeoViolationType.EMPTY_ROUTE,
        "area",
        null,
        null,
        null,
        null,
        false,
        "empty");
    GeoValidationResult rejected = GeoValidationResult.rejected(List.of(violation));
    assertFalse(rejected.executable());
    assertEquals(violation, rejected.primaryViolation().orElseThrow());

    assertThrows(IllegalArgumentException.class, () -> GeoValidationResult.rejected(List.of()));
    assertThrows(IllegalArgumentException.class, () -> new GeoValidationResult(true, List.of(violation)));
  }
}
