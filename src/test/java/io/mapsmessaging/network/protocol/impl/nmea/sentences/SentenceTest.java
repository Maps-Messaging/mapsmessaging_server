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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.network.protocol.impl.nmea.types.BooleanType;
import io.mapsmessaging.network.protocol.impl.nmea.types.LongType;
import io.mapsmessaging.network.protocol.impl.nmea.types.StringType;
import io.mapsmessaging.network.protocol.impl.nmea.types.Type;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SentenceTest {

  @Test
  void accessorsAndWireRenderingPreserveConfiguredOrder() {
    Map<String, Type> values = new LinkedHashMap<>();
    values.put("name", new StringType("alpha"));
    values.put("count", new LongType("12"));
    values.put("enabled", new BooleanType("Y", "Y"));

    Sentence sentence = new Sentence(
        "TEST",
        "Example sentence",
        List.of("count", "name", "enabled"),
        values
    );

    assertEquals("TEST", sentence.getName());
    assertEquals("Example sentence", sentence.getDescription());
    assertSame(values.get("name"), sentence.get("name"));
    assertNull(sentence.get("missing"));
    assertEquals("$TEST,12,alpha,true", sentence.toString());
  }

  @Test
  void jsonRenderingPacksTypedValuesAndDescription() {
    Map<String, Type> values = new LinkedHashMap<>();
    values.put("name", new StringType("alpha"));
    values.put("count", new LongType("12"));

    Sentence sentence = new Sentence("TEST", "Example sentence", List.of("name", "count"), values);

    JsonObject json = JsonParser.parseString(sentence.toJSON()).getAsJsonObject();

    assertEquals("Example sentence", json.get("description").getAsString());
    assertEquals("alpha", json.getAsJsonObject("TEST").get("name").getAsString());
    assertEquals(12L, json.getAsJsonObject("TEST").get("count").getAsLong());
  }

  @Test
  void emptySentenceStillHasStableWireRepresentation() {
    Sentence sentence = new Sentence("EMPTY", "No values", List.of(), Map.of());

    assertEquals("$EMPTY,", sentence.toString());
    assertTrue(JsonParser.parseString(sentence.toJSON()).getAsJsonObject()
        .getAsJsonObject("EMPTY").entrySet().isEmpty());
  }
}
