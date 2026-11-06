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

package io.mapsmessaging.tools.config.lint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextReportWriterTest {

  @TempDir
  Path tempDir;

  @Test
  void writesSummaryConfigsAndSortedIssues() throws Exception {
    List<LintIssue> issues = List.of(
        LintIssue.warn("network", "RootDto", "z.path", "WARN_RULE", "warning"),
        LintIssue.info("network", "RootDto", "m.path", "INFO_RULE", "information"),
        LintIssue.error("network", "RootDto", "a.path", "ERROR_RULE", "error")
    );

    ConfigLintReport report = new ConfigLintReport(
        "2026-09-19T20:00:00Z",
        List.of(
            new ConfigLintConfigResult("network", "RootDto", issues),
            new ConfigLintConfigResult("empty", "EmptyDto", List.of())
        ),
        ConfigLintSummary.from(issues)
    );

    Path output = tempDir.resolve("lint.txt");
    TextReportWriter.write(output, report);

    String text = Files.readString(output);
    assertTrue(text.startsWith("MAPS Config Lint Report\n"));
    assertTrue(text.contains("Generated: 2026-09-19T20:00:00Z"));
    assertTrue(text.contains("Summary: info=1 warn=1 error=1"));
    assertTrue(text.contains("Config: network"));
    assertTrue(text.contains("Root DTO: RootDto"));
    assertTrue(text.contains("Config: empty"));
    assertTrue(text.contains("  No issues."));

    int errorIndex = text.indexOf("[ERROR]");
    int infoIndex = text.indexOf("[INFO]");
    int warnIndex = text.indexOf("[WARN]");
    assertTrue(errorIndex < infoIndex);
    assertTrue(infoIndex < warnIndex);
  }
}
