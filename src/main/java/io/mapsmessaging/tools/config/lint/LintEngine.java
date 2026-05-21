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

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.tools.config.schema.ProtocolTransformationNameRegistry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.reflect.Field;
import java.util.*;

public class LintEngine {

  private static final Set<String> WEAK_EXAMPLES = Set.of(
      "type",
      "string",
      "value",
      "example",
      "changeme",
      "todo"
  );

  private static final Set<String> OPEN_VOCAB_FIELD_NAMES = Set.of(
      "contenttype",
      "mediatype",
      "mimetype"
  );

  private final boolean strict;

  public LintEngine(boolean strict) {
    this.strict = strict;
  }

  public List<LintIssue> lintClass(
      String configName,
      String rootDtoName,
      Class<?> clazz,
      String path
  ) {
    List<LintIssue> issues = new ArrayList<>();

    Schema schema = clazz.getAnnotation(Schema.class);
    if (schema == null) {
      issues.add(issue(
          LintSeverity.WARN,
          configName,
          rootDtoName,
          path,
          "SCHEMA_CLASS_MISSING",
          "Missing @Schema on class " + clazz.getName()
      ));
    }
    else {
      if (schema.description() == null || schema.description().isBlank()) {
        issues.add(issue(
            LintSeverity.WARN,
            configName,
            rootDtoName,
            path,
            "DESCRIPTION_MISSING",
            "Schema description is missing/empty on class " + clazz.getName()
        ));
      }
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

    Class<?> fieldType = field.getType();

    boolean isMap = Map.class.isAssignableFrom(fieldType);
    boolean isCollection = List.class.isAssignableFrom(fieldType) || isMap;
    boolean isNestedDto = BaseConfigDTO.class.isAssignableFrom(fieldType);

    Schema schema = field.getAnnotation(Schema.class);
    if (schema == null) {
      issues.add(issue(
          LintSeverity.WARN,
          configName,
          rootDtoName,
          path,
          "SCHEMA_FIELD_MISSING",
          "Missing @Schema on field " + declaringClass.getName() + "." + field.getName()
      ));
    }
    else {
      issues.addAll(lintSchemaBasics(configName, rootDtoName, field, path, schema, isCollection, isNestedDto));
    }

    // New: discourage Map fields (prefer DTOs)
    issues.addAll(lintMapFields(configName, rootDtoName, field, path, schema, isMap));

    if (fieldType == String.class) {
      issues.addAll(lintStructuredString(configName, rootDtoName, field, path, schema));
      issues.addAll(lintStringEnumHeuristic(configName, rootDtoName, field, path, schema));
    }

    if (ReflectionTypes.isNumeric(fieldType)) {
      issues.addAll(lintNumericConstraints(configName, rootDtoName, field, path, schema));
    }

    if (isBoolean(fieldType)) {
      issues.addAll(lintBooleanDefaults(configName, rootDtoName, field, path, schema));
    }

    if (fieldType.isEnum()) {
      if (schema == null) {
        issues.add(issue(
            LintSeverity.WARN,
            configName,
            rootDtoName,
            path,
            "ENUM_SCHEMA_MISSING",
            "Enum field lacks @Schema; OpenAPI may infer values, but metadata will be weaker"
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

  private List<LintIssue> lintMapFields(
      String configName,
      String rootDtoName,
      Field field,
      String path,
      Schema schema,
      boolean isMap
  ) {
    List<LintIssue> issues = new ArrayList<>();

    if (!isMap) {
      return issues;
    }

    if (isIgnored(field, "MAP_FIELD_DISCOURAGED")) {
      return issues;
    }

    // Allow explicit freeform maps via @Schema(additionalProperties = TRUE)
    if (schema != null && schema.additionalProperties() == Schema.AdditionalPropertiesValue.TRUE) {
      issues.add(LintIssue.info(
          configName,
          rootDtoName,
          path,
          "MAP_FIELD_ALLOWED",
          "Map field marked as freeform via additionalProperties=true"
      ));
      return issues;
    }

    issues.add(issue(
        LintSeverity.WARN,
        configName,
        rootDtoName,
        path,
        "MAP_FIELD_DISCOURAGED",
        "Map fields should usually be represented by a DTO; if intentionally freeform, set @Schema(additionalProperties=TRUE) or @ConfigLintIgnore(\"MAP_FIELD_DISCOURAGED\")"
    ));

    return issues;
  }



  private List<LintIssue> lintSchemaBasics(
      String configName,
      String rootDtoName,
      Field field,
      String path,
      Schema schema,
      boolean isCollection,
      boolean isNestedDto
  ) {
    List<LintIssue> issues = new ArrayList<>();

    if (schema.description() == null || schema.description().isBlank()) {
      issues.add(issue(
          LintSeverity.WARN,
          configName,
          rootDtoName,
          path,
          "DESCRIPTION_MISSING",
          "Schema description is missing/empty"
      ));
    }

    boolean isLeafScalar = isLeafScalar(field, isCollection, isNestedDto);

    if (isLeafScalar) {
      String example = schema.example();
      if (example == null || example.isBlank()) {
        issues.add(issue(
            LintSeverity.WARN,
            configName,
            rootDtoName,
            path,
            "EXAMPLE_MISSING",
            "Leaf scalar field missing @Schema(example)"
        ));
      }
      else {
        String lowered = example.trim().toLowerCase(Locale.ROOT);
        if (WEAK_EXAMPLES.contains(lowered)) {
          issues.add(issue(
              LintSeverity.WARN,
              configName,
              rootDtoName,
              path,
              "EXAMPLE_WEAK",
              "Example looks like a placeholder: '" + example.trim() + "'"
          ));
        }
      }



      boolean isOptional = schema.requiredMode() == Schema.RequiredMode.NOT_REQUIRED;
      Class<?> fieldType = field.getType();
      boolean isPrimitive = fieldType.isPrimitive();
      boolean requiresDefault = isOptional && isPrimitive;
      String defaultValue = schema.defaultValue();
      boolean defaultMissing = requiresDefault && defaultValue.isBlank();
      boolean required = schema.requiredMode() == Schema.RequiredMode.REQUIRED;

      boolean hasDefault = schema.defaultValue() != null
          && !schema.defaultValue().isBlank()
          && schema.nullable();

      if (defaultMissing) {
        issues.add(issue(
            LintSeverity.WARN,
            configName,
            rootDtoName,
            path,
            "DEFAULT_VALUE_MISSING",
            "Optional field is missing @Schema(defaultValue)"
        ));
      }

      if (required && hasDefault) {
        issues.add(issue(
            LintSeverity.WARN,
            configName,
            rootDtoName,
            path,
            "DEFAULT_VALUE_CONFLICT",
            "Field is REQUIRED but also declares defaultValue"
        ));
      }
    }

    return issues;
  }

  private List<LintIssue> lintStructuredString(
      String configName,
      String rootDtoName,
      Field field,
      String path,
      Schema schema
  ) {
    List<LintIssue> issues = new ArrayList<>();

    String fieldNameLower = field.getName().toLowerCase(Locale.ROOT);

    boolean looksLikeUrl = containsAny(fieldNameLower, "url", "uri", "endpoint", "baseurl");
    boolean looksLikeHost = containsAny(fieldNameLower, "host", "hostname");
    boolean looksLikeMime = OPEN_VOCAB_FIELD_NAMES.contains(fieldNameLower);

    if (!looksLikeUrl && !looksLikeHost && !looksLikeMime) {
      return issues;
    }

    if (schema == null) {
      issues.add(issue(
          LintSeverity.WARN,
          configName,
          rootDtoName,
          path,
          "STRUCTURED_STRING_CONSTRAINT_MISSING",
          "Structured string field missing @Schema; expected format/pattern"
      ));
      return issues;
    }

    boolean hasFormat = schema.format() != null && !schema.format().isBlank();
    boolean hasPattern = schema.pattern() != null && !schema.pattern().isBlank();

    if (!hasFormat && !hasPattern) {
      String expected = looksLikeUrl
          ? "uri"
          : looksLikeMime
          ? "media-type"
          : "hostname";

      issues.add(issue(
          LintSeverity.WARN,
          configName,
          rootDtoName,
          path,
          "STRUCTURED_STRING_CONSTRAINT_MISSING",
          "Field looks like a " + expected + " and should declare @Schema(format=...) or @Schema(pattern=...)"
      ));
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

    if (isIgnored(field, "STRING_ENUM_ALLOWABLE_VALUES_MISSING")) {
      return issues;
    }

    // ---- Dynamic enum fields (generator-injected enums) ----
    if (isProtocolTransformationField(field, path)) {
      List<String> allowed = ProtocolTransformationNameRegistry.getAllowedNames(true);

      if (schema != null) {
        validateSchemaExampleAndDefaultAgainstList(
            issues,
            configName,
            rootDtoName,
            path,
            schema,
            allowed
        );

        // Optional sanity: empty default + nullable true is ambiguous
        if ("".equals(schema.defaultValue()) && schema.nullable()) {
          issues.add(issue(
              LintSeverity.WARN,
              configName,
              rootDtoName,
              path,
              "DYNAMIC_ENUM_NULLABLE_AMBIGUOUS",
              "Field uses defaultValue=\"\" but is nullable=true. Prefer nullable=false when \"\" is the 'none' value."
          ));
        }
      }

      issues.add(LintIssue.info(
          configName,
          rootDtoName,
          path,
          "STRING_ENUM_DYNAMIC_OK",
          "String enum is dynamically generated (ServiceLoader) and injected into schema"
      ));
      return issues;
    }

    // ---- Existing heuristic logic ----
    String fieldNameLower = field.getName().toLowerCase(Locale.ROOT);
    boolean openVocab = field.isAnnotationPresent(OpenVocab.class) || OPEN_VOCAB_FIELD_NAMES.contains(fieldNameLower);

    boolean looksLikeEnum = StringEnumHeuristics.looksLikeEnum(field.getName());
    if (!looksLikeEnum || openVocab) {
      return issues;
    }

    if (schema == null) {
      issues.add(issue(
          LintSeverity.WARN,
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
      issues.add(issue(
          LintSeverity.WARN,
          configName,
          rootDtoName,
          path,
          "STRING_ENUM_ALLOWABLE_VALUES_MISSING",
          "String field looks like an enum but @Schema(allowableValues=...) is missing/empty"
      ));
    } else {
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

  private boolean isProtocolTransformationField(Field field, String path) {
    if (!String.class.equals(field.getType())) {
      return false;
    }
    // Robust enough for now: either name or full path match
    if ("linkTransformation".equals(field.getName())) {
      return true;
    }
    return path != null && path.endsWith(".linkTransformation");
  }

  private void validateSchemaExampleAndDefaultAgainstList(
      List<LintIssue> issues,
      String configName,
      String rootDtoName,
      String path,
      Schema schema,
      List<String> allowed
  ) {
    Object example = schema.example();
    if (example != null) {
      String exampleString = String.valueOf(example).trim();
      if (!exampleString.isEmpty() && !allowed.contains(exampleString)) {
        issues.add(issue(
            LintSeverity.WARN,
            configName,
            rootDtoName,
            path,
            "DYNAMIC_ENUM_EXAMPLE_INVALID",
            "Schema example is not a valid transformation name: " + exampleString
        ));
      }
    }

    String defaultValue = schema.defaultValue();
    if (defaultValue != null) {
      String defaultTrimmed = defaultValue.trim();

      // If default is intentionally blank, accept it (matches includeEmpty=true)
      if (!defaultTrimmed.isEmpty() && !allowed.contains(defaultTrimmed)) {
        issues.add(issue(
            LintSeverity.WARN,
            configName,
            rootDtoName,
            path,
            "DYNAMIC_ENUM_DEFAULT_INVALID",
            "Schema defaultValue is not a valid transformation name: " + defaultTrimmed
        ));
      }
    }
  }


  private List<LintIssue> lintNumericConstraints(
      String configName,
      String rootDtoName,
      Field field,
      String path,
      Schema schema
  ) {
    List<LintIssue> issues = new ArrayList<>();

    if (isIgnored(field, "NUMERIC_CONSTRAINT_MISSING")) {
      return issues;
    }

    if (schema == null) {
      issues.add(issue(
          LintSeverity.WARN,
          configName,
          rootDtoName,
          path,
          "NUMERIC_CONSTRAINT_MISSING",
          "Numeric field missing @Schema constraints (minimum/maximum, allowableValues, or multipleOf)"
      ));
      return issues;
    }

    boolean hasRange = (schema.minimum() != null && !schema.minimum().isBlank())
        || (schema.maximum() != null && !schema.maximum().isBlank());

    boolean hasAllowableValues = schema.allowableValues() != null && schema.allowableValues().length > 0;

    boolean hasMultipleOf = schema.multipleOf() > 0.0;

    if (!hasRange && !hasAllowableValues && !hasMultipleOf) {
      issues.add(issue(
          LintSeverity.WARN,
          configName,
          rootDtoName,
          path,
          "NUMERIC_CONSTRAINT_MISSING",
          "Numeric field must define minimum/maximum, allowableValues, or multipleOf"
      ));
    }

    return issues;
  }

  private List<LintIssue> lintBooleanDefaults(
      String configName,
      String rootDtoName,
      Field field,
      String path,
      Schema schema
  ) {
    List<LintIssue> issues = new ArrayList<>();

    if (schema == null) {
      return issues;
    }

    boolean required = schema.requiredMode() == Schema.RequiredMode.REQUIRED;

    Class<?> fieldType = field.getType();
    boolean isPrimitiveBoolean = fieldType == boolean.class;

    String defaultValue = schema.defaultValue(); // never null
    boolean hasDefault = !defaultValue.isBlank();

    boolean shouldWarn = !required && isPrimitiveBoolean && !hasDefault;

    if (shouldWarn) {
      issues.add(issue(
          LintSeverity.WARN,
          configName,
          rootDtoName,
          path,
          "DEFAULT_VALUE_MISSING",
          "Optional boolean field should declare @Schema(defaultValue)"
      ));
    }

    return issues;
  }

  private boolean isLeafScalar(Field field, boolean isCollection, boolean isNestedDto) {
    if (isCollection || isNestedDto) {
      return false;
    }

    Class<?> type = field.getType();

    if (type == String.class) {
      return true;
    }

    if (ReflectionTypes.isNumeric(type)) {
      return true;
    }

    if (isBoolean(type)) {
      return true;
    }

    return type.isEnum();
  }

  private boolean isBoolean(Class<?> type) {
    return type == boolean.class || type == Boolean.class;
  }

  private boolean containsAny(String text, String... tokens) {
    for (String token : tokens) {
      if (text.contains(token)) {
        return true;
      }
    }
    return false;
  }

  private boolean isIgnored(Field field, String ruleId) {
    ConfigLintIgnore ignore = field.getAnnotation(ConfigLintIgnore.class);
    if (ignore == null) {
      return false;
    }

    String[] ruleIds = ignore.value();
    if (ruleIds == null) {
      return false;
    }

    for (String id : ruleIds) {
      if (ruleId.equals(id)) {
        return true;
      }
    }

    return false;
  }

  private LintIssue issue(
      LintSeverity baseSeverity,
      String configName,
      String rootDtoName,
      String path,
      String ruleId,
      String message
  ) {
    LintSeverity finalSeverity = baseSeverity;
    if (strict && baseSeverity == LintSeverity.WARN) {
      finalSeverity = LintSeverity.ERROR;
    }

    if (finalSeverity == LintSeverity.ERROR) {
      return LintIssue.error(configName, rootDtoName, path, ruleId, message);
    }

    if (finalSeverity == LintSeverity.WARN) {
      return LintIssue.warn(configName, rootDtoName, path, ruleId, message);
    }

    return LintIssue.info(configName, rootDtoName, path, ruleId, message);
  }
}
