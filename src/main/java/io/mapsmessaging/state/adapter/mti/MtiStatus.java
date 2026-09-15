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

/** This adapter's own cached view of one asset's last-known MTI status. */
record MtiStatus(String uid, String state, String remarks) {

  static MtiStatus from(MtiWireMessage message) {
    return new MtiStatus(message.uid(), message.state(), buildRemarks(message));
  }

  private static String buildRemarks(MtiWireMessage message) {
    StringBuilder remarks = new StringBuilder("MTI: ").append(nullToUnknown(message.state()));

    if (message.domains() != null && !message.domains().isEmpty()) {
      remarks.append(" | domains: ").append(String.join(", ", message.domains()));
    }

    if (message.findings() != null && !message.findings().isEmpty()) {
      remarks.append(" | findings: ");
      for (int i = 0; i < message.findings().size(); i++) {
        if (i > 0) {
          remarks.append("; ");
        }
        MtiWireMessage.Finding finding = message.findings().get(i);
        remarks.append(finding.domain()).append('#').append(finding.rank())
            .append(' ').append(finding.signalId())
            .append(" (age ").append((int) finding.ageSeconds()).append("s)");
      }
    }

    if (message.mtiId() != null && !message.mtiId().isBlank()) {
      remarks.append(" | mti_id=").append(message.mtiId());
    }
    if (message.classification() != null && !message.classification().isBlank()) {
      remarks.append(" | ").append(message.classification());
    }

    return remarks.toString();
  }

  private static String nullToUnknown(String value) {
    return value == null || value.isBlank() ? "unknown" : value;
  }
}
