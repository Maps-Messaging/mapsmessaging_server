/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.cot.CotManagedPlatformConfigDTO;

public record CotIdentityBinding(
    String endpoint,
    String uid,
    String twinId,
    String outboundUid,
    String taskingProfile,
    Double haeToMslOffsetMeters,
    int sourcePriority,
    boolean taskable) {

  static CotIdentityBinding from(CotManagedPlatformConfigDTO source) {
    String outbound = source.getOutboundUid();
    if (outbound == null || outbound.isBlank()) {
      outbound = source.getUid();
    }
    return new CotIdentityBinding(
        source.getEndpoint(),
        source.getUid(),
        source.getTwinId(),
        outbound,
        source.getTaskingProfile(),
        source.getHaeToMslOffsetMeters(),
        source.getSourcePriority(),
        source.isTaskable());
  }
}
