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
public class ForwardStatus {

  @SerializedName("ID")
  private Long id;

  @SerializedName("AccountID")
  private Integer accountId;

  @SerializedName("IsClosed")
  private Boolean isClosed;

  @SerializedName("State")
  private MessageState state;

  @SerializedName("CreateUTC")
  private String createUtc;

  @SerializedName("StatusUTC")
  private String statusUtc;

  @SerializedName("ErrorID")
  private Integer errorId;

  @SerializedName("Transport")
  private Integer transport;

  @SerializedName("RegionName")
  private String regionName;

  @SerializedName("ReferenceNumber")
  private Integer referenceNumber;

  @SerializedName("Network")
  private Integer network;

  public boolean isDelivered() {
    return state == MessageState.ACKNOWLEDGED;
  }

  public enum MessageState {
    @SerializedName("0")
    ACCEPTED,
    @SerializedName("1")
    ACKNOWLEDGED,
    @SerializedName("2")
    SUBMISSION_ERROR,
    @SerializedName("3")
    DELIVERY_FAILED,
    @SerializedName("4")
    TIMED_OUT,
    @SerializedName("5")
    CANCELLED,
    @SerializedName("6")
    DELAYED_QUEUE,
    @SerializedName("7")
    BROADCASTED,
    @SerializedName("8")
    SENDING
  }
}
