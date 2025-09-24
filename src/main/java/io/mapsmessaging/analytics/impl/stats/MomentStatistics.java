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
 * Higher-order moments (skewness, kurtosis) using online updates.
 * Based on Pebay/Chan one-pass formulas.
 */
public class MomentStatistics extends AdvancedStatistics {

  protected double m3;
  protected double m4;

  public MomentStatistics() {
    this.m3 = 0.0;
    this.m4 = 0.0;
  }

  @Override
  public void reset() {
    super.reset();
    m3 = 0.0;
    m4 = 0.0;
  }

  @Override
  protected void update(double currentValue) {
    double previousMean = mean;
    double previousM2 = m2;
    double previousM3 = m3;
    double previousM4 = m4;

    super.update(currentValue); // updates mean, m2, count

    double n = count;
    double delta = currentValue - previousMean;
    double deltaN = delta / n;
    double deltaN2 = deltaN * deltaN;
    double term1 = delta * deltaN * (n - 1.0);

    m4 = previousM4
        + term1 * deltaN2 * (n * n - 3 * n + 3)
        + 6.0 * deltaN2 * previousM2
        + 4.0 * deltaN * previousM3;

    m3 = previousM3
        + term1 * deltaN * (n - 2.0)
        - 3.0 * deltaN * previousM2;
  }

  public double getSampleSkewness() {
    if (count < 3) {
      return 0.0;
    }
    double n = count;
    double s = Math.sqrt(m2 / (n - 1.0));
    if (s == 0.0) {
      return 0.0;
    }
    return (Math.sqrt(n * (n - 1.0)) / (n - 2.0)) * (m3 / Math.pow(m2, 1.5));
  }

  public double getSampleKurtosisExcess() {
    if (count < 4) {
      return 0.0;
    }
    double n = count;
    return ((n * (n + 1.0) * m4) / (m2 * m2 * (n - 1.0) * (n - 2.0) * (n - 3.0)))
        - (3.0 * (n - 1.0) * (n - 1.0) / ((n - 2.0) * (n - 3.0)));
  }

  @Override
  protected void addSubclassJson(JsonObject o) {
    super.addSubclassJson(o);
    o.addProperty("skewness", getSampleSkewness());
    o.addProperty("kurtosisExcess", getSampleKurtosisExcess());
  }

  @Override
  public Statistics create() {
    return new MomentStatistics();
  }

  @Override
  public String getName() {
    return "Moment";
  }

  @Override
  public String getDescription() {
    return "Moment Statistics";
  }
}
