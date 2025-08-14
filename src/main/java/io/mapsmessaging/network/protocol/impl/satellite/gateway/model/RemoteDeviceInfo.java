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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public abstract class RemoteDeviceInfo {

  @SerializedName(value = "lastModemRegistrationTime", alternate = {"LastRegistrationUTC"})
  private String lastRegistrationUtc; // ISO-8601

  @SerializedName(value = "lastUpdatedTime", alternate = {"UpdateUTC"})
  private String lastUpdatedUtc; // ISO-8601

  @SerializedName(value = "wakeUpInterval", alternate = {"TerminalWakeupPeriod"})
  private Integer wakeUpInterval; // seconds

  @SerializedName(value = "operationMode", alternate = {"LastOperationMode"})
  private Integer operationModeCode; // 0..3

  @SerializedName(value = "network", alternate = {"LastSatelliteNetwork"})
  private Integer networkCode; // 0=IDP, 1=OGx

  @SerializedName(value = "IsRegistered")
  private Integer isRegistered; // nullable


  public abstract String getUniqueId();
}