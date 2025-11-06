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

package io.mapsmessaging.dto.rest.messaging;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Session Info",
    description = "Information about a messaging session including subscriptions and message counts.")
public class SessionInfoDTO {

  @Schema(
      title = "Session Identifier",
      description = "The unique identifier for the session.",
      example = "session-123")
  private String sessionId;

  @Schema(
      title = "Session Name",
      description = "A user-friendly name for the session.",
      example = "MySession",
      nullable = true)
  private String sessionName;

  @Schema(
      title = "Creation Time",
      description = "When the session was created.",
      example = "2024-01-15T10:30:00")
  private LocalDateTime creationTime;

  @Schema(
      title = "Last Activity",
      description = "The last time there was activity on this session.",
      example = "2024-01-15T10:35:00")
  private LocalDateTime lastActivity;

  @Schema(
      title = "Subscription Count",
      description = "The number of active subscriptions in this session.",
      example = "3")
  private int subscriptionCount;

  @Schema(
      title = "Outstanding Messages",
      description = "Map of destination names to the count of outstanding messages.",
      example = "{\"topic1\": 5, \"topic2\": 0}")
  private Map<String, Integer> outstandingMessages;

  @Schema(
      title = "Transactional",
      description = "Whether this is a transactional session.",
      example = "true")
  private boolean transactional;

  @Schema(
      title = "Active Transaction ID",
      description = "The current active transaction identifier if any.",
      example = "txn-12345",
      nullable = true)
  private String activeTransactionId;

  @Schema(
      title = "Session Status",
      description = "The current status of the session (e.g., 'ACTIVE', 'IDLE', 'CLOSED').",
      example = "ACTIVE")
  private String status;

}
