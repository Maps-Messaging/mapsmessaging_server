/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.license.features;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Network feature configuration for the license. All fields are required.")
public class Network {

  @Schema(description = "Enable UDP transport.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean udp;

  @Schema(description = "Enable HMAC authentication.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean hmac;

  @Schema(description = "Enable TCP transport.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean tcp;

  @Schema(description = "Enable SSL/TLS transport.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean ssl;

  @Schema(description = "Enable DTLS transport.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean dtls;

  @Schema(description = "Enable LoRa communication.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean lora;

  @Schema(description = "Enable serial communication.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean serial;

  @Schema(description = "Enable CAN bus communication.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean canbus;

  @Schema(description = "Enable ORBCOMM OGWS integration.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean ogws;

  @Schema(description = "Enable ST OGi modem support.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean stogi;

  @Schema(description = "Enable satellite communication features.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean satellite;

  @Schema(
      description = "Maximum number of concurrent network connections allowed.",
      example = "1000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private int maxConnections;
}