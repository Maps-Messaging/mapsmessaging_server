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

package io.mapsmessaging.network.protocol.impl.orbcomm.inmarsat.client.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public final class DeviceInfo {
  @SerializedName("mailboxId")
  private String mailboxId;
  @SerializedName("deviceId")
  private String deviceId;
  @SerializedName("lastModemRegistrationTime")
  private String lastModemRegistrationTime; // ISO-8601
  @SerializedName("wakeUpInterval")
  private int wakeUpInterval;
  @SerializedName("provisioningTime")
  private String provisioningTime;
  @SerializedName("lastUpdatedTime")
  private String lastUpdatedTime;
  @SerializedName("mailboxAlias")
  private String mailboxAlias;                 // optional
  @SerializedName("operationMode")
  private Integer operationMode;                  // 0..3 (ALWAYS_ON, WAKE_UP, RECEIVE_ON_SEND, HYBRID)
  @SerializedName("network")
  private Integer network;            // 0=IDP, 1=OGx
}
