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
 *
 */

package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "TAK Cursor on Target event.")
public class TakEvent {

  @Schema(description = "CoT schema version.", example = "2.0")
  private String version = "2.0";

  @Schema(description = "Unique TAK event identifier.", example = "drone-1")
  private String uid;

  @Schema(description = "Cursor on Target type.", example = "a-f-A-M-F-U")
  private String type;

  @Schema(description = "How the event position was determined.", example = "h-g-i-g-o")
  private String how;

  @Schema(description = "Event timestamp in UTC ISO-8601 format.", example = "2026-04-21T06:31:03.212Z")
  private String time;

  @Schema(description = "Start timestamp in UTC ISO-8601 format.", example = "2026-04-21T06:31:03.212Z")
  private String start;

  @Schema(description = "Stale timestamp in UTC ISO-8601 format.", example = "2026-04-21T06:31:33.212Z")
  private String stale;

  @Schema(description = "Geospatial point for the event.")
  private TakPoint point;

  @Schema(description = "Detailed TAK metadata.")
  private TakDetail detail;
}