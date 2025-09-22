package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rolling window stats (size-based) with O(1) updates using sum/sumSquares.
 * Mean = sum/n; Sample variance = (sumSquares - sum*sum/n)/(n-1).
 */
public class WindowedStatistics extends AdvancedStatistics {

  private final int windowSize;

  private final Deque<Double> window;
  private double windowSum;
  private double windowSumSquares;

  public WindowedStatistics(int windowSize) {
    super();
    this.windowSize = Math.max(1, windowSize);
    this.window = new ArrayDeque<>(this.windowSize);
    this.windowSum = 0.0;
    this.windowSumSquares = 0.0;
  }

  @Override
  public void reset() {
    super.reset();
    if (this.window != null) {
      window.clear();
    }
    windowSum = 0.0;
    windowSumSquares = 0.0;
  }

  @Override
  protected void update(double currentValue) {
    super.update(currentValue);

    window.addLast(currentValue);
    windowSum += currentValue;
    windowSumSquares += currentValue * currentValue;

    if (window.size() > windowSize) {
      double removed = window.removeFirst();
      windowSum -= removed;
      windowSumSquares -= removed * removed;
    }
  }

  public int getWindowCount() {
    return window.size();
  }

  public double getWindowMean() {
    if (window.isEmpty()) {
      return 0.0;
    }
    return windowSum / window.size();
  }

  public double getWindowStdDeviation() {
    int n = window.size();
    if (n < 2) {
      return 0.0;
    }
    double variance = (windowSumSquares - (windowSum * windowSum) / n) / (n - 1);
    if (variance < 0.0) {
      variance = 0.0;
    }
    return Math.sqrt(variance);
  }

  @Override
  protected void addSubclassJson(JsonObject o) {
    super.addSubclassJson(o);
    o.addProperty("windowSize", windowSize);
    o.addProperty("windowCount", getWindowCount());
    o.addProperty("windowMean", getWindowMean());
    o.addProperty("windowStdDev", getWindowStdDeviation());
  }

}