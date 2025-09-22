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

  public void reset() {
    counts.clear();
    totalCount = 0;
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
    return o;
  }

  public long getCount(String key) {
    return counts.get(key);
  }

  @Override
  public Statistics create() {
    return new StringStatistics();
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