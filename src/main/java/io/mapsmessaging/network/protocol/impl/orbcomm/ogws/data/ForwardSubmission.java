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

package io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ForwardSubmission {
  @SerializedName("ID")
  private Long id;

  @SerializedName("DestinationID")
  private String destinationId;

  @SerializedName("UserMessageID")
  private Long userMessageId;

  @SerializedName("OTAMessageSize")
  private Integer otaMessageSize;

  @SerializedName("OperationMode")
  private Integer operationMode;

  @SerializedName("ScheduledSendUTC")
  private String scheduledSendUTC;

  @SerializedName("TerminalWakeupPeriod")
  private Integer terminalWakeupPeriod;

  @SerializedName("ErrorID")
  private Integer errorId;

  public boolean isAccepted() {
    return errorId == null || errorId == 0;
  }
}