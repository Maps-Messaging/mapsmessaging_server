package io.mapsmessaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InstanceConfigCoverageSweepTest {
  @TempDir
  Path tempDir;

  @Test
  void savedInstanceStateCanBeLoadedBack() {
    String prefix = tempDir.toString() + File.separator;
    UUID id = UUID.randomUUID();

    InstanceConfig source = new InstanceConfig(prefix);
    source.setServerName("node-a");
    source.setUuid(id);
    source.setCreationDate("2026-09-20T20:00:00");
    source.setSecureTokenSecret("secret-value");
    source.saveState();

    InstanceConfig restored = new InstanceConfig(prefix);
    restored.loadState();

    assertEquals("node-a", restored.getServerName());
    assertEquals(id, restored.getUuid());
    assertEquals("2026-09-20T20:00:00", restored.getCreationDate());
    assertEquals("secret-value", restored.getSecureTokenSecret());
  }
}
