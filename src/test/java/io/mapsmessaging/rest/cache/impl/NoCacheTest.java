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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoCacheTest {

  @Test
  void cacheOperationsNeverRetainValues() {
    NoCache<String> cache = new NoCache<>();
    CacheKey key = new CacheKey("/users/me", "admin");

    cache.put(key, "value");
    assertNull(cache.get(key));
    assertEquals(0L, cache.size());

    cache.remove(key);
    cache.removePath("/users");
    cache.clear();

    assertNull(cache.get(key));
    assertEquals(0L, cache.size());
  }

  @Test
  void cacheInfoReportsDisabledZeroSizedCache() {
    NoCache<String> cache = new NoCache<>();

    CacheInfo info = cache.getCacheInfo();

    assertFalse(info.isEnabled());
    assertEquals(0L, info.getLifeTime());
    assertEquals(0L, info.getScanTime());
    assertEquals(0L, info.getCacheSize());
    assertEquals(0L, info.getCacheHits());
    assertEquals(0L, info.getCacheMisses());
  }
}
