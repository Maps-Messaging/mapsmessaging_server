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

import lombok.Value;

import java.util.List;

@Value
public class ConfigLintSummary {

  int infoCount;
  int warnCount;
  int errorCount;

  public static ConfigLintSummary from(List<LintIssue> issues) {
    int infos = 0;
    int warns = 0;
    int errors = 0;

    for (LintIssue issue : issues) {
      if (issue.getSeverity() == LintSeverity.INFO) {
        infos++;
      }
      else if (issue.getSeverity() == LintSeverity.WARN) {
        warns++;
      }
      else if (issue.getSeverity() == LintSeverity.ERROR) {
        errors++;
      }
    }

    return new ConfigLintSummary(infos, warns, errors);
  }
}
