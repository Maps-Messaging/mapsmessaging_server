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

package io.mapsmessaging.tools.configlint;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class TextReportWriter {

  private TextReportWriter() {
  }

  public static void write(Path path, ConfigLintReport report) throws Exception {
    StringBuilder output = new StringBuilder(16_384);

    output.append("MAPS Config Lint Report\n");
    output.append("Generated: ").append(report.getGeneratedAt()).append("\n");
    output.append("Summary: info=").append(report.getSummary().getInfoCount())
        .append(" warn=").append(report.getSummary().getWarnCount())
        .append(" error=").append(report.getSummary().getErrorCount())
        .append("\n\n");

    for (ConfigLintConfigResult config : report.getConfigs()) {
      output.append("Config: ").append(config.getConfigName()).append("\n");
      output.append("Root DTO: ").append(config.getRootDtoClass()).append("\n");

      if (config.getIssues().isEmpty()) {
        output.append("  No issues.\n\n");
        continue;
      }

      config.getIssues().stream()
          .sorted(Comparator
              .comparing((LintIssue i) -> i.getSeverity().name())
              .thenComparing(LintIssue::getPath))
          .forEach(issue -> {
            output.append("  [").append(issue.getSeverity()).append("] ")
                .append(issue.getRuleId())
                .append(" @ ")
                .append(issue.getPath() == null ? "" : issue.getPath())
                .append("\n");
            output.append("      ").append(issue.getMessage()).append("\n");
          });

      output.append("\n");
    }

    Files.write(path, output.toString().getBytes(StandardCharsets.UTF_8));
  }
}
