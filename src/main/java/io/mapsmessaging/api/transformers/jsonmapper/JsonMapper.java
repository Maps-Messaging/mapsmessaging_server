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

package io.mapsmessaging.api.transformers.jsonmapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.transformers.jsonmutate.JsonPath;
import io.mapsmessaging.dto.rest.config.transformer.jsonmapper.JsonMapOpDTO;


import java.util.List;

public class JsonMapper {

  private final List<JsonMapOpDTO> operations;

  public JsonMapper(List<JsonMapOpDTO> operations) {
    this.operations = operations;
  }

  public JsonObject apply(JsonObject source) {
    JsonObject target = new JsonObject();
    return apply(source, target);
  }

  public JsonObject apply(JsonObject source, JsonObject target) {
    if (source == null) {
      return target;
    }
    if (target == null) {
      target = new JsonObject();
    }
    if (operations == null || operations.isEmpty()) {
      return target;
    }

    for (JsonMapOpDTO operation : operations) {
      if (operation == null) {
        continue;
      }
      if (operation.getFrom() == null || operation.getFrom().isBlank()) {
        continue;
      }
      if (operation.getTo() == null || operation.getTo().isBlank()) {
        continue;
      }

      JsonElement value = JsonPath.get(source, operation.getFrom());

      if (value == null || value.isJsonNull()) {
        value = operation.getDefaultValue();
      }

      if ((value == null || value.isJsonNull()) && operation.isIgnoreMissing()) {
        continue;
      }

      value = JsonMapFunctions.apply(
          operation.getFunction(),
          value
      );

      if (value != null) {
        JsonPath.set(target, operation.getTo(), value);
      }
    }

    return target;
  }
}