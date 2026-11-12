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

package io.mapsmessaging.state.mavlink;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Instant;

/** Immutable observation of a MAVLink protocol transaction. Contains no protocol executor or mission payload. */
public record MavlinkTransactionStatus(
    String transactionId, int systemId, int componentId, String status, int attempts, int retryCount,
    int requestedItems, int totalItems, String reason, Instant observedAt) {

  public static MavlinkTransactionStatus fromJson(JsonObject event, Instant observedAt) {
    if (event == null || observedAt == null) {
      throw new IllegalArgumentException("MAVLink transaction status and observation time are required");
    }
    String transactionId = requiredString(event, "transactionId");
    String status = requiredString(event, "status");
    int systemId = requiredInt(event, "systemId");
    int componentId = requiredInt(event, "componentId");
    int attempts = requiredInt(event, "attempts");
    int retryCount = requiredInt(event, "retryCount");
    int requestedItems = requiredInt(event, "requestedItems");
    int totalItems = requiredInt(event, "totalItems");
    if (systemId < 1 || systemId > 255 || componentId < 0 || componentId > 255
        || attempts < 0 || retryCount < 0 || requestedItems < 0 || totalItems < 0 || requestedItems > totalItems) {
      throw new IllegalArgumentException("Invalid MAVLink transaction status values");
    }
    String reason = event.has("reason") && !event.get("reason").isJsonNull() ? requiredString(event, "reason") : null;
    return new MavlinkTransactionStatus(transactionId, systemId, componentId, status, attempts, retryCount,
        requestedItems, totalItems, reason, observedAt);
  }

  private static String requiredString(JsonObject event, String name) {
    JsonElement element = event.get(name);
    if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
        || element.getAsString().isBlank()) {
      throw new IllegalArgumentException("Missing or invalid MAVLink transaction field " + name);
    }
    return element.getAsString();
  }

  private static int requiredInt(JsonObject event, String name) {
    JsonElement element = event.get(name);
    if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      throw new IllegalArgumentException("Missing or invalid MAVLink transaction field " + name);
    }
    long value = element.getAsLong();
    if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE || value != element.getAsDouble()) {
      throw new IllegalArgumentException("Invalid MAVLink transaction numeric field " + name);
    }
    return (int) value;
  }
}
