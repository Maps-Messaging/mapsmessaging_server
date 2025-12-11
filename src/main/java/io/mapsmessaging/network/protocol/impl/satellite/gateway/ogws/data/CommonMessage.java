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

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CommonMessage {

  @SerializedName("Name")
  private String name;

  @SerializedName("IsForward")
  private Boolean isForward; // optional

  @SerializedName("RawPayload")
  private String rawPayload; // optional (only present in some contexts)

  @SerializedName("TransportType")
  private int transportType = 0;

  @SerializedName("MessageLifetime")
  private int messageLifetime = 24; // Hours

  @SerializedName("MessageClass")
  private int messageClass = 2; // normal

  @SerializedName("SIN")
  private int sin = 0;

  @SerializedName("MIN")
  private int min = 0;

  @SerializedName("Fields")
  private List<Field> fields;
}
