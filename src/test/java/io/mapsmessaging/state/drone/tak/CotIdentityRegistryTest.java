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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.config.cot.CotManagedPlatformConfigDTO;
import io.mapsmessaging.state.config.cot.CotTwinConfigDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

class CotIdentityRegistryTest {

  @Test
  void endpointScopedUid_resolvesDifferentTwins() {
    CotIdentityRegistry registry = registry(
        platform("tak-a", "vehicle-1", "alpha"),
        platform("tak-b", "vehicle-1", "bravo"));

    assertEquals("alpha", registry.resolve("tak-a", "vehicle-1").orElseThrow().twinId());
    assertEquals("bravo", registry.resolve("tak-b", "vehicle-1").orElseThrow().twinId());
  }

  @Test
  void multipleAliases_canResolveOneCanonicalTwin() {
    CotIdentityRegistry registry = registry(
        platform("tak-a", "alpha-primary", "alpha"),
        platform("tak-b", "alpha-forwarded", "alpha"));

    assertEquals("alpha", registry.resolve("tak-a", "alpha-primary").orElseThrow().twinId());
    assertEquals("alpha", registry.resolve("tak-b", "alpha-forwarded").orElseThrow().twinId());
  }

  @Test
  void unknownUid_isNotPromotedToManaged() {
    CotIdentityRegistry registry = registry(platform("tak-a", "known", "alpha"));

    assertTrue(registry.resolve("tak-a", "unknown").isEmpty());
  }

  @Test
  void duplicateAliasForDifferentTwins_isRejected() {
    CotTwinConfigDTO config = new CotTwinConfigDTO();
    config.setManagedPlatforms(List.of(
        platform("tak-a", "vehicle-1", "alpha"),
        platform("tak-a", "vehicle-1", "bravo")));

    assertThrows(IllegalArgumentException.class, () -> new CotIdentityRegistry(config));
  }

  private CotIdentityRegistry registry(CotManagedPlatformConfigDTO... platforms) {
    CotTwinConfigDTO config = new CotTwinConfigDTO();
    config.setManagedPlatforms(List.of(platforms));
    return new CotIdentityRegistry(config);
  }

  private CotManagedPlatformConfigDTO platform(String endpoint, String uid, String twinId) {
    CotManagedPlatformConfigDTO platform = new CotManagedPlatformConfigDTO();
    platform.setEndpoint(endpoint);
    platform.setUid(uid);
    platform.setTwinId(twinId);
    platform.setHaeToMslOffsetMeters(-30.0);
    return platform;
  }
}
