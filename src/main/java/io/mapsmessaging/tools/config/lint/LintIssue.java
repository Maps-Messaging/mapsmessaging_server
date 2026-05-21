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

@Value
public class LintIssue {

  LintSeverity severity;
  String configName;
  String rootDtoClass;
  String path;
  String ruleId;
  String message;

  public static LintIssue info(String configName, String rootDtoClass, String path, String ruleId, String message) {
    return new LintIssue(LintSeverity.INFO, configName, rootDtoClass, path, ruleId, message);
  }

  public static LintIssue warn(String configName, String rootDtoClass, String path, String ruleId, String message) {
    return new LintIssue(LintSeverity.WARN, configName, rootDtoClass, path, ruleId, message);
  }

  public static LintIssue error(String configName, String rootDtoClass, String path, String ruleId, String message) {
    return new LintIssue(LintSeverity.ERROR, configName, rootDtoClass, path, ruleId, message);
  }
}
