package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class StringStatistics implements Statistics {

  private final Map<String, Long> counts = new HashMap<>();
  private long totalCount = 0;

  public void reset() {
    counts.clear();
    totalCount = 0;
  }
  public void update(String value) {
    if (value == null) {
      return;
    }
    counts.merge(value, 1L, Long::sum);
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
    return o;
  }
}