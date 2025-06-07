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

package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "Serial Configuration DTO")
public class SerialConfigDTO extends EndPointConfigDTO {

  @Schema(description = "Serial port name", example = "/dev/ttyS0")
  protected String port;
  @Schema(
      description = "Baud rate for the serial connection",
      example = "9600",
      allowableValues = {"110", "300", "600", "1200", "2400", "4800", "9600", "14400", "19200", "28800", "38400", "57600", "115200", "230400", "460800", "921600"}
  )
  protected int baudRate;

  @Schema(
      description = "Number of data bits in the serial connection",
      example = "8",
      allowableValues = {"5", "6", "7", "8"}
  )
  protected int dataBits;

  @Schema(
      description = "Number of stop bits in the serial connection",
      example = "1",
      allowableValues = {"1", "1.5", "2"}
  )
  protected String stopBits;

  @Schema(
      description = "Parity setting for the serial connection",
      example = "n",
      allowableValues = {"n", "o", "e", "m", "s"}
  )
  protected String parity;

  @Schema(
      description = "Flow control setting for the serial connection",
      example = "1",
      allowableValues = {"0", "1", "2", "3"}
  )
  protected int flowControl;

  @Schema(description = "Read timeout in milliseconds", example = "60000")
  protected int readTimeOut;

  @Schema(description = "Write timeout in milliseconds", example = "60000")
  protected int writeTimeOut;

  @Schema(description = "Buffer size in bytes", example = "262144")
  protected int bufferSize;

  @Schema(description = "Serial number for the device, optional", example = "262144")
  protected String serialNo;

}
