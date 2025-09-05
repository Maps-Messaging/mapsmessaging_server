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

import java.util.Objects;

public class CacheKey {
  private final String endpoint;
  private final String role;

  public CacheKey(String endpoint, String role) {
    this.endpoint = endpoint.trim().toLowerCase();
    this.role = role.trim().toLowerCase();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CacheKey cacheKey = (CacheKey) o;
    return Objects.equals(endpoint, cacheKey.endpoint) &&
        Objects.equals(role, cacheKey.role);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpoint, role);
  }

  @Override
  public String toString() {
    return "CacheKey{" +
        "endpoint='" + endpoint + '\'' +
        ", key='" + role + '\'' +
        '}';
  }
}

