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

package io.mapsmessaging.network.protocol.impl.semtech.status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Maps-generated Semtech link/lifecycle/downlink status event.
 * Published to: /semtech/status/{gatewayId}
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SemtechStatusEvent {

  private String gatewayId;

  private SemtechStatusType type;
  private SemtechStatusState state;

  private long timestampMillis;

  /**
   * Correlation id for a downlink message in Maps (if you generate one).
   */
  private String messageId;

  /**
   * Semtech token (used for PULL_RESP / TX_ACK correlation).
   */
  private Integer token;

  /**
   * TX_ACK error string (NONE, TOO_LATE, TX_FREQ, COLLISION_PACKET, etc.)
   */
  private String error;

  /**
   * Human-readable reason for the state (GATEWAY_UNKNOWN, WAITING_FOR_PULL, TTL_EXPIRED, QUEUE_FULL, etc.)
   */
  private String reason;

  /**
   * Optional: ip:port string where the gateway was last seen.
   */
  private String remoteAddress;

  /**
   * Optional: payload size for downlink.
   */
  private Integer size;

}