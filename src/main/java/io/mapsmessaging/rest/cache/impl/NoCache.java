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

public class NoCache <V> implements Cache<CacheKey, V> {

  public NoCache() {
  }

  @Override
  public void put(CacheKey key, V value) {
  }

  @Override
  public V get(CacheKey key) {
    return null;
  }

  @Override
  public void remove(CacheKey key) {
  }

  @Override
  public void clear() {
  }

  @Override
  public long size() {
    return 0;
  }

  @Override
  public CacheInfo getCacheInfo() {
    return new CacheInfo(false, 0, 0, 0, 0, 0);
  }
}
