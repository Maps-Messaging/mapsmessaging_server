package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;

import java.util.Arrays;

/**
 * Streaming quantiles using the P² algorithm (fixed set of quantiles).
 * No external dependencies. Tracks median and selected upper quantiles.
 */
public class QuantileStatistics extends AdvancedStatistics {

  private final P2Quantile[] estimators;

  public QuantileStatistics(double... quantiles) {
    if (quantiles == null || quantiles.length == 0) {
      this.estimators = new P2Quantile[] {
          new P2Quantile(0.50),
          new P2Quantile(0.90),
          new P2Quantile(0.95),
          new P2Quantile(0.99)
      };
    } else {
      this.estimators = Arrays.stream(quantiles).mapToObj(P2Quantile::new).toArray(P2Quantile[]::new);
    }
  }

  @Override
  public void reset() {
    super.reset();
    for (P2Quantile p : estimators) {
      p.reset();
    }
  }

  @Override
  protected void update(double currentValue) {
    super.update(currentValue);
    for (P2Quantile p : estimators) {
      p.add(currentValue);
    }
  }

  public double getQuantile(double q) {
    for (P2Quantile p : estimators) {
      if (p.quantile == q) {
        return p.value();
      }
    }
    return Double.NaN;
  }

  public double getMedian() {
    return getQuantile(0.50);
  }

  public double getP90() {
    return getQuantile(0.90);
  }

  public double getP95() {
    return getQuantile(0.95);
  }

  public double getP99() {
    return getQuantile(0.99);
  }

  private static final class P2Quantile {
    private final double quantile;

    // Marker positions and heights
    private final double[] positions;
    private final double[] desired;
    private final double[] increments;
    private final double[] heights;
    private int count;

    P2Quantile(double quantile) {
      this.quantile = Math.max(0.0, Math.min(1.0, quantile));
      this.positions = new double[5];
      this.desired = new double[5];
      this.increments = new double[] {0.0, this.quantile / 2.0, this.quantile, (1.0 + this.quantile) / 2.0, 1.0};
      this.heights = new double[5];
      this.count = 0;
    }

    void reset() {
      Arrays.fill(positions, 0.0);
      Arrays.fill(desired, 0.0);
      Arrays.fill(heights, 0.0);
      count = 0;
    }

    void add(double x) {
      if (count < 5) {
        heights[count] = x;
        count++;
        if (count == 5) {
          Arrays.sort(heights, 0, 5);
          for (int i = 0; i < 5; i++) {
            positions[i] = i + 1;
          }
          desired[0] = 1;
          desired[1] = 1 + 2 * quantile;
          desired[2] = 1 + 4 * quantile;
          desired[3] = 3 + 2 * quantile;
          desired[4] = 5;
        }
        return;
      }

      int k;
      if (x < heights[0]) {
        heights[0] = x;
        k = 0;
      } else if (x < heights[1]) {
        k = 0;
      } else if (x < heights[2]) {
        k = 1;
      } else if (x < heights[3]) {
        k = 2;
      } else if (x <= heights[4]) {
        k = 3;
      } else {
        heights[4] = x;
        k = 3;
      }

      for (int i = k + 1; i < 5; i++) {
        positions[i] += 1.0;
      }
      for (int i = 0; i < 5; i++) {
        desired[i] += increments[i];
      }

      adjust(1);
      adjust(2);
      adjust(3);
    }

    private void adjust(int i) {
      double d = desired[i] - positions[i];
      int s = (int) Math.signum(d);
      if (s != 0 && positions[i + s] - positions[i - 1 + s] > 1.0) {
        double p = positions[i] - positions[i - 1];
        double q = positions[i + 1] - positions[i];
        double r = heights[i] - heights[i - 1];
        double t = heights[i + 1] - heights[i];
        double hp = heights[i] + s * (p * t / (positions[i + 1] - positions[i - 1]) + q * r / (positions[i + 1] - positions[i - 1]));
        if (hp > heights[i - 1] && hp < heights[i + 1]) {
          heights[i] = hp;
        } else {
          heights[i] = heights[i] + s * (heights[i + s] - heights[i]) / (positions[i + s] - positions[i]);
        }
        positions[i] += s;
      }
    }

    double value() {
      if (count < 5) {
        if (count == 0) {
          return Double.NaN;
        }
        double[] copy = Arrays.copyOf(heights, count);
        Arrays.sort(copy);
        int mid = (count - 1) / 2;
        if ((count & 1) == 1) {
          return copy[mid];
        } else {
          return 0.5 * (copy[mid] + copy[mid + 1]);
        }
      }
      return heights[2];
    }
  }
  @Override
  protected void addSubclassJson(JsonObject o) {
    super.addSubclassJson(o);
    o.addProperty("median", getMedian());
    o.addProperty("p90", getP90());
    o.addProperty("p95", getP95());
    o.addProperty("p99", getP99());
  }

}
