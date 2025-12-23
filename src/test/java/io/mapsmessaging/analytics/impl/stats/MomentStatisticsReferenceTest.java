package io.mapsmessaging.analytics.impl.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MomentStatisticsReferenceTest {

  @Test
  void skewnessAndKurtosis_matchDirectCentralMomentReference() {
    double[] values = {1, 2, 2, 3, 9};

    MomentStatistics stats = new MomentStatistics();
    for (double v : values) {
      stats.update(v);
    }

    ReferenceMoments ref = ReferenceMoments.compute(values);
    assertEquals(ref.sampleSkewness, stats.getSampleSkewness(), 1e-3);
    assertEquals(ref.sampleKurtosisExcess, stats.getSampleKurtosisExcess(), 1e-3);
  }

  @Test
  void symmetricDiscreteData_hasZeroSkewness_andNegativeExcessKurtosis() {
    double[] values = {-2.0, -1.5, -1.0, -0.5, 0.0, 0.5, 1.0, 1.5, 2.0};

    MomentStatistics stats = new MomentStatistics();
    for (double v : values) {
      stats.update(v);
    }

    ReferenceMoments ref = ReferenceMoments.compute(values);

    // Symmetry should give exact (or extremely close) zero skewness.
    assertEquals(ref.sampleSkewness, stats.getSampleSkewness(), 1e-12);

    // This dataset is NOT normal, so excess kurtosis is expected to be negative.
    assertEquals(ref.sampleKurtosisExcess, stats.getSampleKurtosisExcess(), 1e-12);
  }

  private static final class ReferenceMoments {
    private final double sampleSkewness;
    private final double sampleKurtosisExcess;

    private ReferenceMoments(double sampleSkewness, double sampleKurtosisExcess) {
      this.sampleSkewness = sampleSkewness;
      this.sampleKurtosisExcess = sampleKurtosisExcess;
    }

    static ReferenceMoments compute(double[] x) {
      int n = x.length;
      if (n == 0) {
        return new ReferenceMoments(Double.NaN, Double.NaN);
      }

      double mean = 0.0;
      for (double v : x) {
        mean += v;
      }
      mean /= n;

      double m2 = 0.0;
      double m3 = 0.0;
      double m4 = 0.0;

      for (double v : x) {
        double d = v - mean;
        double d2 = d * d;
        m2 += d2;
        m3 += d2 * d;
        m4 += d2 * d2;
      }

      double skew = 0.0;
      if (n >= 3 && m2 != 0.0) {
        skew = (Math.sqrt((double) n * (n - 1.0)) / (n - 2.0)) * (m3 / Math.pow(m2, 1.5));
      }

      double kurtExcess = 0.0;
      if (n >= 4 && m2 != 0.0) {
        kurtExcess =
            ((n * (n + 1.0) * m4) / (m2 * m2 * (n - 1.0) * (n - 2.0) * (n - 3.0)))
                - (3.0 * (n - 1.0) * (n - 1.0) / ((n - 2.0) * (n - 3.0)));
      }

      return new ReferenceMoments(skew, kurtExcess);
    }
  }
}
