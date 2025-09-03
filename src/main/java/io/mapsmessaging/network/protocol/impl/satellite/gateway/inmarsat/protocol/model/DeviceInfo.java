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
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
@Data
public final class DeviceInfo extends RemoteDeviceInfo {
  @SerializedName("mailboxId")
  private String mailboxId;
  @SerializedName("deviceId")
  private String deviceId;

  @SerializedName("provisioningTime")
  private String provisioningTime;
  @SerializedName("mailboxAlias")
  private String mailboxAlias;


  @Override
  public String getUniqueId() {
    return deviceId;
  }
}
