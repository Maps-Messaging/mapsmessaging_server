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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.dto.rest.config.tenant.TenantConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionTenantConfigTest {

  @Test
  void rootTenant_doesNotPrefixDestinationNames() {
    SessionTenantConfig config = new SessionTenantConfig("/", null);

    assertEquals("", config.getTenantPath());
    assertEquals("/devices/one", config.calculateDestinationName("/devices/one"));
    assertEquals("/devices/one", config.calculateOriginalName("/devices/one"));
  }

  @Test
  void tenantPath_roundTripsLeadingSlashDestination() {
    SessionTenantConfig config = new SessionTenantConfig("tenant/", null);

    String fullyQualifiedName = config.calculateDestinationName("/devices/one");

    assertEquals("tenant/_devices/one", fullyQualifiedName);
    assertEquals("/devices/one", config.calculateOriginalName(fullyQualifiedName));
  }

  @Test
  void globalAndSystemDestinations_bypassTenantMapping() {
    TenantConfigDTO globalConfig = new TenantConfigDTO();
    globalConfig.setNamespaceRoot("shared/");
    SessionTenantConfig config = new SessionTenantConfig("tenant/", List.of(globalConfig));

    assertEquals("shared/events", config.calculateDestinationName("shared/events"));
    assertEquals("shared/events", config.calculateOriginalName("shared/events"));
    assertEquals("$SYS/status", config.calculateDestinationName("$SYS/status"));
    assertEquals("$SYS/status", config.calculateOriginalName("$SYS/status"));
  }

  @Test
  void nullAndSingleCharacterGlobalRoots_areIgnored() {
    TenantConfigDTO nullRoot = new TenantConfigDTO();
    TenantConfigDTO singleCharacterRoot = new TenantConfigDTO();
    singleCharacterRoot.setNamespaceRoot("/");
    SessionTenantConfig config = new SessionTenantConfig("tenant/", List.of(nullRoot, singleCharacterRoot));

    assertEquals("tenant/_events", config.calculateDestinationName("/events"));
  }
}
