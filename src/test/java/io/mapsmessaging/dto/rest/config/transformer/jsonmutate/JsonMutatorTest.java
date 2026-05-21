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

package io.mapsmessaging.dto.rest.config.transformer.jsonmutate;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;

import io.mapsmessaging.api.transformers.jsonmutate.JsonMutator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonMutatorTest {

  @Test
  void setCreatesPathAndSetsValue() {
    JsonObject root = new JsonObject();

    JsonMutateOpDTO op = new JsonMutateOpDTO();
    op.setOp(JsonMutateOperation.SET);
    op.setPath("payload.temperature");
    op.setValue(new JsonPrimitive(23.5));

    JsonMutator mutator = new JsonMutator(List.of(op));
    JsonObject mutated = mutator.apply(root);

    assertNotNull(mutated.getAsJsonObject("payload"));
    assertEquals(23.5, mutated.getAsJsonObject("payload").get("temperature").getAsDouble(), 0.00001);
  }

  @Test
  void removeDeletesFieldWhenPresent() {
    JsonObject root = new JsonObject();
    JsonObject payload = new JsonObject();
    payload.add("debug", new JsonPrimitive(true));
    root.add("payload", payload);

    JsonMutateOpDTO op = new JsonMutateOpDTO();
    op.setOp(JsonMutateOperation.REMOVE);
    op.setPath("payload.debug");

    JsonMutator mutator = new JsonMutator(List.of(op));
    JsonObject mutated = mutator.apply(root);

    assertFalse(mutated.getAsJsonObject("payload").has("debug"));
  }

  @Test
  void renameMovesValue() {
    JsonObject root = new JsonObject();
    JsonObject payload = new JsonObject();
    payload.add("tempC", new JsonPrimitive(21));
    root.add("payload", payload);

    JsonMutateOpDTO op = new JsonMutateOpDTO();
    op.setOp(JsonMutateOperation.RENAME);
    op.setFrom("payload.tempC");
    op.setTo("payload.temperatureC");

    JsonMutator mutator = new JsonMutator(List.of(op));
    JsonObject mutated = mutator.apply(root);

    assertFalse(mutated.getAsJsonObject("payload").has("tempC"));
    assertEquals(21, mutated.getAsJsonObject("payload").get("temperatureC").getAsInt());
  }

  @Test
  void removeMissingPathIsNoOp() {
    JsonObject root = new JsonObject();

    JsonMutateOpDTO op = new JsonMutateOpDTO();
    op.setOp(JsonMutateOperation.REMOVE);
    op.setPath("payload.missing");

    JsonMutator mutator = new JsonMutator(List.of(op));
    JsonObject mutated = mutator.apply(root);

    assertEquals(root, mutated);
  }

  @Test
  void setArrayIndexCreatesArray() {
    JsonObject root = new JsonObject();

    JsonMutateOpDTO op = new JsonMutateOpDTO();
    op.setOp(JsonMutateOperation.SET);
    op.setPath("payload.items[1].name");
    op.setValue(new JsonPrimitive("x"));

    JsonMutator mutator = new JsonMutator(List.of(op));
    JsonObject mutated = mutator.apply(root);

    assertTrue(mutated.getAsJsonObject("payload").get("items").isJsonArray());
    assertTrue(mutated.getAsJsonObject("payload").getAsJsonArray("items").get(0) instanceof JsonNull);
    assertEquals(
        "x",
        mutated.getAsJsonObject("payload")
            .getAsJsonArray("items")
            .get(1)
            .getAsJsonObject()
            .get("name")
            .getAsString()
    );
  }
}
