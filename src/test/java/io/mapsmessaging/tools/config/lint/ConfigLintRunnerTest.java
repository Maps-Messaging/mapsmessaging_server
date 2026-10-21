package io.mapsmessaging.tools.config.lint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLintRunnerTest {

  @TempDir
  Path tempDir;

  @AfterEach
  void clearProperties() {
    System.clearProperty("maps.configlint.strict");
    System.clearProperty("maps.configlint.showInfo");
  }

  @Test
  void runDiscoversConfigsAndWritesBothReports() throws Exception {
    Path json = tempDir.resolve("nested/config-lint.json");
    Path text = tempDir.resolve("nested/config-lint.txt");

    ConfigLintReport report = new ConfigLintRunner(json, text).run();

    assertNotNull(report);
    assertNotNull(report.getSummary());
    assertTrue(Files.isRegularFile(json));
    assertTrue(Files.isRegularFile(text));
    assertFalse(Files.readString(json).isBlank());
    assertFalse(Files.readString(text).isBlank());
  }

  @Test
  void strictAndInfoFlagsAreAcceptedWithoutChangingReportContract() throws Exception {
    System.setProperty("maps.configlint.strict", "true");
    System.setProperty("maps.configlint.showInfo", "true");

    ConfigLintReport report =
        new ConfigLintRunner(
            tempDir.resolve("strict.json"),
            tempDir.resolve("strict.txt"))
            .run();

    assertNotNull(report.getConfigs());
    assertNotNull(report.getSummary());
  }
}
