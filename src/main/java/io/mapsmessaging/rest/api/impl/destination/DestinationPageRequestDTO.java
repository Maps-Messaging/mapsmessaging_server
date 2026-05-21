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

package io.mapsmessaging.rest.api.impl.destination;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(
    name = "DestinationPageRequest",
    description = "Request for a single page of destination entries under a prefix. Pagination is zero-based."
)
public class DestinationPageRequestDTO {

  @QueryParam("prefix")
  @Schema(
      description = "Namespace prefix to browse. Leading '/' is significant. Server normalizes duplicate and trailing slashes. Empty means root.",
      example = "/a/b",
      defaultValue = "",
      nullable = true
  )
  private String prefix = "";

  @QueryParam("pageSize")
  @Schema(
      description = "Maximum number of entries returned in this page.",
      example = "50",
      defaultValue = "100",
      minimum = "10",
      maximum = "1000"
  )
  private int pageSize = 100;

  @QueryParam("pageNumber")
  @Schema(
      description = "Zero-based page number.",
      example = "0",
      defaultValue = "0",
      minimum = "0"
  )
  private int pageNumber = 0;
}
