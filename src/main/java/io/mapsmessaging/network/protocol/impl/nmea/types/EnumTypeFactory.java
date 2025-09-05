/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.nmea.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class EnumTypeFactory {

  private static class Holder {
    static final EnumTypeFactory INSTANCE = new EnumTypeFactory();
  }
  public static EnumTypeFactory getInstance() {
    return Holder.INSTANCE;
  }

  private final Map<String, Map<String, String>> configuration;

  public synchronized void register(String name, String jsonObjectOptions) {
    JsonArray jsonArray = JsonParser.parseString(jsonObjectOptions).getAsJsonArray();
    Map<String, String> map = new LinkedHashMap<>();

    for (JsonElement element : jsonArray) {
      JsonObject jsonObject = element.getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
        map.put(entry.getKey(), entry.getValue().getAsString());
      }
    }
    configuration.put(name, map);
  }


  public synchronized EnumType getEnum(String name, String id) {
    Map<String, String> map = configuration.get(name);
    if (map != null) {
      String desc = map.get(id);
      return new EnumType(id, desc);
    }
    return null;
  }

  private EnumTypeFactory() {
    configuration = new LinkedHashMap<>();
  }

}
