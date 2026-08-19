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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.engine.session.persistence.SessionDetails;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistentSessionManagerTest {

  @TempDir
  Path temporaryDirectory;

  @Test
  void resetSessionDetails_replacesPersistedIdentityAndDeletesPreviousState() throws IOException {
    PersistentSessionManager manager = new PersistentSessionManager(temporaryDirectory + File.separator);
    SessionContext originalContext = new SessionContext("client", new TestClientConnection());
    originalContext.setInternalSessionId(1L);
    originalContext.setExpiry(60L);
    originalContext.setPersistentSession(true);
    SessionDetails originalDetails = manager.getSessionDetails(originalContext);
    Path originalState = Path.of(manager.getDataPath(), originalDetails.getUniqueId() + ".bin");
    try (OutputStream outputStream = Files.newOutputStream(originalState)) {
      originalDetails.save(outputStream);
    }

    SessionContext replacementContext = new SessionContext("client", new TestClientConnection());
    replacementContext.setInternalSessionId(2L);
    replacementContext.setExpiry(0L);
    SessionDetails replacementDetails = manager.resetSessionDetails(replacementContext);

    assertFalse(Files.exists(originalState));
    assertNotEquals(originalDetails.getUniqueId(), replacementDetails.getUniqueId());
    assertEquals(2L, replacementDetails.getInternalUnqueId());
    assertNull(manager.getSessionDetails("client"));
  }

  @Test
  void removeSessionDetails_deletesOnlyTheMatchingSessionState() throws IOException {
    PersistentSessionManager manager = new PersistentSessionManager(temporaryDirectory + File.separator);
    SessionContext originalContext = new SessionContext("client", new TestClientConnection());
    originalContext.setPersistentSession(true);
    SessionDetails originalDetails = manager.getSessionDetails(originalContext);
    SessionContext replacementContext = new SessionContext("client", new TestClientConnection());
    replacementContext.setExpiry(60L);
    replacementContext.setPersistentSession(true);
    SessionDetails replacementDetails = manager.resetSessionDetails(replacementContext);
    Path replacementState = Path.of(manager.getDataPath(), replacementDetails.getUniqueId() + ".bin");
    try (OutputStream outputStream = Files.newOutputStream(replacementState)) {
      replacementDetails.save(outputStream);
    }

    manager.removeSessionDetails("client", originalDetails.getUniqueId());

    assertSame(replacementDetails, manager.getSessionDetails("client"));
    assertTrue(Files.exists(replacementState));

    manager.removeSessionDetails("client", replacementDetails.getUniqueId());

    assertNull(manager.getSessionDetails("client"));
    assertFalse(Files.exists(replacementState));
  }

  private static final class TestClientConnection implements ClientConnection {

    @Override
    public long getTimeOut() {
      return 0;
    }

    @Override
    public String getName() {
      return "test";
    }

    @Override
    public String getVersion() {
      return "test";
    }

    @Override
    public void sendKeepAlive() {
    }

    @Override
    public Principal getPrincipal() {
      return null;
    }

    @Override
    public String getAuthenticationConfig() {
      return "";
    }

    @Override
    public String getUniqueName() {
      return "test";
    }

    @Override
    public String getProtocolName() {
      return "test";
    }

    @Override
    public String getRemoteIp() {
      return "127.0.0.1";
    }
  }
}
