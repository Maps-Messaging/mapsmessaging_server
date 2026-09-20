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

package io.mapsmessaging.tools.config.schema;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DynamicEnumSchemaInjectorTest {

  @Test
  void matchingStringFieldReceivesDynamicEnumAndDefault() {
    Map<String, Object> node = DynamicEnumSchemaInjector.newEmptyPropertyNode();

    assertTrue(DynamicEnumSchemaInjector.injectProtocolTransformationEnumIfApplicable(
        "linkTransformation", String.class, node));

    assertEquals("string", node.get("type"));
    assertEquals(ProtocolTransformationNameRegistry.getAllowedNames(true), node.get("enum"));
    assertEquals("", node.get("default"));
  }

  @Test
  void existingDefaultIsPreserved() {
    Map<String, Object> node = DynamicEnumSchemaInjector.newEmptyPropertyNode();
    node.put("default", "custom");

    assertTrue(DynamicEnumSchemaInjector.injectProtocolTransformationEnumIfApplicable(
        "linkTransformation", String.class, node));

    assertEquals("custom", node.get("default"));
  }

  @Test
  void nonMatchingInputsAreIgnored() {
    assertFalse(DynamicEnumSchemaInjector.injectProtocolTransformationEnumIfApplicable(
        null, String.class, Map.of()));
    assertFalse(DynamicEnumSchemaInjector.injectProtocolTransformationEnumIfApplicable(
        "linkTransformation", null, new java.util.LinkedHashMap<>()));
    assertFalse(DynamicEnumSchemaInjector.injectProtocolTransformationEnumIfApplicable(
        "linkTransformation", Integer.class, new java.util.LinkedHashMap<>()));
    assertFalse(DynamicEnumSchemaInjector.injectProtocolTransformationEnumIfApplicable(
        "other", String.class, new java.util.LinkedHashMap<>()));
    assertFalse(DynamicEnumSchemaInjector.injectProtocolTransformationEnumIfApplicable(
        "linkTransformation", String.class, null));
  }
}
