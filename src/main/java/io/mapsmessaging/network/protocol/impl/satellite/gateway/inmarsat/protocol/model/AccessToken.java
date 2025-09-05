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

// src/main/java/io/mapsmessaging/network/protocol/impl/orbcomm/inmarsat/model/AccessToken.java
package io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;

@Data
@ToString
public final class AccessToken {
  @SerializedName("token_type")
  private String tokenType;
  @SerializedName("expires_in")
  private int expiresIn;
  @SerializedName("access_token")
  private String token;
  @SerializedName("scope")
  private String scope;

  public Instant expiresAt(Instant obtainedAt, int skewSeconds) {
    return obtainedAt.plusSeconds(Math.max(0L, (long) expiresIn - Math.max(0, skewSeconds)));
  }
}
