/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.rest.cache.impl;

import io.mapsmessaging.dto.rest.cache.CacheInfo;
import io.mapsmessaging.rest.cache.CacheKey;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RoleBasedCacheTest {

  @Test
  void putAndGetShouldReturnValueAndCountHit() {
    RoleBasedCache<String> cache = new RoleBasedCache<>(10_000, 60_000);

    CacheKey key = Mockito.mock(CacheKey.class);
    Mockito.when(key.getEndpoint()).thenReturn("users/me");

    cache.put(key, "value1");

    String value = cache.get(key);
    assertEquals("value1", value);

    CacheInfo info = cache.getCacheInfo();
    assertEquals(1L, info.getCacheHits());
    assertEquals(0L, info.getCacheMisses());
    assertEquals(1L, info.getCacheSize());
    assertEquals(10_000L, info.getLifeTime());
    assertEquals(60_000L, info.getScanTime());
    assertTrue(info.isEnabled());
    cache.close();
  }

  @Test
  void getMissingKeyShouldReturnNullAndCountMiss() {
    RoleBasedCache<String> cache = new RoleBasedCache<>(10_000, 60_000);

    CacheKey key = Mockito.mock(CacheKey.class);
    Mockito.when(key.getEndpoint()).thenReturn("users/me");

    String value = cache.get(key);
    assertNull(value);

    CacheInfo info = cache.getCacheInfo();
    assertEquals(0L, info.getCacheHits());
    assertEquals(1L, info.getCacheMisses());
    assertEquals(0L, info.getCacheSize());
    cache.close();
  }

  @Test
  void expiredEntryShouldReturnNullRemoveEntryAndCountMiss() throws Exception {
    RoleBasedCache<String> cache = new RoleBasedCache<>(50, 60_000);

    CacheKey key = Mockito.mock(CacheKey.class);
    Mockito.when(key.getEndpoint()).thenReturn("users/me");

    cache.put(key, "value1");

    Thread.sleep(80);

    String value = cache.get(key);
    assertNull(value);
    assertEquals(0L, cache.size());

    CacheInfo info = cache.getCacheInfo();
    assertEquals(0L, info.getCacheHits());
    assertEquals(1L, info.getCacheMisses());
    assertEquals(0L, info.getCacheSize());
    cache.close();
  }

  @Test
  void putShouldNotOverwriteExistingNonExpiredEntry() {
    RoleBasedCache<String> cache = new RoleBasedCache<>(10_000, 60_000);

    CacheKey key = Mockito.mock(CacheKey.class);
    Mockito.when(key.getEndpoint()).thenReturn("users/me");

    cache.put(key, "value1");
    cache.put(key, "value2");

    String value = cache.get(key);
    assertEquals("value1", value, "computeIfAbsent means put() does not overwrite a non-expired entry");
    cache.close();
  }

  @Test
  void removeShouldDeleteEntry() {
    RoleBasedCache<String> cache = new RoleBasedCache<>(10_000, 60_000);

    CacheKey key = Mockito.mock(CacheKey.class);
    Mockito.when(key.getEndpoint()).thenReturn("users/me");

    cache.put(key, "value1");
    assertEquals(1L, cache.size());

    cache.remove(key);
    assertEquals(0L, cache.size());
    assertNull(cache.get(key));

    CacheInfo info = cache.getCacheInfo();
    assertEquals(0L, info.getCacheHits());
    assertEquals(1L, info.getCacheMisses());
    cache.close();
  }

  @Test
  void clearShouldRemoveAllEntries() {
    RoleBasedCache<String> cache = new RoleBasedCache<>(10_000, 60_000);

    CacheKey key1 = Mockito.mock(CacheKey.class);
    Mockito.when(key1.getEndpoint()).thenReturn("admin/users/1");

    CacheKey key2 = Mockito.mock(CacheKey.class);
    Mockito.when(key2.getEndpoint()).thenReturn("public/info");

    cache.put(key1, "a");
    cache.put(key2, "b");
    assertEquals(2L, cache.size());

    cache.clear();
    assertEquals(0L, cache.size());
    assertNull(cache.get(key1));
    assertNull(cache.get(key2));
    cache.close();
  }

  @Test
  void removePathShouldRemoveMatchingEndpointPrefixWithOrWithoutLeadingSlash() {
    RoleBasedCache<String> cache = new RoleBasedCache<>(10_000, 60_000);

    CacheKey k1 = Mockito.mock(CacheKey.class);
    Mockito.when(k1.getEndpoint()).thenReturn("admin/users/1");

    CacheKey k2 = Mockito.mock(CacheKey.class);
    Mockito.when(k2.getEndpoint()).thenReturn("admin/users/2");

    CacheKey k3 = Mockito.mock(CacheKey.class);
    Mockito.when(k3.getEndpoint()).thenReturn("public/info");

    cache.put(k1, "a");
    cache.put(k2, "b");
    cache.put(k3, "c");

    cache.removePath("/admin/users");

    assertNull(cache.get(k1));
    assertNull(cache.get(k2));
    assertEquals("c", cache.get(k3));
    assertEquals(1L, cache.size());
    cache.close();
  }

  @Test
  void scheduledCleanupShouldEvictExpiredEntriesEventually() throws Exception {
    RoleBasedCache<String> cache = new RoleBasedCache<>(60, 20);

    CacheKey key = Mockito.mock(CacheKey.class);
    Mockito.when(key.getEndpoint()).thenReturn("users/me");

    cache.put(key, "value1");
    assertEquals(1L, cache.size());

    long deadline = System.currentTimeMillis() + Duration.ofSeconds(2).toMillis();
    while (System.currentTimeMillis() < deadline && cache.size() > 0) {
      Thread.sleep(10);
    }

    assertEquals(0L, cache.size(), "scheduled cleanup should remove expired entries");
    assertNull(cache.get(key));
    cache.close();
  }
}