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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

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

  @Test
  void doesNotRecreateDeletedBootstrapPasswordFile() throws Exception {
    Path passwordFile = tempDir.resolve("admin_password");

    AuthManager.secureExistingInitialPasswordFile(passwordFile);

    assertTrue(Files.notExists(passwordFile));
  }

  @Test
  void unsupported_permission_restriction_does_not_abort_startup() {
    Path passwordFile = mock(Path.class);
    File file = mock(File.class);
    when(passwordFile.toFile()).thenReturn(file);
    when(file.setReadable(false, false)).thenReturn(false);
    when(file.setWritable(false, false)).thenReturn(false);

    try (MockedStatic<Files> files = mockStatic(Files.class)) {
      files.when(() -> Files.getFileAttributeView(passwordFile, PosixFileAttributeView.class)).thenReturn(null);

      assertDoesNotThrow(() -> AuthManager.restrictInitialPasswordFile(passwordFile));
    }
  }

  @Test
  void password_file_remains_writable_after_creation_and_restart() throws Exception {
    Path passwordFile = tempDir.resolve("admin_password");
    AuthManager.prepareInitialPasswordFile(passwordFile);
    Files.writeString(passwordFile, "admin=test-password\n", StandardOpenOption.APPEND);

    AuthManager.secureExistingInitialPasswordFile(passwordFile);

    assertEquals("admin=test-password\n", Files.readString(passwordFile));
    Files.writeString(passwordFile, "user=another-password\n", StandardOpenOption.APPEND);
    assertEquals("admin=test-password\nuser=another-password\n", Files.readString(passwordFile));
  }

  private void assumePosix() {
    assumeTrue(Files.getFileAttributeView(tempDir, PosixFileAttributeView.class) != null);
  }
}
