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

  @Override
  public void update(Object entry) {
    if (entry == null) {
      return;
    }
    if (!(entry instanceof Number number)) {
      mismatched++;
      return;
    }
    double value = number.doubleValue();
    if (!Double.isFinite(value)) {
      mismatched++;
      return;
    }
    update(System.nanoTime(), value);
  }

  @Override
  protected void update(double currentValue) {
    update(System.nanoTime(), currentValue);
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
  @Override
  public Statistics create() {
    return new TrendStatistics();
  }

  @Override
  public String getName() {
    return "Trend";
  }

  @Override
  public String getDescription() {
    return "Trend Statistics";
  }
}
