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

package io.mapsmessaging.license;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LicenseFileStoreTest {

  private final Logger logger = LoggerFactory.getLogger(LicenseFileStoreTest.class);

  @TempDir
  Path tempDir;

  @Test
  void saveLicenseFile_writesEditionSpecificContent() throws Exception {
    byte[] content = "enterprise-license".getBytes(StandardCharsets.UTF_8);
    LicenseFileStore store = new LicenseFileStore(logger);

    assertTrue(store.saveLicenseFile(tempDir.toFile(), "enterprise", content));

    Path savedLicense = tempDir.resolve("license_enterprise.lic");
    assertTrue(Files.isRegularFile(savedLicense));
    assertArrayEquals(content, Files.readAllBytes(savedLicense));
  }

  @Test
  void saveLicenseFile_missingDirectory_returnsFalse() {
    Path missingDirectory = tempDir.resolve("missing");
    LicenseFileStore store = new LicenseFileStore(logger);

    assertFalse(store.saveLicenseFile(missingDirectory.toFile(), "community", new byte[]{1, 2, 3}));
    assertFalse(Files.exists(missingDirectory));
  }

  @Test
  void ensureFallbackLicensePresent_existingLicense_isNotOverwritten() throws Exception {
    Path existingLicense = tempDir.resolve("license_enterprise.lic");
    byte[] originalContent = "existing-license".getBytes(StandardCharsets.UTF_8);
    Files.write(existingLicense, originalContent);
    LicenseFileStore store = new LicenseFileStore(logger);

    store.ensureFallbackLicensePresent(tempDir.toFile());

    assertArrayEquals(originalContent, Files.readAllBytes(existingLicense));
    assertFalse(Files.exists(tempDir.resolve("license_community.lic")));
  }

  @Test
  void ensureFallbackLicensePresent_invalidDirectories_areIgnored() throws Exception {
    Path regularFile = tempDir.resolve("not-a-directory");
    Files.writeString(regularFile, "content");
    LicenseFileStore store = new LicenseFileStore(logger);

    assertDoesNotThrow(() -> store.ensureFallbackLicensePresent(null));
    assertDoesNotThrow(() -> store.ensureFallbackLicensePresent(tempDir.resolve("missing").toFile()));
    assertDoesNotThrow(() -> store.ensureFallbackLicensePresent(regularFile.toFile()));
    assertEquals("content", Files.readString(regularFile));
  }
}
