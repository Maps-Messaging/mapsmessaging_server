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
 * The KPIs computed on the mission roster. The five critical ones drive the overall picture
 * status and MTTR (IC26 KPI clarification Q6, Q8, Q10); the rest are the diagnostic layer.
 */
public enum KpiId {
  /** Fresh GO / active MTI-eligible assets (stakeholder's corrected readiness definition). */
  READINESS(true),
  /** Fresh MTI record of any state, including explicit UNKNOWN / active MTI-eligible assets (Q3). */
  COVERAGE(true),
  /** Assets that are unknown for any reason or MTI-degraded / active assets (Q4, Q7). */
  UNKNOWN_DEGRADED(true),
  /** Assets whose position/telemetry is stale / active assets (Q5). */
  POSITION_STALE(true),
  /** Assets with a specific classification (resolved or kept original type) / active assets (Q19). */
  CLASSIFICATION_TRUST(true),

  /** Fresh GO, MITIGATE or HOLD / active MTI-eligible assets - an actionable trust opinion (Q3). */
  ACTIONABLE_TRUST(false),
  /** MTI-eligible assets without a fresh MTI status / active MTI-eligible assets (Q5). */
  MTI_STALE(false),
  /** Assets unknown for any of the four reasons / active assets. */
  UNKNOWN(false),
  /** Fresh MITIGATE or HOLD / active assets. */
  DEGRADED(false),
  /** Assets that kept their original CoT type / active assets (Q19 fallback). */
  CLASSIFICATION_FALLBACK(false),
  /** Assets with a generic or no classification / active assets (Q19 hard failure). */
  CLASSIFICATION_FAILURE(false),
  /** Scripted assets classified as expected / scripted assets with a classification (Q19). */
  CLASSIFICATION_CORRECTNESS(false);

  private final boolean critical;

  KpiId(boolean critical) {
    this.critical = critical;
  }

  public boolean isCritical() {
    return critical;
  }
}
