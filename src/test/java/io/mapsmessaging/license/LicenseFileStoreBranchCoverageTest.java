package io.mapsmessaging.license;

import io.mapsmessaging.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LicenseFileStoreBranchCoverageTest {

  @TempDir
  Path tempDir;

  @Test
  void saveLicenseFileWritesEditionSpecificFile() throws Exception {
    LicenseFileStore store = new LicenseFileStore(mock(Logger.class));

    assertTrue(store.saveLicenseFile(tempDir.toFile(), "enterprise", new byte[]{1, 2, 3}));

    Path saved = tempDir.resolve("license_enterprise.lic");
    assertTrue(Files.exists(saved));
    assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(saved));
  }

  @Test
  void saveFailureReturnsFalseWhenParentIsNotDirectory() throws Exception {
    Path notDirectory = tempDir.resolve("file");
    Files.writeString(notDirectory, "x");
    LicenseFileStore store = new LicenseFileStore(mock(Logger.class));

    assertFalse(store.saveLicenseFile(notDirectory.toFile(), "community", new byte[]{1}));
  }

  @Test
  void fallbackCheckIgnoresInvalidDirectoriesAndExistingLicenses() throws Exception {
    LicenseFileStore store = new LicenseFileStore(mock(Logger.class));

    assertDoesNotThrow(() -> store.ensureFallbackLicensePresent(null));

    File missing = tempDir.resolve("missing").toFile();
    assertDoesNotThrow(() -> store.ensureFallbackLicensePresent(missing));

    Path existing = tempDir.resolve("license_custom.lic");
    Files.writeString(existing, "existing");
    store.ensureFallbackLicensePresent(tempDir.toFile());

    assertEquals("existing", Files.readString(existing));
    assertEquals(1, Files.list(tempDir)
        .filter(path -> path.getFileName().toString().endsWith(".lic"))
        .count());
  }
}