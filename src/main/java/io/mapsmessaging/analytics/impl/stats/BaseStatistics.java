package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

public class BaseStatistics implements Statistics {
  protected double first;
  protected double last;
  protected double min;
  protected double max;
  protected double average;
  protected int count;

  protected int mismatched;

  // --- time tracking ---
  protected long firstUpdateMillis;
  protected long lastUpdateMillis;

  public BaseStatistics() {
    reset();
  }

  public void reset() {
    first = Double.NaN;
    last = Double.NaN;
    min = Double.MAX_VALUE;
    max = Double.MIN_VALUE;
    average = 0.0;
    count = 0;
    mismatched = 0;
    firstUpdateMillis = 0L;
    lastUpdateMillis = 0L;
  }

  public void incrementMismatch() {
    mismatched++;
  }

  public void update(Object entry) {
    if (entry instanceof Number number) {
      update(number.doubleValue());
    }
  }

  protected void update(double cur) {
    long now = System.currentTimeMillis();
    if (count == 0) {
      first = cur;
      firstUpdateMillis = now;
    }
    if (min > cur) {
      min = cur;
    }
    if (max < cur || max == Double.MIN_VALUE) {
      max = cur;
    }
    count++;
    average = ((average * (count - 1)) + cur) / count;
    last = cur;
    lastUpdateMillis = now;
  }

  public JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("first", first);
    o.addProperty("last", last);
    o.addProperty("min", min);
    o.addProperty("max", max);
    o.addProperty("average", average);
    o.addProperty("count", count);
    o.addProperty("range", max - min);
    o.addProperty("delta", last - first);
    o.addProperty("mismatched", mismatched);

    // time-based stats
    o.addProperty("firstUpdateMillis", firstUpdateMillis);
    o.addProperty("lastUpdateMillis", lastUpdateMillis);
    if (firstUpdateMillis > 0 && lastUpdateMillis > firstUpdateMillis) {
      long duration = lastUpdateMillis - firstUpdateMillis;
      o.addProperty("durationMillis", duration);
      double ratePerSec = (count * 1000.0) / duration;
      o.addProperty("ratePerSec", ratePerSec);
    }

    addSubclassJson(o);
    return o;
  }

  protected void addSubclassJson(JsonObject o) {
    // default: nothing
  }

  @Override
  public Statistics create() {
    return new BaseStatistics();
  }

  @Override
  public String getName() {
    return "Base";
  }

  @Override
  public String getDescription() {
    return "Base statistics";
  }
}
