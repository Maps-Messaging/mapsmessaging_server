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

/**
 * Thresholds of one KPI as fractions (0.95 = 95%). Bands are inclusive at the green and amber
 * boundary, matching the stakeholder's wording ("Green &gt;= 95%", "Green &lt;= 5%").
 *
 * @param higherIsBetter true for rates such as readiness, false for shares such as stale assets
 * @param green the green boundary
 * @param amber the amber boundary; beyond it the KPI is red
 */
public record BandDefinition(boolean higherIsBetter, double green, double amber) {

  public BandDefinition {
    if (higherIsBetter ? amber > green : amber < green) {
      throw new IllegalArgumentException("amber boundary " + amber + " is beyond green boundary " + green);
    }
  }

  public Band evaluate(double value) {
    if (Double.isNaN(value)) {
      return Band.NO_DATA;
    }
    if (higherIsBetter) {
      if (value >= green) {
        return Band.GREEN;
      }
      return value >= amber ? Band.AMBER : Band.RED;
    }
    if (value <= green) {
      return Band.GREEN;
    }
    return value <= amber ? Band.AMBER : Band.RED;
  }
}
