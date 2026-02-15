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
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.api.transformers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.jsonmutate.JsonMutator;
import io.mapsmessaging.dto.rest.config.transformer.jsonmutate.JsonMutateOpDTO;
import io.mapsmessaging.dto.rest.config.transformer.jsonmutate.JsonMutateOperation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonMutateTransformationTest {

  private static final Gson gson = new Gson();

  @Test
  void setAddsOrOverwritesAField() {
    JsonMutateTransformation transformation = new JsonMutateTransformation(
        new JsonMutator(List.of(
            buildSet("payload.temperature", 21.5),
            buildSet("payload.unit", "c")
        ))
    );

    ParsedMessage input = new ParsedMessage(
        "/device/sensor",
        messageWithJson("""
            {"payload":{"temperature":20.0,"debug":true}}
            """)
    );

    ParsedMessage output = transformation.transform("/device/sensor", input);

    assertNotNull(output);
    assertJsonEquals("""
        {"payload":{"temperature":21.5,"debug":true,"unit":"c"}}
        """, jsonFromMessage(output.getMessage()));
  }

  @Test
  void removeDeletesAFieldIfPresent() {
    JsonMutateTransformation transformation = new JsonMutateTransformation(
        new JsonMutator(List.of(
            buildRemove("payload.debug")
        ))
    );

    ParsedMessage input = new ParsedMessage(
        "/device/sensor",
        messageWithJson("""
            {"payload":{"temperature":20.0,"debug":true}}
            """)
    );

    ParsedMessage output = transformation.transform("/device/sensor", input);

    assertNotNull(output);
    assertJsonEquals("""
        {"payload":{"temperature":20.0}}
        """, jsonFromMessage(output.getMessage()));
  }

  @Test
  void removeMissingFieldIsNoOp() {
    JsonMutateTransformation transformation = new JsonMutateTransformation(
        new JsonMutator(List.of(
            buildRemove("$.payload.nope")
        ))
    );

    ParsedMessage input = new ParsedMessage(
        "/device/sensor",
        messageWithJson("""
            {"payload":{"temperature":20.0}}
            """)
    );

    ParsedMessage output = transformation.transform("/device/sensor", input);

    assertNotNull(output);
    assertJsonEquals("""
        {"payload":{"temperature":20.0}}
        """, jsonFromMessage(output.getMessage()));
  }

  @Test
  void invalidJsonLeavesMessageUnchanged() {
    JsonMutateTransformation transformation = new JsonMutateTransformation(
        new JsonMutator(List.of(
            buildRemove("$.payload.debug")
        ))
    );

    Message message = new MessageBuilder()
        .setOpaqueData("{not-json".getBytes(StandardCharsets.UTF_8))
        .build();

    ParsedMessage input = new ParsedMessage("/device/sensor", message);

    ParsedMessage output = transformation.transform("/device/sensor", input);

    assertNotNull(output);
    assertArrayEquals(
        input.getMessage().getOpaqueData(),
        output.getMessage().getOpaqueData()
    );
  }

  private static JsonMutateOpDTO buildSet(String path, Object value) {
    JsonMutateOpDTO op = new JsonMutateOpDTO();
    op.setOp(JsonMutateOperation.SET);
    op.setPath(path);
    op.setValue(gson.toJsonTree(value));
    return op;
  }

  private static JsonMutateOpDTO buildRemove(String path) {
    JsonMutateOpDTO op = new JsonMutateOpDTO();
    op.setOp(JsonMutateOperation.REMOVE);
    op.setPath(path);
    return op;
  }

  private static Message messageWithJson(String json) {
    return new MessageBuilder()
        .setOpaqueData(json.trim().getBytes(StandardCharsets.UTF_8))
        .build();
  }

  private static String jsonFromMessage(Message message) {
    return new String(message.getOpaqueData(), StandardCharsets.UTF_8);
  }

  private static void assertJsonEquals(String expectedJson, String actualJson) {
    JsonElement expected = JsonParser.parseString(expectedJson);
    JsonElement actual = JsonParser.parseString(actualJson);
    assertEquals(expected, actual);
  }
}
