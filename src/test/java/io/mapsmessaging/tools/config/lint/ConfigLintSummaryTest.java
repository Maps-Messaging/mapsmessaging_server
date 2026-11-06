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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigLintSummaryTest {

  @Test
  void emptyIssueListProducesZeroCounts() {
    ConfigLintSummary summary = ConfigLintSummary.from(List.of());

    assertEquals(0, summary.getInfoCount());
    assertEquals(0, summary.getWarnCount());
    assertEquals(0, summary.getErrorCount());
  }

  @Test
  void eachSeverityIsCountedIndependently() {
    ConfigLintSummary summary = ConfigLintSummary.from(List.of(
        LintIssue.info("a", "Root", "one", "I1", "info"),
        LintIssue.info("a", "Root", "two", "I2", "info"),
        LintIssue.warn("a", "Root", "three", "W1", "warn"),
        LintIssue.error("a", "Root", "four", "E1", "error"),
        LintIssue.error("a", "Root", "five", "E2", "error"),
        LintIssue.error("a", "Root", "six", "E3", "error")
    ));

    assertEquals(2, summary.getInfoCount());
    assertEquals(1, summary.getWarnCount());
    assertEquals(3, summary.getErrorCount());
  }
}
