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
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.dto.rest.config.transformer.jsonmapper.JsonMapFunction;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Instant;

public class JsonMapFunctions {

  private JsonMapFunctions() {
  }

  public static JsonElement apply(JsonMapFunction function, JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return JsonNull.INSTANCE;
    }
    if (function == null || function == JsonMapFunction.NONE) {
      return value.deepCopy();
    }

    return switch (function) {
      case NONE -> value.deepCopy();
      case TO_STRING -> new JsonPrimitive(asString(value));
      case TO_INT -> new JsonPrimitive(asInt(value));
      case TO_LONG -> new JsonPrimitive(asLong(value));
      case TO_FLOAT -> new JsonPrimitive(asFloat(value));
      case TO_DOUBLE -> new JsonPrimitive(asDouble(value));
      case TO_BOOLEAN -> new JsonPrimitive(asBoolean(value));
      case TO_EPOCH_SECONDS -> new JsonPrimitive(asLong(value) / 1000);
      case DATE_TO_EPOCH_SECONDS -> new JsonPrimitive(Instant.parse(asString(value)).getEpochSecond());
      case BASE64_ENCODE -> new JsonPrimitive(
          Base64.getEncoder().encodeToString(asString(value).getBytes(StandardCharsets.UTF_8))
      );
      case BASE64_DECODE -> new JsonPrimitive(
          new String(Base64.getDecoder().decode(asString(value)), StandardCharsets.UTF_8)
      );
    };
  }

  private static String asString(JsonElement value) {
    if (value.isJsonPrimitive()) {
      return value.getAsString();
    }
    return value.toString();
  }

  private static int asInt(JsonElement value) {
    return value.getAsInt();
  }

  private static long asLong(JsonElement value) {
    return value.getAsLong();
  }

  private static float asFloat(JsonElement value) {
    return value.getAsFloat();
  }

  private static double asDouble(JsonElement value) {
    return value.getAsDouble();
  }

  private static boolean asBoolean(JsonElement value) {
    return value.getAsBoolean();
  }
}