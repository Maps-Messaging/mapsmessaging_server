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

package io.mapsmessaging.state.auditor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.mapsmessaging.state.drone.core.TwinManager;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditorFactoryTest {

  @TempDir
  private Path tempDir;

  @Test
  void build_returnsGenericAuditContextThatTwinManagerCanExpose() throws Exception {
    AuditorFactory factory = new AuditorFactory();

    try (AuditorFactory.AuditorInstance auditorInstance = factory.build(tempDir)) {
      StateAuditContext auditContext = auditorInstance.getAuditContext();
      TwinManager twinManager = new TwinManager(true, 10000L, 5000L, 120000L, auditContext);

      assertNotNull(auditContext);
      assertNotNull(auditContext.auditLogger());
      assertNotNull(auditContext.auditPayloadStore());
      assertSame(auditContext, twinManager.getAuditContext());
    }
  }
}
