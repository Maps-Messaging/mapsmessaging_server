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

package io.mapsmessaging.dto.rest.lora;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    title = "LoRa Device Configuration Information",
    description =
        "Represents configuration information for a LoRa device, including radio details and hardware settings.")
public class
LoRaDeviceConfigInfoDTO {

  @Schema(
      title = "Device Name",
      description = "The name of the LoRa device.",
      example = "LoRa_Radio_01",
      nullable = false)
  private String name;

  @Schema(
      title = "Radio Type",
      description = "Type of radio module used by the LoRa device.",
      example = "rfm95",
      nullable = false)
  private String radio;

  @Schema(
      title = "Chip Select Pin",
      description = "The chip select pin number for the LoRa device.",
      example = "10",
      minimum = "0")
  private int cs;

  @Schema(
      title = "Interrupt Request Pin",
      description = "The interrupt request (IRQ) pin number for the LoRa device.",
      example = "2",
      minimum = "0")
  private int irq;

  @Schema(
      title = "Reset Pin",
      description = "The reset pin number for the LoRa device.",
      example = "4",
      minimum = "0")
  private int rst;

  @Schema(
      title = "Power Level",
      description = "The transmission power level setting for the LoRa device.",
      example = "14",
      minimum = "0",
      maximum = "20")
  private int power;

  @Schema(
      title = "CAD Timeout",
      description = "The Channel Activity Detection (CAD) timeout in milliseconds.",
      example = "100",
      minimum = "0")
  private int cadTimeout;

  @Schema(
      title = "Frequency",
      description = "The operating frequency for the LoRa device in MHz.",
      example = "915.0",
      minimum = "0.0")
  private float frequency;
}
