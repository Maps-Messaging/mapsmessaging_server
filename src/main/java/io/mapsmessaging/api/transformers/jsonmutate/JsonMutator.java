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

package io.mapsmessaging.api.transformers.jsonmutate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.dto.rest.config.transformer.jsonmutate.JsonMutateOpDTO;
import io.mapsmessaging.dto.rest.config.transformer.jsonmutate.JsonMutateOperation;

import java.util.List;

public class JsonMutator {

  private final List<JsonMutateOpDTO> operations;

  public JsonMutator(List<JsonMutateOpDTO> operations) {
    this.operations = operations;
  }

  public JsonObject apply(JsonObject input) {
    if (input == null || operations == null || operations.isEmpty()) {
      return input;
    }

    for (JsonMutateOpDTO operation : operations) {
      if (operation == null || operation.getOp() == null) {
        continue;
      }

      JsonMutateOperation op = operation.getOp();
      if (op == JsonMutateOperation.SET) {
        applySet(input, operation.getPath(), operation.getValue());
      } else if (op == JsonMutateOperation.REMOVE) {
        applyRemove(input, operation.getPath());
      } else if (op == JsonMutateOperation.RENAME) {
        applyRename(input, operation.getFrom(), operation.getTo());
      }
    }

    return input;
  }

  private void applySet(JsonObject root, String path, JsonElement value) {
    if (path == null || path.isBlank() || value == null) {
      return;
    }
    JsonPath.set(root, path, value);
  }

  private void applyRemove(JsonObject root, String path) {
    if (path == null || path.isBlank()) {
      return;
    }
    JsonPath.remove(root, path);
  }

  private void applyRename(JsonObject root, String from, String to) {
    if (from == null || from.isBlank() || to == null || to.isBlank()) {
      return;
    }
    JsonElement extracted = JsonPath.get(root, from);
    if (extracted == null) {
      return;
    }
    JsonPath.remove(root, from);
    JsonPath.set(root, to, extracted);
  }
}
