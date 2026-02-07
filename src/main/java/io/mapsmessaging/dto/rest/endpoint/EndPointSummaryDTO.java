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

package io.mapsmessaging.dto.rest.endpoint;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "End Point Information",
    description = "Provides overview information about the end point, including identity, protocol details, "
        + "connection timestamps, and traffic/buffer statistics."
)
public class EndPointSummaryDTO {

  @Schema(
      description = "Unique identifier for the endpoint.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "12345",
      minimum = "1",
      maximum = "9223372036854775807"
  )
  private long id;

  @Schema(
      description = "Adapter name or type associated with this endpoint (implementation-specific).",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "tcp",
      minLength = 1,
      maxLength = 64
  )
  private String adapter;

  @Schema(
      description = "Name assigned to the endpoint.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "sensor-gateway-01",
      minLength = 1,
      maxLength = 128
  )
  private String name;

  @Schema(
      description = "Username associated with the endpoint, if authenticated.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "matthew",
      minLength = 1,
      maxLength = 128
  )
  private String user;

  @Schema(
      description = "Name of the protocol used by the endpoint.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "mqtt",
      minLength = 1,
      maxLength = 32
  )
  private String protocolName;

  @Schema(
      description = "Version of the protocol used by the endpoint.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "5.0",
      minLength = 1,
      maxLength = 32
  )
  private String protocolVersion;

  @Schema(
      description = "Proxy address used to connect the endpoint, if any. Typically an IP or hostname, optionally with port.",
      nullable = true,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      example = "203.0.113.10:3128",
      minLength = 1,
      maxLength = 255
  )
  private String proxyAddress;

  @Schema(
      description = "Connection start time in milliseconds since epoch.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "1738891200000",
      minimum = "0",
      maximum = "253402300799999"
  )
  private long connectedTimeMs;

  @Schema(
      description = "Timestamp of the last read operation in milliseconds since epoch.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "1738891265123",
      minimum = "0",
      maximum = "253402300799999"
  )
  private long lastRead;

  @Schema(
      description = "Timestamp of the last write operation in milliseconds since epoch.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "1738891268456",
      minimum = "0",
      maximum = "253402300799999"
  )
  private long lastWrite;

  @Schema(
      description = "Total bytes read by the endpoint since connection start.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "987654321",
      minimum = "0",
      maximum = "9223372036854775807"
  )
  private long totalBytesRead;

  @Schema(
      description = "Total bytes written by the endpoint since connection start.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "123456789",
      minimum = "0",
      maximum = "9223372036854775807"
  )
  private long totalBytesWritten;

  @Schema(
      description = "Total number of buffer overflows since connection start.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "0",
      minimum = "0",
      maximum = "9223372036854775807"
  )
  private long totalOverflow;

  @Schema(
      description = "Total number of buffer underflows since connection start.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "2",
      minimum = "0",
      maximum = "9223372036854775807"
  )
  private long totalUnderflow;

  @Schema(
      description = "Bytes read in the current statistics interval (interval length is server-defined).",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "4096",
      minimum = "0",
      maximum = "9223372036854775807"
  )
  private long bytesRead;

  @Schema(
      description = "Bytes written in the current statistics interval (interval length is server-defined).",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "1024",
      minimum = "0",
      maximum = "9223372036854775807"
  )
  private long bytesWritten;

  @Schema(
      description = "Buffer overflow count in the current statistics interval.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "0",
      minimum = "0",
      maximum = "9223372036854775807"
  )
  private long overFlow;

  @Schema(
      description = "Buffer underflow count in the current statistics interval.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "0",
      minimum = "0",
      maximum = "9223372036854775807"
  )
  private long underFlow;
}
