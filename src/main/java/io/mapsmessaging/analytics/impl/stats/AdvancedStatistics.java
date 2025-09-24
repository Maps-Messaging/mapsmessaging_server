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

public class AdvancedStatistics extends BaseStatistics {

  protected double m2;
  protected double mean;

  protected double sumTime;
  protected double sumValue;
  protected double sumTimeSquared;
  protected double sumTimeValueProduct;

  public AdvancedStatistics() {
    //required for services
  }

  @Override
  public void reset() {
    super.reset();
    m2 = 0.0;
    mean = 0.0;
    sumTime = 0.0;
    sumValue = 0.0;
    sumTimeSquared = 0.0;
    sumTimeValueProduct = 0.0;
  }

  public double getStdDeviation() {
    if (count < 2) {
      return 0.0;
    }
    return Math.sqrt(m2 / (count - 1));
  }

  public double getSlope() {
    if (count < 2) {
      return 0.0;
    }
    double denominator = count * sumTimeSquared - (sumTime * sumTime);
    if (denominator == 0.0) {
      return 0.0;
    }
    return (count * sumTimeValueProduct - sumTime * sumValue) / denominator;
  }

  public double getIntercept() {
    if (count < 2) {
      return 0.0;
    }
    double denominator = count * sumTimeSquared - (sumTime * sumTime);
    if (denominator == 0.0) {
      return 0.0;
    }
    return (sumValue * sumTimeSquared - sumTime * sumTimeValueProduct) / denominator;
  }

  @Override
  protected void addSubclassJson(JsonObject o) {
    o.addProperty("stdDev", getStdDeviation());
    o.addProperty("slope", getSlope());
    o.addProperty("intercept", getIntercept());
  }

  @Override
  public String toString() {
    return super.toString() + ", std dev=" + getStdDeviation() + ", slope=" + getSlope()+", intercept=" + getIntercept();
  }

  @Override
  protected void update(double currentValue) {
    super.update(currentValue);

    // Welford’s algorithm for mean and variance
    double delta = currentValue - mean;
    mean += delta / count;
    double delta2 = currentValue - mean;
    m2 += delta * delta2;

    // Incremental linear regression over sample index as time (t = count)
    double time = count;
    sumTime += time;
    sumValue += currentValue;
    sumTimeSquared += time * time;
    sumTimeValueProduct += time * currentValue;
  }

  @Override
  public Statistics create() {
    return new AdvancedStatistics();
  }

  @Override
  public String getName() {
    return "Advanced";
  }

  @Override
  public String getDescription() {
    return "Advanced Statistics";
  }

}
