package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;

/**
 * Timestamp-aware regression. Keeps separate sums for true-time linear fit.
 * Call update(timestampNanos, value) to enable timestamp-based slope.
 */
public class TrendStatistics extends AdvancedStatistics {

  protected long lastTimestampNanos;
  protected boolean hasTimestamps;

  protected double sumTimestamp;
  protected double sumTimestampSquared;
  protected double sumTimestampValueProduct;

  public TrendStatistics() {
    this.lastTimestampNanos = 0L;
    this.hasTimestamps = false;
    this.sumTimestamp = 0.0;
    this.sumTimestampSquared = 0.0;
    this.sumTimestampValueProduct = 0.0;
  }

  @Override
  public void reset() {
    super.reset();
    lastTimestampNanos = 0L;
    hasTimestamps = false;
    sumTimestamp = 0.0;
    sumTimestampSquared = 0.0;
    sumTimestampValueProduct = 0.0;
  }

  public void update(long timestampNanos, double currentValue) {
    hasTimestamps = true;
    super.update(currentValue);

    double t = timestampNanos * 1e-9; // seconds
    sumTimestamp += t;
    sumTimestampSquared += t * t;
    sumTimestampValueProduct += t * currentValue;
    lastTimestampNanos = timestampNanos;
  }

  public double getTimestampSlopePerSecond() {
    if (!hasTimestamps || count < 2) {
      return 0.0;
    }
    double denominator = count * sumTimestampSquared - (sumTimestamp * sumTimestamp);
    if (denominator == 0.0) {
      return 0.0;
    }
    return (count * sumTimestampValueProduct - sumTimestamp * sumValue) / denominator;
  }

  public double getTimestampIntercept() {
    if (!hasTimestamps || count < 2) {
      return 0.0;
    }
    double denominator = count * sumTimestampSquared - (sumTimestamp * sumTimestamp);
    if (denominator == 0.0) {
      return 0.0;
    }
    return (sumValue * sumTimestampSquared - sumTimestamp * sumTimestampValueProduct) / denominator;
  }

  @Override
  protected void addSubclassJson(JsonObject o) {
    super.addSubclassJson(o);
    o.addProperty("timestampSlopePerSec", getTimestampSlopePerSecond());
    o.addProperty("timestampIntercept", getTimestampIntercept());
  }

}
