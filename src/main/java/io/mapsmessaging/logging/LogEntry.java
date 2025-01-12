/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.logging;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(
    title = "LogEntry",
    description =
        "Represents a log entry from the server.")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LogEntry{
  @Schema(
      title = "logNumber",
      description = "Represents the order for the log entry.")
  private long logNumber;

  @Schema(
      title = "level",
      description = "The level of this log entry")
  private int level;

  @Schema(
      title = "message",
      description = "The actual log entry")
  private String message;
}
