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

package io.mapsmessaging.state.drone.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A contact detected by a drone.")
public class Contact {

  @Schema(
      description = "Unique identifier for this detected contact.",
      example = "019f1a3e-8f15-7c02-9d12-7159b3c61a12",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private UUID id;

  @Schema(
      description = "Human-readable description of the detected contact.",
      example = "Unknown vehicle detected near road junction",
      nullable = true
  )
  private String description;

  @Schema(
      description = "Last known position of the contact.",
      nullable = true
  )
  private GeoPosition position;

  @Schema(
      description = "Time-to-live for the contact in milliseconds.",
      example = "30000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private long ttlMillis;

  @Schema(
      description = "Time the contact was first created, expressed as epoch milliseconds.",
      example = "1780537200000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private long createdTimeMs;

  @Schema(
      description = "Time the contact was last updated, expressed as epoch milliseconds.",
      example = "1780537215000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private long updatedTimeMs;

  public Contact(String description, GeoPosition position, long ttlMillis) {
    long now = System.currentTimeMillis();

    this.id = UUID.nameUUIDFromBytes(description.getBytes());
    this.description = description;
    this.position = position;
    this.ttlMillis = ttlMillis;
    this.createdTimeMs = now;
    this.updatedTimeMs = now;
  }

  public boolean isExpired(long nowMs) {
    return ttlMillis > 0 && updatedTimeMs + ttlMillis <= nowMs;
  }

  public void update(String description, GeoPosition position, long ttlMillis, long nowMs) {
    this.description = description;
    this.position = position;
    this.ttlMillis = ttlMillis;
    this.updatedTimeMs = nowMs;
  }
}