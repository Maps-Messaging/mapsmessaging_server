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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlCommentEmitterTest {

  @Test
  void descriptionWinsOverTitleAndIncludesConstraints() {
    SchemaDoc doc = new SchemaDoc();
    doc.setTitle("Fallback title");
    doc.setDescription("  Primary description  ");
    doc.setDefaultValue("10");
    doc.setMinimum("1");
    doc.setMaximum("20");
    doc.setMinLength("1");
    doc.setMaxLength("4");
    doc.setMultipleOf("1");
    doc.setPattern("[0-9]+");
    doc.setFormat("int32");

    List<String> lines = new YamlCommentEmitter().buildCommentLines(doc);

    assertEquals(1, lines.size());
    assertEquals(
        "Primary description (default=10, range=1..20, length=1..4, multipleOf=1, pattern=[0-9]+, format=int32)",
        lines.get(0)
    );
  }

  @Test
  void titleIsUsedWhenDescriptionIsBlank() {
    SchemaDoc doc = new SchemaDoc();
    doc.setTitle("Port number");
    doc.setDescription(" ");

    assertEquals(List.of("Port number"), new YamlCommentEmitter().buildCommentLines(doc));
  }

  @Test
  void exclusiveRangeIsUsedWhenInclusiveRangeIsAbsent() {
    SchemaDoc doc = new SchemaDoc();
    doc.setExclusiveMinimum("0");
    doc.setExclusiveMaximum("100");

    assertEquals(
        List.of("exclusiveRange=0..100"),
        new YamlCommentEmitter().buildCommentLines(doc)
    );
  }

  @Test
  void allowedValuesWrapAcrossLongCommentLines() {
    SchemaDoc doc = new SchemaDoc();
    doc.setAllowedValues(List.of(
        "FIRST_VALUE_WITH_A_REASONABLY_LONG_NAME",
        "SECOND_VALUE_WITH_A_REASONABLY_LONG_NAME",
        "THIRD_VALUE_WITH_A_REASONABLY_LONG_NAME"
    ));

    List<String> lines = new YamlCommentEmitter().buildCommentLines(doc);

    assertTrue(lines.size() >= 2);
    assertTrue(lines.get(0).startsWith("allowed: "));
    assertTrue(lines.get(1).startsWith("         "));
  }

  @Test
  void nullOrEmptyDocumentationProducesNoComments() {
    YamlCommentEmitter emitter = new YamlCommentEmitter();

    assertTrue(emitter.buildCommentLines(null).isEmpty());
    assertTrue(emitter.buildCommentLines(new SchemaDoc()).isEmpty());
  }
}
