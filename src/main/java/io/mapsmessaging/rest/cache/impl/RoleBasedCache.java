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

package io.mapsmessaging.rest.cache.impl;

import io.mapsmessaging.dto.rest.cache.CacheInfo;
import io.mapsmessaging.rest.cache.Cache;
import io.mapsmessaging.rest.cache.CacheKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class RoleBasedCache<V> implements Cache<CacheKey, V> {
  private final Map<CacheKey, CacheEntry<V>> cache = new ConcurrentHashMap<>();
  private final long expiryDuration;
  private final long cleanupInterval;
  private LongAdder cacheHits;
  private LongAdder cacheMisses;

  public RoleBasedCache(long expiryDuration, long cleanupInterval) {
    this.expiryDuration = expiryDuration;
    this.cleanupInterval = cleanupInterval;
    cacheHits = new LongAdder();
    cacheMisses = new LongAdder();
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
    CacheEntry<V> existingEntry = cache.computeIfAbsent(key, k -> new CacheEntry<>(value, System.currentTimeMillis()));

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
      cacheMisses.increment();
      return null;
    }
    cacheHits.increment();
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

  @Override
  public long size() {
    return cache.size();
  }

  @Override
  public CacheInfo getCacheInfo() {
    return new CacheInfo(true, expiryDuration, cleanupInterval, cache.size(), cacheHits.sum(), cacheMisses.sum());
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

