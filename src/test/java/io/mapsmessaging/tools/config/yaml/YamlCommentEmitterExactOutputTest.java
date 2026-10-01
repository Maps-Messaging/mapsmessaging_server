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

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlCommentEmitterExactOutputTest {
  @Test
  void exact_limit_has_no_wrap_or_extra_separator() {
    SchemaDoc doc = new SchemaDoc();
    doc.setAllowedValues(List.of("x".repeat(91)));
    assertEquals(List.of("allowed: " + "x".repeat(91)), new YamlCommentEmitter().buildCommentLines(doc));
  }

  @Test
  void wrap_retains_nine_space_indent_and_drops_leading_comma() {
    SchemaDoc doc = new SchemaDoc();
    doc.setAllowedValues(List.of("x".repeat(91), "second", "third"));
    assertEquals(List.of("allowed: " + "x".repeat(91), "         second, third"),
        new YamlCommentEmitter().buildCommentLines(doc));
  }

  @Test
  void oversized_first_value_preserves_prefix_only_line() {
    SchemaDoc doc = new SchemaDoc();
    doc.setAllowedValues(List.of("x".repeat(92)));
    assertEquals(List.of("allowed: ", "         " + "x".repeat(92)),
        new YamlCommentEmitter().buildCommentLines(doc));
  }

  @Test
  void empty_and_unicode_values_preserve_exact_separators() {
    SchemaDoc doc = new SchemaDoc();
    doc.setAllowedValues(List.of("", "µ", ""));
    assertEquals(List.of("allowed: µ, "), new YamlCommentEmitter().buildCommentLines(doc));
  }
}
