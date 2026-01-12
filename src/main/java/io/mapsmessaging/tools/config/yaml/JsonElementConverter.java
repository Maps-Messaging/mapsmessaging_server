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


import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonElementConverter {

  private JsonElementConverter() {
  }

  public static Object toJava(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return null;
    }

    if (element.isJsonPrimitive()) {
      if (element.getAsJsonPrimitive().isBoolean()) {
        return element.getAsBoolean();
      }
      if (element.getAsJsonPrimitive().isNumber()) {
        return element.getAsNumber();
      }
      return element.getAsString();
    }

    if (element.isJsonArray()) {
      List<Object> list = new ArrayList<>();
      for (JsonElement item : element.getAsJsonArray()) {
        list.add(toJava(item));
      }
      return list;
    }

    if (element.isJsonObject()) {
      Map<String, Object> map = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
        map.put(entry.getKey(), toJava(entry.getValue()));
      }
      return map;
    }

    return element.toString();
  }
}