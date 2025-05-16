/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.rest.cache;

import io.mapsmessaging.rest.cache.impl.RoleBasedCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CacheTest {


  @Test
  void basicTest() {
    // Create a RoleBasedCache with 5-second expiry and 1-second cleanup interval
    Cache<CacheKey, String> cache = new RoleBasedCache<>(5000, 1000);

    // Define composite keys
    CacheKey keyAdmin = new CacheKey("/api/resource", "admin");
    CacheKey keyUser = new CacheKey("/api/resource", "user");

    // Add entries to the cache
    cache.put(keyAdmin, "Admin Response");
    cache.put(keyUser, "User Response");

    Assertions.assertEquals("Admin Response", cache.get(keyAdmin));
    Assertions.assertEquals("User Response", cache.get(keyUser));

    // Wait for entries to expire
    try { Thread.sleep(6000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

    // Verify expired entries
    Assertions.assertNull(cache.get(keyAdmin)," Should be null");
    Assertions.assertNull( cache.get(keyUser), " Should be null");
  }


}
