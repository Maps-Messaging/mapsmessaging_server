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

package io.mapsmessaging.network.protocol.impl.nmea.sentences;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SentenceParserTest {

  @Test
  void parserWithoutSyntaxProducesEmptySentence() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("description", "No fields");
    config.put("aliasOf", "OTHER");

    SentenceParser parser = new SentenceParser("TEST", config);
    Sentence sentence = parser.parse(List.<String>of().iterator());

    assertEquals("TEST", parser.getName());
    assertEquals("No fields", parser.getDescription());
    assertEquals("OTHER", parser.getAlias());
    assertTrue(parser.getConfigs().isEmpty());
    assertEquals("$TEST,", sentence.toString());
  }

  @Test
  void parsesConfiguredFieldsInConfiguredOrder() {
    SentenceParser parser = new SentenceParser("TEST", parserConfig(
        field("first", "String", "", 1),
        field("count", "int", "", 1),
        field("enabled", "boolean", "Y", 1)
    ));

    Sentence sentence = parser.parse(List.of("alpha", "42", "Y").iterator());

    assertEquals("alpha", sentence.get("first").jsonPack());
    assertEquals(42L, sentence.get("count").jsonPack());
    assertEquals(true, sentence.get("enabled").jsonPack());
    assertEquals("$TEST,alpha,42,true", sentence.toString());
  }

  @Test
  void repeatedFieldsReceiveStableIndexedNames() {
    SentenceParser parser = new SentenceParser("TEST", parserConfig(
        field("value", "String", "", 3)
    ));

    Sentence sentence = parser.parse(List.of("one", "two", "three").iterator());

    assertEquals("one", sentence.get("value_0").jsonPack());
    assertEquals("two", sentence.get("value_1").jsonPack());
    assertEquals("three", sentence.get("value_2").jsonPack());
    assertNull(sentence.get("value"));
    assertEquals("$TEST,one,two,three", sentence.toString());
  }

  @Test
  void missingRemainingInputLeavesLaterFieldsAbsent() {
    SentenceParser parser = new SentenceParser("TEST", parserConfig(
        field("first", "String", "", 1),
        field("second", "String", "", 1)
    ));

    Sentence sentence = parser.parse(List.of("only").iterator());

    assertEquals("only", sentence.get("first").jsonPack());
    assertNull(sentence.get("second"));
  }

  @Test
  void unknownTypesAreIgnoredWithoutChangingOrder() {
    SentenceParser parser = new SentenceParser("TEST", parserConfig(
        field("ignored", "NotAType", "", 1),
        field("value", "String", "", 1)
    ));

    Sentence sentence = parser.parse(List.of("ignored-input", "actual").iterator());

    assertNull(sentence.get("ignored"));
    assertEquals("actual", sentence.get("value").jsonPack());
    assertEquals("$TEST,actual", sentence.toString());
  }

  private static ConfigurationProperties parserConfig(ConfigurationProperties... fields) {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("description", "test sentence");

    ConfigurationProperties syntax = new ConfigurationProperties();
    for (int index = 0; index < fields.length; index++) {
      syntax.put(Integer.toString(index + 1), fields[index]);
    }
    config.put("syntax", syntax);
    return config;
  }

  private static ConfigurationProperties field(String name, String type, String param, int repeat) {
    ConfigurationProperties field = new ConfigurationProperties();
    field.put("name", name);
    field.put("type", type);
    field.put("param", param);
    field.put("repeat", Integer.toString(repeat));
    return field;
  }
}
