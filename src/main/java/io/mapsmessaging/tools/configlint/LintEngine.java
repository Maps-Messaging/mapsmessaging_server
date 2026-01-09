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

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class LintEngine {

  public List<LintIssue> lintClass(
      String configName,
      String rootDtoName,
      Class<?> clazz,
      String path
  ) {
    List<LintIssue> issues = new ArrayList<>();

    Schema schema = clazz.getAnnotation(Schema.class);
    if (schema == null) {
      issues.add(LintIssue.warn(
          configName,
          rootDtoName,
          path,
          "SCHEMA_CLASS_MISSING",
          "Missing @Schema on class " + clazz.getName()
      ));
    }

    return issues;
  }

  public List<LintIssue> lintField(
      String configName,
      String rootDtoName,
      Class<?> declaringClass,
      Field field,
      String path
  ) {
    List<LintIssue> issues = new ArrayList<>();

    Schema schema = field.getAnnotation(Schema.class);
    if (schema == null) {
      issues.add(LintIssue.warn(
          configName,
          rootDtoName,
          path,
          "SCHEMA_FIELD_MISSING",
          "Missing @Schema on field " + declaringClass.getName() + "." + field.getName()
      ));
    }

    Class<?> fieldType = field.getType();

    if (fieldType == String.class) {
      issues.addAll(lintStringEnumHeuristic(configName, rootDtoName, field, path, schema));
    }

    if (ReflectionTypes.isNumeric(fieldType)) {
      issues.addAll(lintNumericRange(configName, rootDtoName, field, path, schema));
    }

    if (fieldType.isEnum()) {
      if (schema == null) {
        issues.add(LintIssue.warn(
            configName,
            rootDtoName,
            path,
            "ENUM_SCHEMA_MISSING",
            "Enum field lacks @Schema; OpenAPI may still infer values, but metadata will be weaker"
        ));
      }
      else {
        issues.add(LintIssue.info(
            configName,
            rootDtoName,
            path,
            "ENUM_SCHEMA_OK",
            "Enum field has @Schema"
        ));
      }
    }

    return issues;
  }

  private List<LintIssue> lintStringEnumHeuristic(
      String configName,
      String rootDtoName,
      Field field,
      String path,
      Schema schema
  ) {
    List<LintIssue> issues = new ArrayList<>();

    boolean looksLikeEnum = StringEnumHeuristics.looksLikeEnum(field.getName());

    if (!looksLikeEnum) {
      return issues;
    }

    if (schema == null) {
      issues.add(LintIssue.warn(
          configName,
          rootDtoName,
          path,
          "STRING_ENUM_ALLOWABLE_VALUES_MISSING",
          "String field looks like an enum but has no @Schema(allowableValues=...)"
      ));
      return issues;
    }

    String[] allowableValues = schema.allowableValues();
    if (allowableValues == null || allowableValues.length == 0) {
      issues.add(LintIssue.warn(
          configName,
          rootDtoName,
          path,
          "STRING_ENUM_ALLOWABLE_VALUES_MISSING",
          "String field looks like an enum but @Schema(allowableValues=...) is missing/empty"
      ));
    }
    else {
      issues.add(LintIssue.info(
          configName,
          rootDtoName,
          path,
          "STRING_ENUM_OK",
          "String enum exposed via allowableValues"
      ));
    }

    return issues;
  }

  private List<LintIssue> lintNumericRange(
      String configName,
      String rootDtoName,
      Field field,
      String path,
      Schema schema
  ) {
    List<LintIssue> issues = new ArrayList<>();

    if (schema == null) {
      issues.add(LintIssue.warn(
          configName,
          rootDtoName,
          path,
          "NUMERIC_RANGE_MISSING",
          "Numeric field missing @Schema(minimum/maximum)"
      ));
      return issues;
    }

    String minimum = schema.minimum();
    String maximum = schema.maximum();

    boolean minimumPresent = minimum != null && !minimum.isBlank();
    boolean maximumPresent = maximum != null && !maximum.isBlank();

    if (!minimumPresent && !maximumPresent) {
      issues.add(LintIssue.warn(
          configName,
          rootDtoName,
          path,
          "NUMERIC_RANGE_MISSING",
          "Numeric field missing @Schema(minimum/maximum)"
      ));
    }

    return issues;
  }
}
