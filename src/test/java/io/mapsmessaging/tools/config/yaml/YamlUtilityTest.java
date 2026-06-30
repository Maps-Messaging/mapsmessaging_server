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

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlUtilityTest {

  @Test
  void removeVersionFromChildrenOnly_keepsRootAndRemovesNestedVersions() {
    Map<String, Object> nestedMap = new LinkedHashMap<>();
    nestedMap.put("schemaLoadingVersion", 2);
    nestedMap.put("name", "nested");

    Map<String, Object> listEntry = new LinkedHashMap<>();
    listEntry.put("schemaLoadingVersion", 3);

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("schemaLoadingVersion", 1);
    root.put("nested", nestedMap);
    root.put("items", new ArrayList<>(List.of(listEntry, "value")));

    new VersionFieldStripper("schemaLoadingVersion").removeVersionFromChildrenOnly(root);

    assertEquals(1, root.get("schemaLoadingVersion"));
    assertFalse(nestedMap.containsKey("schemaLoadingVersion"));
    assertFalse(listEntry.containsKey("schemaLoadingVersion"));
    assertEquals("nested", nestedMap.get("name"));
  }

  @Test
  void formatScalar_formatsPrimitiveAndEmptyContainerValues() {
    YamlValueFormatter formatter = new YamlValueFormatter();

    assertEquals("null", formatter.formatScalar(null));
    assertEquals("true", formatter.formatScalar(true));
    assertEquals("12.5", formatter.formatScalar(12.500d));
    assertEquals("12", formatter.formatScalar(12.0f));
    assertEquals("{}", formatter.formatScalar(Map.of("key", "value")));
    assertEquals("[]", formatter.formatScalar(List.of("value")));
  }

  @Test
  void formatScalar_quotesYamlLiteralsAndEscapesQuotedContent() {
    YamlValueFormatter formatter = new YamlValueFormatter();

    assertEquals("\"\"", formatter.formatScalar(""));
    assertEquals("\"true\"", formatter.formatScalar("true"));
    assertEquals("\"1.25\"", formatter.formatScalar("1.25"));
    assertEquals("\" key \"", formatter.formatScalar(" key "));
    assertEquals("\"a#b\"", formatter.formatScalar("a#b"));
    assertEquals("\"a#\\\\\\\"b\"", formatter.formatScalar("a#\\\"b"));
    assertEquals("plain-value", formatter.formatScalar("plain-value"));
  }

  @Test
  void jsonElementConverter_preservesNestedValuesAndInsertionOrder() {
    Object result = JsonElementConverter.toJava(JsonParser.parseString(
        "{\"first\":1,\"nested\":{\"enabled\":true},\"items\":[\"value\",null]}"));

    assertInstanceOf(Map.class, result);
    Map<?, ?> resultMap = (Map<?, ?>) result;
    assertEquals(List.of("first", "nested", "items"), List.copyOf(resultMap.keySet()));
    assertEquals("1", resultMap.get("first").toString());
    assertEquals(Map.of("enabled", true), resultMap.get("nested"));
    assertEquals(java.util.Arrays.asList("value", null), resultMap.get("items"));
  }

  @Test
  void jsonElementConverter_nullInputs_returnNull() {
    assertNull(JsonElementConverter.toJava(null));
    assertNull(JsonElementConverter.toJava(JsonParser.parseString("null")));
  }
}
