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

package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class StringStatistics implements Statistics {

  private final Map<String, Long> counts = new HashMap<>();
  @Getter
  private long totalCount = 0;
  private int mismatched;

  public void reset() {
    counts.clear();
    totalCount = 0;
    mismatched = 0;
  }

  public void update(Object value) {
    String comp = null;

    if(value instanceof String str) {
      comp = str.trim();
    }
    if(value instanceof Number num){
      comp = num.toString();
    }
    if (comp == null) {
      return;
    }
    counts.merge(comp, 1L, Long::sum);
    totalCount++;

  }


  @Override
  public JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("totalCount", totalCount);
    JsonArray arr = new JsonArray();

    counts.entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .forEach(e -> {
          JsonObject item = new JsonObject();
          item.addProperty("value", e.getKey());
          item.addProperty("count", e.getValue());
          arr.add(item);
        });

    o.add("distribution", arr);
    o.addProperty("mismatched", mismatched);
    return o;
  }

  public long getCount(String key) {
    return counts.getOrDefault(key, 0L);
  }

  @Override
  public Statistics create() {
    return new StringStatistics();
  }

  @Override
  public void incrementMismatch() {
    mismatched++;
  }

  @Override
  public String getName() {
    return "String";
  }

  @Override
  public String getDescription() {
    return "String Statistics";
  }
}