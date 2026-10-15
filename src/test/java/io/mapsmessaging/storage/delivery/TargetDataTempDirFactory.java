/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.delivery;

import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDirFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Isolated test directories under the server harness data root; JUnit cleans them. */
public final class TargetDataTempDirFactory implements TempDirFactory {
  @Override
  public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
      throws IOException {
    Path root = Path.of("target", "data", "packet-id-store-tests").toAbsolutePath();
    Files.createDirectories(root);
    return Files.createTempDirectory(root, "case-");
  }
}
