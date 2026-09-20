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

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthManagerPasswordFileTest {

  private static final Set<java.nio.file.attribute.PosixFilePermission> OWNER_READ_WRITE =
      Set.of(OWNER_READ, OWNER_WRITE);

  @TempDir
  Path tempDir;

  @Test
  void createsPasswordFileWithOwnerOnlyPermissions() throws Exception {
    assumePosix();
    Path passwordFile = tempDir.resolve("admin_password");

    AuthManager.prepareInitialPasswordFile(passwordFile);

    assertTrue(Files.exists(passwordFile));
    assertEquals(OWNER_READ_WRITE, Files.getPosixFilePermissions(passwordFile));
  }

  private void assumePosix() {
    assumeTrue(Files.getFileAttributeView(tempDir, PosixFileAttributeView.class) != null);
  }
}
