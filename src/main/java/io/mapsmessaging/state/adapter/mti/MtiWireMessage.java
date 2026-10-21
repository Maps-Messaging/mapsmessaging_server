/*
 *
 *  Copyright [ 2026 ] Ralf Himmelein and Claude
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

package io.mapsmessaging.state.adapter.mti;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Gson binding for the MTI server's {@code mti.asset.health/v1} wire message. Field-for-field
 * per the MTI team's spec. {@code domains}/{@code findings} are opaque, mission-configured
 * display detail - never pattern-matched against here, only rendered as text (see
 * {@link MtiStatus}).
 */
record MtiWireMessage(
    String schema,
    String op, // new | update | delete
    String uid,
    String name,
    String state, // go | mitigate | hold | unknown
    List<String> domains,
    @SerializedName("cia_impact") List<String> ciaImpact,
    List<Finding> findings,
    @SerializedName("observed_at") String observedAt,
    @SerializedName("valid_until") String validUntil,
    @SerializedName("mti_id") String mtiId,
    String classification) {

  record Finding(
      String domain,
      int rank,
      @SerializedName("signal_id") String signalId,
      @SerializedName("age_seconds") double ageSeconds) {
  }
}
