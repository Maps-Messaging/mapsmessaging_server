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
import lombok.Getter;

/**
 * Data quality and outlier tracking.
 * Outlier threshold uses mean ± k * std deviation (sample).
 */
@Getter
public class QualityStatistics extends AdvancedStatistics {

  protected long missingCount;
  protected long nanCount;
  protected long infiniteCount;
  protected long outlierCount;

  protected double outlierStdDevs;

  public QualityStatistics() {
    this.missingCount = 0L;
    this.nanCount = 0L;
    this.infiniteCount = 0L;
    this.outlierCount = 0L;
    this.outlierStdDevs = 3.0;
  }

  @Override
  public void reset() {
    super.reset();
    missingCount = 0L;
    nanCount = 0L;
    infiniteCount = 0L;
    outlierCount = 0L;
  }

  public void setOutlierStdDevs(double stdDevs) {
    this.outlierStdDevs = Math.max(0.0, stdDevs);
  }

  public void updateMissing() {
    missingCount++;
  }

  @Override
  protected void update(double currentValue) {
    if (Double.isNaN(currentValue)) {
      nanCount++;
      return;
    }
    if (!Double.isFinite(currentValue)) {
      infiniteCount++;
      return;
    }

    super.update(currentValue);

    if (count >= 2) {
      double std = getStdDeviation();
      double threshold = outlierStdDevs * std;
      if (threshold > 0.0 && Math.abs(currentValue - mean) > threshold) {
        outlierCount++;
      }
    }
  }

  @Override
  protected void addSubclassJson(JsonObject o) {
    super.addSubclassJson(o);
    o.addProperty("missing", getMissingCount());
    o.addProperty("nan", getNanCount());
    o.addProperty("infinite", getInfiniteCount());
    o.addProperty("outliers", getOutlierCount());
    o.addProperty("outlierStdDevs", outlierStdDevs);
  }
  @Override
  public Statistics create() {
    return new QualityStatistics();
  }

  @Override
  public String getName() {
    return "Quality";
  }

  @Override
  public String getDescription() {
    return "Quality statistics";
  }
}
