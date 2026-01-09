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

import java.nio.file.Path;

public class ConfigLintMain {

  public static void main(String[] args) throws Exception {
    Path projectDir = Path.of(System.getProperty("user.dir"));
    Path targetDir = projectDir.resolve("target");

    ConfigLintRunner runner = new ConfigLintRunner(
        targetDir.resolve("config-lint-report.json"),
        targetDir.resolve("config-lint-report.txt")
    );

    ConfigLintReport report = runner.run();

    if (report.getSummary().getErrorCount() > 0) {
      System.err.println("Config lint: FAILED (errors=" + report.getSummary().getErrorCount() + ")");
      System.exit(2);
    }

    System.out.println("Config lint: OK (warnings=" + report.getSummary().getWarnCount() + ")");
  }
}
