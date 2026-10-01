/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.state.kpi;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.List;

/** Registered as {@code io.mapsmessaging:type=Integration,name=Kpi,kpi=<KPI>}. */
@JMXBean(description = "One IC26 mission KPI on the mission roster, with its band")
public class KpiJMX implements KpiBean {

  private final KpiEvaluator evaluator;
  private final KpiId kpi;
  private final ObjectInstance mbean;

  KpiJMX(KpiEvaluator evaluator, KpiId kpi) {
    this.evaluator = evaluator;
    this.kpi = kpi;
    this.mbean = JMXManager.getInstance().register(this, List.of("type=Integration", "name=Kpi", "kpi=" + kpi.name()));
  }

  @Override
  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Value", description = "KPI as a fraction 0.0-1.0, -1 when its population is empty")
  public double getValue() {
    KpiSnapshot latest = evaluator.getLatest();
    if (latest == null) {
      return -1.0;
    }
    double value = latest.value(kpi).value();
    return Double.isNaN(value) ? -1.0 : value;
  }

  @JMXBeanAttribute(name = "Numerator", description = "Assets counted by the KPI")
  public long getNumerator() {
    KpiSnapshot latest = evaluator.getLatest();
    return latest == null ? 0 : latest.value(kpi).numerator();
  }

  @JMXBeanAttribute(name = "Denominator", description = "Population the KPI is measured against")
  public long getDenominator() {
    KpiSnapshot latest = evaluator.getLatest();
    return latest == null ? 0 : latest.value(kpi).denominator();
  }

  @JMXBeanAttribute(name = "Raw Band", description = "Band of the current value: 0 green, 1 amber, 2 red, -1 no data or no band")
  public int getRawBand() {
    return evaluator.getRawBand(kpi).getCode();
  }

  @JMXBeanAttribute(name = "Band", description = "Displayed band (worsens at once, improves after the recovery hold): 0 green, 1 amber, 2 red, -1 no data or no band")
  public int getBand() {
    return evaluator.getDisplayedBand(kpi).getCode();
  }

  @JMXBeanAttribute(name = "Critical", description = "1 if this KPI drives the overall picture status and MTTR")
  public int getCritical() {
    return kpi.isCritical() ? 1 : 0;
  }

  @JMXBeanAttribute(name = "Green Threshold", description = "Green boundary as a fraction, -1 if the KPI has no band")
  public double getGreenThreshold() {
    BandDefinition band = evaluator.getConfig().band(kpi);
    return band == null ? -1.0 : band.green();
  }

  @JMXBeanAttribute(name = "Amber Threshold", description = "Amber boundary as a fraction, -1 if the KPI has no band")
  public double getAmberThreshold() {
    BandDefinition band = evaluator.getConfig().band(kpi);
    return band == null ? -1.0 : band.amber();
  }
}
