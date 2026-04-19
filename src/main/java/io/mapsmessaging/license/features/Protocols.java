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
@Schema(description = "Protocol feature configuration for the license. All fields are required.")
public class Protocols {

  @Schema(description = "Enable MQTT protocol support.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean mqtt;

  @Schema(description = "Enable AMQP protocol support.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean amqp;

  @Schema(description = "Enable NATS protocol support.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean nats;

  @Schema(description = "Enable MQTT-SN protocol support.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean mqtt_sn;

  @Schema(description = "Enable CoAP protocol support.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean coap;

  @Schema(description = "Enable NMEA 0183 protocol support.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean nmea_0183;

  @Schema(description = "Enable Semtech LoRa protocol support.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean semtech;

  @Schema(description = "Enable custom protocol extensions.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean extensions;

  @Schema(description = "Enable STOMP protocol support.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean stomp;

  @Schema(description = "Enable REST protocol support.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean rest;

  @Schema(description = "Enable LoRa protocol support.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean lora;

  @Schema(description = "Enable WebSocket (WS) protocol support.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean ws;

  @Schema(description = "Enable secure WebSocket (WSS) protocol support.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean wss;

  @Schema(description = "Enable ST OGi protocol support.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean stogi;

  @Schema(description = "Enable MAVLink protocol support.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean mavlink;

  @Schema(description = "Enable NMEA 2000 (N2K) protocol support.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean n2k;
}