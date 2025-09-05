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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnMessage {

  @SerializedName("ID")
  private Long id;

  @SerializedName("MessageUTC")
  private String messageUTC;

  @SerializedName("ReceiveUTC")
  private String receiveUTC;

  @SerializedName("SIN")
  private Integer sin;

  @SerializedName("MobileID")
  private String mobileId;

  @SerializedName("NetworkID")
  private String networkId;

  @SerializedName("RawPayload")
  private String rawPayload;

  @SerializedName("Payload")
  private CommonMessage payload;

  @SerializedName("RegionName")
  private String regionName;

  @SerializedName("OTAMessageSize")
  private Integer otaMessageSize;

  @SerializedName("AccountID")
  private Integer accountId;

  @SerializedName("Transport")
  private Integer transport;

  @SerializedName("MobileOwnerID")
  private Integer mobileOwnerId;

  @SerializedName("Network")
  private Integer network;
}
