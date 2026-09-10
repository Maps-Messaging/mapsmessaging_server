/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.cot.CotManagedPlatformConfigDTO;
import io.mapsmessaging.state.config.cot.CotTwinConfigDTO;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Resolves configured endpoint-scoped UIDs. Configuration is the durable identity source. */
public class CotIdentityRegistry {

  private final Map<String, CotIdentityBinding> byAlias = new LinkedHashMap<>();
  private final Map<String, CotIdentityBinding> byTwinId = new LinkedHashMap<>();

  public CotIdentityRegistry(CotTwinConfigDTO config) {
    if (config == null || config.getManagedPlatforms() == null) {
      return;
    }
    for (CotManagedPlatformConfigDTO platform : config.getManagedPlatforms()) {
      validate(platform);
      CotIdentityBinding binding = CotIdentityBinding.from(platform);
      String alias = alias(binding.endpoint(), binding.uid());
      CotIdentityBinding duplicate = byAlias.putIfAbsent(alias, binding);
      if (duplicate != null && !duplicate.twinId().equals(binding.twinId())) {
        throw new IllegalArgumentException("CoT alias " + alias + " maps to multiple twins");
      }
      byTwinId.putIfAbsent(binding.twinId(), binding);
    }
  }

  public Optional<CotIdentityBinding> resolve(String endpoint, String uid) {
    CotIdentityBinding exact = byAlias.get(alias(endpoint, uid));
    if (exact != null) {
      return Optional.of(exact);
    }
    return Optional.ofNullable(byAlias.get(alias("*", uid)));
  }

  public Optional<CotIdentityBinding> findByTwinId(String twinId) {
    return Optional.ofNullable(byTwinId.get(twinId));
  }

  private void validate(CotManagedPlatformConfigDTO platform) {
    if (platform == null
        || blank(platform.getEndpoint())
        || blank(platform.getUid())
        || blank(platform.getTwinId())) {
      throw new IllegalArgumentException("CoT managed platform requires endpoint, uid and twinId");
    }
    if (platform.isTaskable() && blank(platform.getTaskingProfile())) {
      throw new IllegalArgumentException("Taskable CoT platform requires taskingProfile");
    }
  }

  private String alias(String endpoint, String uid) {
    return "cot:" + normalise(endpoint) + ":" + normalise(uid);
  }

  private String normalise(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
