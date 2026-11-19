package io.mapsmessaging.utilities.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FusionConfigFileMigrationTest {

  @TempDir
  Path directory;

  @Test
  void migratesLegacyManagerFileWithoutChangingContents() throws IOException {
    Path legacy = directory.resolve("AggregatorManager.yaml");
    Path renamed = directory.resolve("FusionManager.yaml");
    String content = "AggregatorManager:\\n  data:\\n    stripeCount: 2\\n".replace("\\n", "\n");
    Files.writeString(legacy, content);

    FusionConfigFileMigration.migrateDirectory(directory);
    FusionConfigFileMigration.migrateDirectory(directory);

    assertFalse(Files.exists(legacy));
    assertEquals(content, Files.readString(renamed));
  }

  @Test
  void preservesExistingFusionConfigurationAndLegacyFile() throws IOException {
    Path legacy = directory.resolve("AggregatorManager.yaml");
    Path renamed = directory.resolve("FusionManager.yaml");
    Files.writeString(legacy, "old");
    Files.writeString(renamed, "new");

    FusionConfigFileMigration.migrateDirectory(directory);

    assertEquals("old", Files.readString(legacy));
    assertEquals("new", Files.readString(renamed));
  }

  @Test
  void migratesLowercaseConfigurationFile() throws IOException {
    Files.writeString(directory.resolve("aggregator.yaml"), "legacy");

    FusionConfigFileMigration.migrateDirectory(directory);

    assertEquals("legacy", Files.readString(directory.resolve("fusion.yaml")));
    assertFalse(Files.exists(directory.resolve("aggregator.yaml")));
  }

  @Test
  void leavesMissingConfigurationFilesAlone() {
    FusionConfigFileMigration.migrateDirectory(directory);
    assertFalse(Files.exists(directory.resolve("FusionManager.yaml")));
    assertFalse(Files.exists(directory.resolve("fusion.yaml")));
  }
}
