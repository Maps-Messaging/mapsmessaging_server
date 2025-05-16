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

package io.mapsmessaging.network.protocol.impl.nmea.sentences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nmea.types.Type;
import lombok.Getter;

import java.util.List;
import java.util.Map;

public class Sentence {

  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Getter
  private final String name;
  @Getter
  private final String description;

  private final Map<String, Type> values;
  private final List<String> order;

  public Sentence(String name, String description, List<String> order, Map<String, Type> values) {
    this.name = name;
    this.description = description;
    this.values = values;
    this.order = order;
  }

  public Type get(String key) {
    return values.get(key);
  }

  public String toJSON() {
    JsonObject response = new JsonObject();
    response.add(name, packObject());
    response.addProperty("description", description);
    return gson.toJson(response);
  }

  private JsonObject packObject() {
    JsonObject jsonObject = new JsonObject();
    values.keySet().forEach(key -> {
      Type type = values.get(key);
      Object packed = type.jsonPack();
      jsonObject.add(key, gson.toJsonTree(packed));
    });
    return jsonObject;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("$").append(name).append(",");
    for (int x = 0; x < order.size(); x++) {
      sb.append(values.get(order.get(x)));
      if (x != order.size() - 1) {
        sb.append(",");
      }
    }
    return sb.toString();
  }

}
