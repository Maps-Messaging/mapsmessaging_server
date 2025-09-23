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

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class StatisticsTests {

  // ---------- Helpers ----------
  private static void feedSequential(AdvancedStatistics s, int n) {
    for (int i = 0; i < n; i++) s.update(i);
  }

  private static void assertJsonHas(JsonObject o, String... keys) {
    for (String k : keys) assertTrue(o.has(k), "missing key: " + k);
  }

  // ---------- AdvancedStatistics ----------
  @Test
  void advanced_basicAndRegression() {
    AdvancedStatistics stats =(AdvancedStatistics) StatisticsFactory.getInstance().getAnalyser("Advanced");
    feedSequential(stats, 1000);

    assertEquals(0.0, stats.first, 0.0);
    assertEquals(999.0, stats.last, 0.0);
    assertEquals(0.0, stats.min, 0.0);
    assertEquals(999.0, stats.max, 0.0);
    assertEquals(499.5, stats.average, 1e-12);
    assertEquals(1000, stats.count);

    assertEquals(288.8194360957494, stats.getStdDeviation(), 1e-9);
    assertEquals(1.0, stats.getSlope(), 1e-12);
    assertEquals(-1.0, stats.getIntercept(), 1e-12);

    JsonObject json = stats.toJson();
    assertJsonHas(json, "first","last","min","max","average","count","range","delta","stdDev","slope","intercept");
    assertEquals(499.5, json.get("average").getAsDouble(), 1e-12);
  }

  // ---------- WindowedStatistics ----------
  @Test
  void windowed_lastTen() {
    WindowedStatistics stats = (WindowedStatistics) StatisticsFactory.getInstance().getAnalyser("Windowed");
    feedSequential(stats, 1000);

    assertEquals(10, stats.getWindowCount());
    assertEquals(994.5, stats.getWindowMean(), 1e-12);
    assertEquals(3.0276503540974917, stats.getWindowStdDeviation(), 1e-12);

    JsonObject json = stats.toJson();
    assertJsonHas(json, "windowSize","windowCount","windowMean","windowStdDev");
    assertEquals(10, json.get("windowSize").getAsInt());
    assertEquals(10, json.get("windowCount").getAsInt());
  }

  // ---------- QuantileStatistics ----------
  @Test
  void quantiles_p2Approx() {
    QuantileStatistics q = (QuantileStatistics) StatisticsFactory.getInstance().getAnalyser("Quantiles"); ;
    for (int i = 1; i <= 10000; i++) q.update(i);

    assertEquals(5000.5, q.getMedian(), 10.0);
    assertEquals(9000.5, q.getP90(), 20.0);
    assertEquals(9500.5, q.getP95(), 20.0);
    assertEquals(9900.5, q.getP99(), 30.0);

    JsonObject json = q.toJson();
    assertJsonHas(json, "median","p90","p95","p99");
  }

  // ---------- TrendStatistics (timestamps) ----------
  @Test
  void trend_timestampSlope() {
    TrendStatistics t = (TrendStatistics) StatisticsFactory.getInstance().getAnalyser("Trend");
    for (int i = 0; i < 100; i++) {
      long ts = i * 1_000_000_000L; // seconds in nanos
      double v = 2.0 * i + 5.0;
      t.update(ts, v);
    }
    assertEquals(2.0, t.getTimestampSlopePerSecond(), 1e-9);
    assertEquals(5.0, t.getTimestampIntercept(), 1e-9);

    JsonObject json = t.toJson();
    assertJsonHas(json, "timestampSlopePerSec","timestampIntercept");
  }

  // ---------- MomentStatistics (skew/kurt) ----------
  @Test
  void moments_skewKurt() {
    MomentStatistics m = (MomentStatistics) StatisticsFactory.getInstance().getAnalyser("Moment");
    for (int i = 0; i < 10000; i++) m.update(i);

    assertEquals(0.0, m.getSampleSkewness(), 1e-3);
    assertTrue(m.getSampleKurtosisExcess() < 0.0);

    JsonObject json = m.toJson();
    assertJsonHas(json, "skewness","kurtosisExcess");
  }

  // ---------- QualityStatistics ----------
  @Test
  void quality_countsOutliers() {
    QualityStatistics qs = (QualityStatistics) StatisticsFactory.getInstance().getAnalyser("Quality");
    qs.updateMissing();
    qs.update(Double.NaN);
    qs.update(Double.POSITIVE_INFINITY);

    for (int i = 0; i < 100; i++) qs.update(100.0 + (i % 5) - 2.0); // 98..102 cluster
    long baseOutliers = qs.getOutlierCount();

    qs.update(1000.0);
    qs.update(-800.0);

    assertEquals(1, qs.getMissingCount());
    assertEquals(1, qs.getNanCount());
    assertEquals(1, qs.getInfiniteCount());
    assertTrue(qs.getOutlierCount() >= baseOutliers + 2);

    JsonObject json = qs.toJson();
    assertJsonHas(json, "missing","nan","infinite","outliers","outlierStdDevs");
  }

  // ---------- CrossSeriesStatistics ----------
  @Test
  void cross_covCorr() {
    CrossSeriesStatistics c = (CrossSeriesStatistics) StatisticsFactory.getInstance().getAnalyser("CrossSeries");
    for (int i = 0; i < 1000; i++) {
      double x = i;
      double y = 2.0 * i + 5.0 + ((i & 1) == 0 ? 0.01 : -0.01);
      c.updatePair(x, y);
    }
    assertEquals(1000, c.getPairedCount());
    assertTrue(c.getSampleCovariance() > 0.0);
    assertTrue(c.getSampleCorrelation() > 0.999);

    JsonObject json = c.toJson();
    assertJsonHas(json, "pairedCount","covariance","correlation");
  }

  // ---------- StringStatistics ----------
  @Test
  void strings_sortedDistribution() {
    StringStatistics ss = (StringStatistics) StatisticsFactory.getInstance().getAnalyser("String");
    ss.update("ON");
    ss.update("OFF");
    ss.update("ON");
    ss.update("IDLE");
    ss.update("ON");

    assertEquals(3, ss.getCount("ON"));
    assertEquals(1, ss.getCount("OFF"));
    assertEquals(1, ss.getCount("IDLE"));
    assertEquals(5, ss.getTotalCount());


    JsonObject json = ss.toJson();
    assertJsonHas(json, "totalCount","distribution");
    JsonArray dist = json.getAsJsonArray("distribution");
    assertEquals(3, dist.size());
    // Highest first
    assertEquals("ON", dist.get(0).getAsJsonObject().get("value").getAsString());
    assertEquals(3, dist.get(0).getAsJsonObject().get("count").getAsLong());
  }
}
