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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public final class Item {
  @SerializedName("destinationId")
  private final String destinationId;
  @SerializedName("userMessageId")
  private final String userMessageId; // optional
  @SerializedName("payloadRaw")
  private final String payloadRaw;    // optional (Base64)
  @SerializedName("payloadJson")
  private final JsonObject payloadJson; // optional

  public Item(String destinationId, String userMessageId, String payloadRaw, JsonObject payloadJson) {
    this.destinationId = destinationId;
    this.userMessageId = userMessageId;
    this.payloadRaw = payloadRaw;
    this.payloadJson = payloadJson;
  }
}