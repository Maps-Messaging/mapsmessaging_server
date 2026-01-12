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

package io.mapsmessaging.tools.config.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class YamlCommentEmitter {

  private static final int MAX_COMMENT_LINE_LENGTH = 100;

  public List<String> buildCommentLines(SchemaDoc schemaDoc) {
    List<String> lines = new ArrayList<>();
    if (schemaDoc == null) {
      return lines;
    }

    String description = schemaDoc.getDescription();
    String title = schemaDoc.getTitle();

    String baseText = null;
    if (description != null && !description.isBlank()) {
      baseText = description.trim();
    } else if (title != null && !title.isBlank()) {
      baseText = title.trim();
    }

    String constraints = buildConstraintsSuffix(schemaDoc);

    if (baseText != null) {
      if (constraints != null) {
        lines.add(baseText + " (" + constraints + ")");
      } else {
        lines.add(baseText);
      }
    } else if (constraints != null) {
      lines.add(constraints);
    }

    List<String> allowedLines = buildAllowedLines(schemaDoc);
    lines.addAll(allowedLines);

    return lines;
  }

  private String buildConstraintsSuffix(SchemaDoc schemaDoc) {
    StringJoiner joiner = new StringJoiner(", ");

    if (schemaDoc.getDefaultValue() != null) {
      joiner.add("default=" + schemaDoc.getDefaultValue());
    }

    String range = buildRange(schemaDoc);
    if (range != null) {
      joiner.add(range);
    }

    if (schemaDoc.getMinLength() != null || schemaDoc.getMaxLength() != null) {
      String minLength = schemaDoc.getMinLength() != null ? schemaDoc.getMinLength() : "";
      String maxLength = schemaDoc.getMaxLength() != null ? schemaDoc.getMaxLength() : "";
      joiner.add("length=" + minLength + ".." + maxLength);
    }

    if (schemaDoc.getMultipleOf() != null) {
      joiner.add("multipleOf=" + schemaDoc.getMultipleOf());
    }

    if (schemaDoc.getPattern() != null && !schemaDoc.getPattern().isBlank()) {
      joiner.add("pattern=" + schemaDoc.getPattern());
    }

    if (schemaDoc.getFormat() != null && !schemaDoc.getFormat().isBlank()) {
      joiner.add("format=" + schemaDoc.getFormat());
    }

    String result = joiner.toString();
    if (result.isBlank()) {
      return null;
    }
    return result;
  }

  private String buildRange(SchemaDoc schemaDoc) {
    if (schemaDoc.getMinimum() != null || schemaDoc.getMaximum() != null) {
      String minimum = schemaDoc.getMinimum() != null ? schemaDoc.getMinimum() : "";
      String maximum = schemaDoc.getMaximum() != null ? schemaDoc.getMaximum() : "";
      return "range=" + minimum + ".." + maximum;
    }

    if (schemaDoc.getExclusiveMinimum() != null || schemaDoc.getExclusiveMaximum() != null) {
      String minimum = schemaDoc.getExclusiveMinimum() != null ? schemaDoc.getExclusiveMinimum() : "";
      String maximum = schemaDoc.getExclusiveMaximum() != null ? schemaDoc.getExclusiveMaximum() : "";
      return "exclusiveRange=" + minimum + ".." + maximum;
    }

    return null;
  }

  private List<String> buildAllowedLines(SchemaDoc schemaDoc) {
    List<String> lines = new ArrayList<>();

    if (schemaDoc.getAllowedValues() == null || schemaDoc.getAllowedValues().isEmpty()) {
      return lines;
    }

    String prefix = "allowed: ";
    String current = prefix;

    for (String value : schemaDoc.getAllowedValues()) {
      String token = value;
      if (!current.equals(prefix)) {
        token = ", " + token;
      }

      if ((current.length() + token.length()) > MAX_COMMENT_LINE_LENGTH) {
        lines.add(current);
        current = "         " + value;
      } else {
        current = current + token;
      }
    }

    if (!current.isBlank()) {
      lines.add(current);
    }

    return lines;
  }
}
