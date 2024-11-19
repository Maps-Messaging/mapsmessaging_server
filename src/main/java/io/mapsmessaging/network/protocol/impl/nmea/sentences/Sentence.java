/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.nmea.sentences;

import io.mapsmessaging.network.protocol.impl.nmea.types.Type;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class Sentence {

  private final String name;
  private final String description;
  private final Map<String, Type> values;
  private final List<String> order;

  public Sentence(String name, String description, List<String> order, Map<String, Type> values) {
    this.name = name;
    this.description = description;
    this.values = values;
    this.order = order;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Type get(String key) {
    return values.get(key);
  }

  public String toJSON() {
    JSONObject response = new JSONObject();
    response.put(name, packObject());
    response.put("description", description);
    return response.toString(4);
  }

  private JSONObject packObject() {
    JSONObject jsonObject = new JSONObject();
    values.keySet().forEach(key -> {
      Type type = values.get(key);
      jsonObject.put(key, type.jsonPack());
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
