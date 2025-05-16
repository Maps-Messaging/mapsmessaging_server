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

package io.mapsmessaging.network.protocol.impl.nats.frames;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class InfoData {

  @SerializedName("client_id")
  private long clientId;

  @SerializedName("client_ip")
  private String clientIp;

  @SerializedName("server_id")
  private String serverId;

  private String serverName;

  private String version;
  private String host;
  private int port;

  @SerializedName("max_payload")
  private int maxPayloadLength;

  @SerializedName("tls_required")
  private boolean tlsRequired = false;

  @SerializedName("auth_required")
  private boolean authRequired = false;

  private boolean headers = true;

  @SerializedName("jetstream")
  private boolean jetStream = true;

  private int proto = 1;

  private String java;
}
