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
    description = "Provides overview information about the end point")
public class EndPointSummaryDTO {

  @Schema(description = "Unique identifier for the endpoint")
  private long id;

  @Schema(description = "Adapter name or type associated with this endpoint")
  private String adapter;

  @Schema(description = "Name assigned to the endpoint")
  private String name;

  @Schema(description = "Username associated with the endpoint")
  private String user;

  @Schema(description = "Name of the protocol used by the endpoint")
  private String protocolName;

  @Schema(description = "Version of the protocol used by the endpoint")
  private String protocolVersion;

  @Schema(description = "Proxy address used to connect the endpoint, if any")
  private String proxyAddress;

  @Schema(description = "Connection start time in milliseconds since epoch")
  private long connectedTimeMs;

  @Schema(description = "Timestamp of the last read operation in milliseconds since epoch")
  private long lastRead;

  @Schema(description = "Timestamp of the last write operation in milliseconds since epoch")
  private long lastWrite;

  @Schema(description = "Total bytes read by the endpoint")
  private long totalBytesRead;

  @Schema(description = "Total bytes written by the endpoint")
  private long totalBytesWritten;

  @Schema(description = "Total number of buffer overflows")
  private long totalOverflow;

  @Schema(description = "Total number of buffer underflows")
  private long totalUnderflow;

  @Schema(description = "Bytes read in the current interval")
  private long bytesRead;

  @Schema(description = "Bytes written in the current interval")
  private long bytesWritten;

  @Schema(description = "Buffer overflow count in the current interval")
  private long overFlow;

  @Schema(description = "Buffer underflow count in the current interval")
  private long underFlow;
}
