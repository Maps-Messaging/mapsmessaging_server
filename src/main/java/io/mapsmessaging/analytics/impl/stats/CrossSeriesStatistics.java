package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Cross-series stats: online covariance/correlation with a paired series.
 * Feed pairs via updatePair(x, y); you may also call update(x) for base stats.
 */
public class CrossSeriesStatistics extends AdvancedStatistics {

  protected long pairedCount;

  protected double sumX;
  protected double sumY;
  protected double sumXX;
  protected double sumYY;
  protected double sumXY;

  public CrossSeriesStatistics() {
    this.pairedCount = 0L;
    this.sumX = 0.0;
    this.sumY = 0.0;
    this.sumXX = 0.0;
    this.sumYY = 0.0;
    this.sumXY = 0.0;
  }

  @Override
  public void reset() {
    super.reset();
    pairedCount = 0L;
    sumX = 0.0;
    sumY = 0.0;
    sumXX = 0.0;
    sumYY = 0.0;
    sumXY = 0.0;
  }

  public void updatePair(double x, double y) {
    // Optional: also track single-series stats on x
    update(x);

    pairedCount++;
    sumX += x;
    sumY += y;
    sumXX += x * x;
    sumYY += y * y;
    sumXY += x * y;
  }

  public double getSampleCovariance() {
    if (pairedCount < 2) {
      return 0.0;
    }
    double n = pairedCount;
    return (sumXY - (sumX * sumY) / n) / (n - 1.0);
  }

  public double getSampleCorrelation() {
    if (pairedCount < 2) {
      return 0.0;
    }
    double n = pairedCount;
    double varX = (sumXX - (sumX * sumX) / n) / (n - 1.0);
    double varY = (sumYY - (sumY * sumY) / n) / (n - 1.0);
    if (varX <= 0.0 || varY <= 0.0) {
      return 0.0;
    }
    double cov = getSampleCovariance();
    return cov / Math.sqrt(varX * varY);
  }

  @Override
  protected void addSubclassJson(JsonObject o) {
    super.addSubclassJson(o);
    o.addProperty("pairedCount", pairedCount);
    o.addProperty("covariance", getSampleCovariance());
    o.addProperty("correlation", getSampleCorrelation());
  }

}
