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

import java.util.Map;
import java.util.Set;

/**
 * KPI values of one evaluation, before banding.
 *
 * @param values every KPI's numerator and denominator
 * @param rosterSize active (mission roster) assets
 * @param eligibleSize active MTI-eligible assets
 * @param unknownReasons assets per unknown reason; an asset can count under several reasons
 * @param freshMtiUids MTI-eligible assets with a fresh MTI record - the MTI cache rebuild baseline
 */
public record KpiSnapshot(
    Map<KpiId, Value> values,
    int rosterSize,
    int eligibleSize,
    Map<AssetAssessment.UnknownReason, Integer> unknownReasons,
    Set<String> freshMtiUids) {

  /** A share: {@code numerator / denominator}, NaN when the denominator is empty. */
  public record Value(int numerator, int denominator) {

    public double value() {
      return denominator == 0 ? Double.NaN : (double) numerator / denominator;
    }
  }

  public Value value(KpiId kpi) {
    return values.get(kpi);
  }
}
