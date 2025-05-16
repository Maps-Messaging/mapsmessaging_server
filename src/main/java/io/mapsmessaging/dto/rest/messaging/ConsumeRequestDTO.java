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

package io.mapsmessaging.dto.rest.messaging;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Consume Request",
    description = "Requests the server to respond with any outstanding messages specified by the destination or all if no destination supplied")
public class ConsumeRequestDTO {
  @Schema(
      title = "Destination name",
      description = "Optional, if supplied gets any messages outstanding for this destination, else all messages pending delivery",
      example = "topicName")
  private String destination;

  @Schema(
      title = "Depth",
      description = "The max number of events that should be returned",
      example = "60",
      defaultValue = "10")
  private int depth;
}
