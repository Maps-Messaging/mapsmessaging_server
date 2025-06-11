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

package io.mapsmessaging.utilities.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "Details Statistics")
public class DetailedStatisticsJMX {

  private final LinkedMovingAverages movingAverages;
  private final ObjectInstance objectInstance;

  public DetailedStatisticsJMX(List<String> jmxPath, LinkedMovingAverages movingAverages) {
    this.movingAverages = movingAverages;
    List<String> copy = new ArrayList<>(jmxPath);
    copy.add("Statistics=detailed");
    objectInstance = JMXManager.getInstance().register(this, copy);
  }

  public void close() {
    JMXManager.getInstance().unregister(objectInstance);
  }

  //<editor-fold desc="JMX Bean implementation">
  @JMXBeanAttribute(name = "Maximum value", description = "Returns the current maximum within the current range")
  public double getMax() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0.0;
    }
    return statistics.getMax();
  }

  @JMXBeanAttribute(name = "Minimum value", description = "Returns the current minimum within the current range")
  public double getMin() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0.0;
    }
    return statistics.getMin();
  }

  @JMXBeanAttribute(name = "mean value", description = "Returns the current mean within the current range")
  public double getMean() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0.0;
    }
    return statistics.getMean();
  }

  @JMXBeanAttribute(name = "Geometric Mean value", description = "Returns the current geometric mean within the current range")
  public double getGeometricMean() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0.0;
    }
    return statistics.getGeometricMean();
  }

  @JMXBeanAttribute(name = "Quadratic Mean value", description = "Returns the current quadratic mean within the current range")
  public double getQuadraticMean() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0.0;
    }
    return statistics.getQuadraticMean();
  }

  @JMXBeanAttribute(name = "Standard Deviation value", description = "Returns the current standard deviation within the current range")
  public double getStandardDeviation() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0.0;
    }
    return statistics.getStandardDeviation();
  }

  @JMXBeanAttribute(name = "Population Variance value", description = "Returns the current population variance within the current range")
  public double getPopulationVariance() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0.0;
    }
    return statistics.getPopulationVariance();
  }

  @JMXBeanAttribute(name = "Variance value", description = "Returns the current variance within the current range")
  public double getVariance() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0.0;
    }
    return statistics.getVariance();
  }

  @JMXBeanAttribute(name = "Current number of values", description = "Returns the current number of entries within the current range")
  public long getN() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0;
    }
    return statistics.getN();
  }

  @JMXBeanAttribute(name = "SecondMoment", description = "Returns the current second moment within the current range")
  public double getSecondMoment() {
    SummaryStatistics statistics = movingAverages.getDetailedStatistics();
    if (statistics == null) {
      return 0.0;
    }
    return statistics.getSecondMoment();
  }

  //</editor-fold>
}
