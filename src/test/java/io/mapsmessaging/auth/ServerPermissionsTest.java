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

package io.mapsmessaging.auth;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ServerPermissionsTest {

  @Test
  void permission_masks_areUniqueSingleBits() {
    Set<Long> masks = Arrays.stream(ServerPermissions.values())
        .map(ServerPermissions::getMask)
        .collect(Collectors.toSet());

    assertEquals(ServerPermissions.values().length, masks.size());
    for (ServerPermissions permission : ServerPermissions.values()) {
      assertEquals(1, Long.bitCount(permission.getMask()), permission.getName());
    }
  }

  @Test
  void permissions_areClassifiedByMaskRange() {
    for (ServerPermissions permission : ServerPermissions.values()) {
      boolean expectedServerPermission = permission.getMask() < (1L << 32);
      assertEquals(expectedServerPermission, permission.isServer(), permission.getName());
    }
  }

  @Test
  void generatedOpenFgaModel_containsAllowDenyAndEffectiveRelationsForEveryPermission() {
    String model = ServerPermissions.generateOpenFgaModel();

    assertTrue(model.startsWith("model\n  schema 1.1\n"));
    assertTrue(model.contains("define member: [user]"));
    for (ServerPermissions permission : ServerPermissions.values()) {
      String name = permission.getName();
      assertTrue(model.contains("define allow_" + name + ": [user, group#member]"), name);
      assertTrue(model.contains("define deny_" + name + ": [user, group#member]"), name);
      assertTrue(model.contains("define " + name + ": allow_" + name + " but not deny_" + name), name);
    }
  }
}
