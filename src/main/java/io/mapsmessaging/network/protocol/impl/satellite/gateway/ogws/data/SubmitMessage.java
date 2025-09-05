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
@NoArgsConstructor
@AllArgsConstructor
public class SubmitMessage {

  @SerializedName("DestinationID")
  private String destinationId;

  @SerializedName("UserMessageID")
  private Long userMessageId;

  @SerializedName("RawPayload")
  private String rawPayload;

  @SerializedName("TransportType")
  private Integer transportType;

  @SerializedName("MessageLifetime")
  private Integer messageLifetime; // in hours

  @SerializedName("DelayedSendOptions")
  private SatelliteDelayedSendOptions delayedSendOptions;

  @SerializedName("MessageClass")
  private Integer messageClass; // 1: Premium, 2: Normal (default), 3: Background

  private transient Runnable completionCallback;
}
