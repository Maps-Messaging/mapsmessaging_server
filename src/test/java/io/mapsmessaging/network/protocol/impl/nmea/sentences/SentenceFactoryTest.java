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

class SentenceFactoryTest {

  @Test
  void configuredSentenceIsParsedAndUnknownSentenceReturnsNull() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("BASE", sentenceConfig("Base", null));

    SentenceFactory factory = new SentenceFactory(properties);

    Sentence sentence = factory.parse("BASE", List.of("alpha").iterator());
    assertNotNull(sentence);
    assertEquals("alpha", sentence.get("value").jsonPack());
    assertNull(factory.parse("UNKNOWN", List.of("x").iterator()));
  }

  @Test
  void aliasWithoutOwnSyntaxInheritsTargetSyntax() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("BASE", sentenceConfig("Base", null));

    ConfigurationProperties alias = new ConfigurationProperties();
    alias.put("description", "Alias");
    alias.put("aliasOf", "BASE");
    properties.put("ALIAS", alias);

    SentenceFactory factory = new SentenceFactory(properties);
    Sentence parsed = factory.parse("ALIAS", List.of("inherited").iterator());

    assertNotNull(parsed);
    assertEquals("inherited", parsed.get("value").jsonPack());
  }

  @Test
  void aliasWithOwnSyntaxKeepsItsOwnConfiguration() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("BASE", sentenceConfig("Base", null));
    properties.put("ALIAS", sentenceConfig("Alias", "BASE"));

    Sentence parsed = new SentenceFactory(properties)
        .parse("ALIAS", List.of("own").iterator());

    assertEquals("own", parsed.get("value").jsonPack());
  }

  private static ConfigurationProperties sentenceConfig(String description, String aliasOf) {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("description", description);
    if (aliasOf != null) {
      config.put("aliasOf", aliasOf);
    }

    ConfigurationProperties syntax = new ConfigurationProperties();
    ConfigurationProperties entry = new ConfigurationProperties();
    entry.put("name", "value");
    entry.put("type", "String");
    syntax.put("1", entry);
    config.put("syntax", syntax);
    return config;
  }
}
