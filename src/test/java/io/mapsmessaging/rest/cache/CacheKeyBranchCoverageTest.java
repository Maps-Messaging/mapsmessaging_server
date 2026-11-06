package io.mapsmessaging.rest.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheKeyBranchCoverageTest {

  @Test
  void constructorNormalizesEndpointAndRoleForStableKeys() {
    CacheKey key = new CacheKey(" /API/Status ", " ADMIN ");

    assertEquals("/api/status", key.getEndpoint());
    assertEquals("admin", key.getRole());
  }

  @Test
  void equalityCoversIdentityNullOtherTypeAndDifferentFields() {
    CacheKey key = new CacheKey("/api", "admin");

    assertEquals(key, key);
    assertNotEquals(key, null);
    assertNotEquals(key, "/api");
    assertNotEquals(key, new CacheKey("/other", "admin"));
    assertNotEquals(key, new CacheKey("/api", "user"));
    assertEquals(key, new CacheKey("/API", "ADMIN"));
    assertEquals(key.hashCode(), new CacheKey("/api", "admin").hashCode());
  }

  @Test
  void stringRepresentationIncludesNormalizedValues() {
    String text = new CacheKey("/API", "ROLE").toString();

    assertTrue(text.contains("/api"));
    assertTrue(text.contains("role"));
  }
}