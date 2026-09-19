/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.adapter.mti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.drone.tak.MtiLookupResult;
import org.junit.jupiter.api.Test;

class MtiStatusAdapterTest {

  @Test
  void newerStateWinsAndOlderUpdateAndDeleteAreIgnored() {
    MtiStatusAdapter adapter = new MtiStatusAdapter("/mti/status");

    adapter.handle(message("update", "hold", "2026-09-19T10:00:00Z", "2099-01-01T00:00:00Z"));
    adapter.handle(message("update", "go", "2026-09-19T09:00:00Z", "2099-01-01T00:00:00Z"));

    MtiLookupResult result = adapter.lookup("asset-1");
    assertNotNull(result);
    assertEquals(Boolean.FALSE, result.readiness());
    assertEquals(-65536, result.colorArgb());

    adapter.handle(deleteMessage("2026-09-19T09:30:00Z"));
    assertNotNull(adapter.lookup("asset-1"));

    adapter.handle(deleteMessage("2026-09-19T11:00:00Z"));
    assertNull(adapter.lookup("asset-1"));
  }

  @Test
  void newerAlreadyExpiredStatusClearsOlderCachedState() {
    MtiStatusAdapter adapter = new MtiStatusAdapter("/mti/status");

    adapter.handle(message("update", "hold", "2020-01-01T00:00:00Z", "2099-01-01T00:00:00Z"));
    assertNotNull(adapter.lookup("asset-1"));

    adapter.handle(message("update", "go", "2021-01-01T00:00:00Z", "2021-01-02T00:00:00Z"));

    assertNull(adapter.lookup("asset-1"));
    assertEquals(0, adapter.getCacheSize());
  }

  @Test
  void invalidValidityTimestampsAreRejected() {
    MtiStatusAdapter adapter = new MtiStatusAdapter("/mti/status");

    adapter.handle(message("update", "hold", "not-a-time", "2099-01-01T00:00:00Z"));
    adapter.handle(message("update", "hold", "2026-09-19T10:00:00Z", "bad-valid-until"));

    assertNull(adapter.lookup("asset-1"));
    assertEquals(0, adapter.getCacheSize());
  }

  @Test
  void mapsMtiStatesToCotOverrides() {
    MtiStatusAdapter adapter = new MtiStatusAdapter("/mti/status");

    adapter.handle(message("update", "unknown", "2026-09-19T10:00:00Z", "2099-01-01T00:00:00Z"));
    MtiLookupResult unknown = adapter.lookup("asset-1");
    assertNotNull(unknown);
    assertEquals("u", unknown.affiliationOverride());

    adapter.handle(message("update", "mitigate", "2026-09-19T11:00:00Z", "2099-01-01T00:00:00Z"));
    MtiLookupResult mitigate = adapter.lookup("asset-1");
    assertNotNull(mitigate);
    assertEquals(-23296, mitigate.colorArgb());
    assertEquals(Boolean.FALSE, mitigate.readiness());
    assertEquals("ddos3_64x64.png", mitigate.cyberIconFile());

    adapter.handle(message("update", "hold", "2026-09-19T12:00:00Z", "2099-01-01T00:00:00Z"));
    MtiLookupResult hold = adapter.lookup("asset-1");
    assertNotNull(hold);
    assertEquals(-65536, hold.colorArgb());
    assertEquals(Boolean.FALSE, hold.readiness());
    assertEquals("ddos1_64x64.png", hold.cyberIconFile());

    adapter.handle(message("update", "go", "2026-09-19T13:00:00Z", "2099-01-01T00:00:00Z"));
    assertNull(adapter.lookup("asset-1"));
  }

  @Test
  void mtiStatusExpiryHelperTreatsValidUntilAsExclusive() {
    MtiStatus status = new MtiStatus(
        "asset-1",
        "hold",
        "MTI: hold",
        java.time.Instant.parse("2026-09-19T10:00:00Z"),
        java.time.Instant.parse("2026-09-19T11:00:00Z"));

    assertFalse(status.isExpired(java.time.Instant.parse("2026-09-19T10:59:59Z")));
    assertTrue(status.isExpired(java.time.Instant.parse("2026-09-19T11:00:00Z")));
  }

  private String message(String op, String state, String observedAt, String validUntil) {
    return """
        {
          "schema": "mti.asset.health/v1",
          "op": "%s",
          "uid": "asset-1",
          "state": "%s",
          "observed_at": "%s",
          "valid_until": "%s",
          "mti_id": "mti-1",
          "classification": "TEST"
        }
        """.formatted(op, state, observedAt, validUntil);
  }

  private String deleteMessage(String observedAt) {
    return """
        {
          "schema": "mti.asset.health/v1",
          "op": "delete",
          "uid": "asset-1",
          "observed_at": "%s"
        }
        """.formatted(observedAt);
  }
}
