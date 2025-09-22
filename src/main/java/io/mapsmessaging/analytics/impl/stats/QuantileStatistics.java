package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;

import java.util.Arrays;

/**
 * Streaming quantiles using the P² algorithm (fixed set of quantiles).
 * No external dependencies. Tracks median and selected upper quantiles.
 */
public class QuantileStatistics extends AdvancedStatistics {

  private final P2Quantile[] estimators;

  public QuantileStatistics() {
    this.estimators = new P2Quantile[] {
        new P2Quantile(0.50),
        new P2Quantile(0.90),
        new P2Quantile(0.95),
        new P2Quantile(0.99)
    };
  }

  @Override
  public void reset() {
    super.reset();
    if(estimators != null) {
      for (P2Quantile p : estimators) {
        if (p != null) {
          p.reset();
        }
      }
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
    P2Quantile best = null;
    double bestDiff = Double.POSITIVE_INFINITY;
    for (P2Quantile est : estimators) {
      double d = Math.abs(est.p - q);   // was est.quantile
      if (d < bestDiff) { bestDiff = d; best = est; }
    }
    return (best != null && bestDiff <= 1e-9) ? best.value() : Double.NaN;
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
    private final double p;

    // Marker heights (q1..q5), positions (n1..n5), desired positions (np), and increments (dn)
    private final double[] q = new double[5];
    private final double[] n = new double[5];
    private final double[] np = new double[5];
    private final double[] dn = new double[5];
    private int m = 0; // number of samples seen so far (<=5 during init)

    P2Quantile(double quantile) {
      this.p = Math.max(0.0, Math.min(1.0, quantile));
    }

    void reset() {
      Arrays.fill(q, 0.0);
      Arrays.fill(n, 0.0);
      Arrays.fill(np, 0.0);
      Arrays.fill(dn, 0.0);
      m = 0;
    }

    void add(double x) {
      // Initialization: collect first 5 samples
      if (m < 5) {
        q[m++] = x;
        if (m == 5) {
          Arrays.sort(q, 0, 5);
          // Initial positions
          n[0] = 1; n[1] = 2; n[2] = 3; n[3] = 4; n[4] = 5;
          // Desired positions
          np[0] = 1;
          np[1] = 1 + 2 * p;
          np[2] = 1 + 4 * p;
          np[3] = 3 + 2 * p;
          np[4] = 5;
          // Increments per observation
          dn[0] = 0.0;
          dn[1] = p / 2.0;
          dn[2] = p;
          dn[3] = (1.0 + p) / 2.0;
          dn[4] = 1.0;
        }
        return;
      }

      // Find cell k and clamp extremes
      int k;
      if (x < q[0]) {
        q[0] = x;
        k = 0;
      } else if (x < q[1]) {
        k = 0;
      } else if (x < q[2]) {
        k = 1;
      } else if (x < q[3]) {
        k = 2;
      } else if (x <= q[4]) {
        k = 3;
      } else {
        q[4] = x;
        k = 3;
      }

      // Increment positions of markers above k
      for (int i = k + 1; i < 5; i++) n[i] += 1.0;

      // Update desired positions
      for (int i = 0; i < 5; i++) np[i] += dn[i];

      // Adjust markers 2..4 (indexes 1..3)
      for (int i = 1; i <= 3; i++) adjust(i);
    }

    private void adjust(int i) {
      double d = np[i] - n[i];
      int s = (d >= 1.0) ? 1 : (d <= -1.0 ? -1 : 0);
      if (s == 0) return;

      // Try parabolic interpolation
      double n_im1 = n[i - 1], n_i = n[i], n_ip1 = n[i + 1];
      double q_im1 = q[i - 1], q_i = q[i], q_ip1 = q[i + 1];

      double denom = (n_ip1 - n_im1);
      double qiParabolic = q_i;
      boolean parabolicOk = false;

      if (denom != 0.0 && (n[i + s] - n_i) > 0.0 && (n_i - n[i - s]) > 0.0) {
        qiParabolic = q_i + s * (
            ((n_i - n_im1 + s) * (q_ip1 - q_i) / (n_ip1 - n_i)) +
                ((n_ip1 - n_i - s) * (q_i - q_im1) / (n_i - n_im1))
        ) / denom;
        parabolicOk = (qiParabolic > q_im1 && qiParabolic < q_ip1);
      }

      // Accept parabolic if within neighbors; else do linear step toward neighbor
      if (parabolicOk) {
        q[i] = qiParabolic;
      } else {
        q[i] = q_i + s * (q[i + s] - q_i) / (n[i + s] - n_i);
      }

      n[i] += s;
    }

    double value() {
      if (m == 0) return Double.NaN;
      if (m < 5) {
        double[] copy = Arrays.copyOf(q, m);
        Arrays.sort(copy);
        int mid = (m - 1) / 2;
        return ((m & 1) == 1) ? copy[mid] : 0.5 * (copy[mid] + copy[mid + 1]);
      }
      return q[2]; // middle marker tracks the quantile
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

  @Override
  public Statistics create() {
    return new QuantileStatistics();
  }

  @Override
  public String getName() {
    return "Quantiles";
  }

  @Override
  public String getDescription() {
    return "Quantile statistics";
  }
}
