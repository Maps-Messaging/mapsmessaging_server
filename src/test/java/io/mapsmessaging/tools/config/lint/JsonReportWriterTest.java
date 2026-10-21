package io.mapsmessaging.tools.config.lint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonReportWriterTest {

  @TempDir
  Path tempDir;

  @Test
  void writesReadableIndentedJsonReport() throws Exception {
    List<LintIssue> issues = List.of(
        LintIssue.warn("server", "ExampleDto", "root.value", "rule-1", "warning")
    );
    ConfigLintReport report = new ConfigLintReport(
        "2026-09-20T08:00:00Z",
        List.of(new ConfigLintConfigResult("server", "ExampleDto", issues)),
        ConfigLintSummary.from(issues)
    );
    Path output = tempDir.resolve("report.json");

    JsonReportWriter.write(output, report);

    String raw = Files.readString(output);
    assertTrue(raw.contains(System.lineSeparator()));

    JsonNode json = new ObjectMapper().readTree(raw);
    assertEquals("2026-09-20T08:00:00Z", json.get("generatedAt").asText());
    assertEquals(1, json.get("summary").get("warnCount").asInt());
    assertEquals("rule-1",
        json.get("configs").get(0).get("issues").get(0).get("ruleId").asText());
  }

  @Test
  void ioFailuresArePropagatedToCaller() {
    ConfigLintReport report =
        new ConfigLintReport("now", List.of(), new ConfigLintSummary(0, 0, 0));

    assertThrows(Exception.class, () -> JsonReportWriter.write(tempDir, report));
  }
}
