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

import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LintEngineTest {

  @Test
  void missingClassSchemaIsWarningAndStrictModePromotesItToError() {
    List<LintIssue> normal = new LintEngine(false)
        .lintClass("cfg", "Root", Unannotated.class, "root");
    List<LintIssue> strict = new LintEngine(true)
        .lintClass("cfg", "Root", Unannotated.class, "root");

    assertRule(normal, "SCHEMA_CLASS_MISSING", LintSeverity.WARN);
    assertRule(strict, "SCHEMA_CLASS_MISSING", LintSeverity.ERROR);
  }

  @Test
  void classDescriptionIsRequired() {
    List<LintIssue> issues = new LintEngine(false)
        .lintClass("cfg", "Root", EmptyDescription.class, "root");

    assertRule(issues, "DESCRIPTION_MISSING", LintSeverity.WARN);
  }

  @Test
  void scalarFieldRulesDetectWeakExamplesEnumsAndStructuredStrings() throws Exception {
    LintEngine engine = new LintEngine(false);

    List<LintIssue> modeIssues = lint(engine, "mode");
    assertRule(modeIssues, "EXAMPLE_WEAK", LintSeverity.WARN);
    assertRule(modeIssues, "STRING_ENUM_ALLOWABLE_VALUES_MISSING", LintSeverity.WARN);

    List<LintIssue> urlIssues = lint(engine, "endpointUrl");
    assertRule(urlIssues, "STRUCTURED_STRING_CONSTRAINT_MISSING", LintSeverity.WARN);
  }

  @Test
  void numericAndBooleanOptionalFieldsNeedConstraintsAndDefaults() throws Exception {
    List<LintIssue> numeric = lint(new LintEngine(false), "retryCount");
    assertRule(numeric, "NUMERIC_CONSTRAINT_MISSING", LintSeverity.WARN);
    assertRule(numeric, "DEFAULT_VALUE_MISSING", LintSeverity.WARN);

    List<LintIssue> bool = lint(new LintEngine(false), "enabled");
    assertRule(bool, "DEFAULT_VALUE_MISSING", LintSeverity.WARN);
  }

  @Test
  void requiredNullableFieldWithDefaultReportsConflict() throws Exception {
    List<LintIssue> issues = lint(new LintEngine(false), "requiredWithDefault");

    assertRule(issues, "DEFAULT_VALUE_CONFLICT", LintSeverity.WARN);
  }

  @Test
  void mapRulesDistinguishFreeformDiscouragedAndIgnoredFields() throws Exception {
    List<LintIssue> freeform = lint(new LintEngine(false), "freeform");
    assertRule(freeform, "MAP_FIELD_ALLOWED", LintSeverity.INFO);

    List<LintIssue> discouraged = lint(new LintEngine(false), "structuredMap");
    assertRule(discouraged, "MAP_FIELD_DISCOURAGED", LintSeverity.WARN);

    List<LintIssue> ignored = lint(new LintEngine(false), "ignoredMap");
    assertFalse(hasRule(ignored, "MAP_FIELD_DISCOURAGED"));
  }

  @Test
  void enumWithSchemaProducesInformationalConfirmation() throws Exception {
    List<LintIssue> issues = lint(new LintEngine(false), "policy");

    assertRule(issues, "ENUM_SCHEMA_OK", LintSeverity.INFO);
  }

  private static List<LintIssue> lint(LintEngine engine, String fieldName) throws Exception {
    Field field = Fields.class.getDeclaredField(fieldName);
    return engine.lintField("cfg", "Root", Fields.class, field, "root." + fieldName);
  }

  private static boolean hasRule(List<LintIssue> issues, String rule) {
    return issues.stream().anyMatch(issue -> rule.equals(issue.getRuleId()));
  }

  private static void assertRule(List<LintIssue> issues, String rule, LintSeverity severity) {
    assertTrue(
        issues.stream().anyMatch(issue ->
            rule.equals(issue.getRuleId()) && issue.getSeverity() == severity),
        "Expected rule " + rule + " with severity " + severity + " but got " + issues
    );
  }

  static class Unannotated {
  }

  @Schema(description = "")
  static class EmptyDescription {
  }

  enum Policy {
    FIRST,
    SECOND
  }

  static class Fields {
    @Schema(description = "Mode", example = "type")
    String mode;

    @Schema(description = "Endpoint URL", example = "https://example.org")
    String endpointUrl;

    @Schema(
        description = "Retry count",
        example = "3",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    int retryCount;

    @Schema(
        description = "Enabled",
        example = "true",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    boolean enabled;

    @Schema(
        description = "Required value",
        example = "5",
        defaultValue = "5",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true
    )
    int requiredWithDefault;

    @Schema(
        description = "Freeform values",
        additionalProperties = Schema.AdditionalPropertiesValue.TRUE
    )
    Map<String, Object> freeform;

    @Schema(description = "Structured values")
    Map<String, String> structuredMap;

    @Schema(description = "Ignored values")
    @ConfigLintIgnore("MAP_FIELD_DISCOURAGED")
    Map<String, String> ignoredMap;

    @Schema(description = "Policy", example = "FIRST")
    Policy policy;
  }
}
