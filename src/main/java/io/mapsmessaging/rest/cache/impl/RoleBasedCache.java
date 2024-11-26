/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.rest.cache.impl;

import io.mapsmessaging.rest.cache.Cache;
import io.mapsmessaging.rest.cache.CacheKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoleBasedCache<V> implements Cache<CacheKey, V> {
  private final Map<CacheKey, CacheEntry<V>> cache = new ConcurrentHashMap<>();
  private final long expiryDuration;

  public RoleBasedCache(long expiryDuration, long cleanupInterval) {
    this.expiryDuration = expiryDuration;
    // Schedule periodic cleanup if needed
    new Thread(() -> {
      while (true) {
        try {
          Thread.sleep(cleanupInterval);
          cleanup();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }).start();
  }

  @Override
  public void put(CacheKey key, V value) {
    CacheEntry<V> existingEntry = cache.computeIfAbsent(key, k -> {
      return new CacheEntry<>(value, System.currentTimeMillis());
    });

    if (isExpired(System.currentTimeMillis(), existingEntry)) {
      remove(key); // Remove the expired entry
      put(key, value); // Recursively add the new entry
    }
  }


  @Override
  public V get(CacheKey key) {
    CacheEntry<V> entry = cache.get(key);
    if (entry == null || isExpired(System.currentTimeMillis(), entry)) {
      cache.remove(key);
      return null;
    }
    return entry.value;
  }

  @Override
  public void remove(CacheKey key) {
    cache.remove(key);
  }

  @Override
  public void clear() {
    cache.clear();
  }

  private void cleanup() {
    long now = System.currentTimeMillis();
    for (Map.Entry<CacheKey, CacheEntry<V>> entry : cache.entrySet()) {
      if (isExpired(now, entry.getValue())) {
        cache.remove(entry.getKey());
      }
    }
  }

  private boolean isExpired(long now, CacheEntry<V> entry) {
    return now - entry.timestamp > expiryDuration;
  }

  private static class CacheEntry<V> {
    final V value;
    final long timestamp;

    CacheEntry(V value, long timestamp) {
      this.value = value;
      this.timestamp = timestamp;
    }
  }
}

