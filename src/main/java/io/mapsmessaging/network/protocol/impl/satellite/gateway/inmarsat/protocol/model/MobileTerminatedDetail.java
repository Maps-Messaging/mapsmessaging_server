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

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public final class MobileTerminatedDetail {
  @SerializedName("messageId")
  private String messageId;     // UUID
  @SerializedName("destinationId")
  private String destinationId; // device
  @SerializedName("state")
  private Integer state;        // 0..8
  @SerializedName("submitTime")
  private String submitTime;    // ISO-8601 (if present)
  @SerializedName("updatedTime")
  private String updatedTime;   // ISO-8601
  @SerializedName("errorCode")
  private Integer errorCode;    // optional
  @SerializedName("errorMsg")
  private String errorMsg;      // optional
}
