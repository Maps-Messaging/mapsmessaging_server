package io.mapsmessaging.utilities.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Migrates legacy Event Fusion configuration filenames before YAML loading.
 * Never overwrites a destination or changes the configuration document.
 */
public final class FusionConfigFileMigration {
  private static final Logger LOGGER = Logger.getLogger(FusionConfigFileMigration.class.getName());

  private FusionConfigFileMigration() {
  }

  public static void migrateDirectory(Path directory) {
    migrate(directory.resolve("AggregatorManager.yaml"), directory.resolve("FusionManager.yaml"));
    migrate(directory.resolve("aggregator.yaml"), directory.resolve("fusion.yaml"));
  }

  private static void migrate(Path legacy, Path destination) {
    if (!Files.isRegularFile(legacy) || Files.exists(destination)) {
      return;
    }
    try {
      Files.move(legacy, destination);
    } catch (IOException exception) {
      LOGGER.log(Level.WARNING, "Unable to migrate " + legacy + " to " + destination + "; legacy configuration remains available", exception);
    }
  }
}
