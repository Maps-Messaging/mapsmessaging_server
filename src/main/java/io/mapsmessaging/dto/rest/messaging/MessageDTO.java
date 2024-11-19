/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.dto.rest.messaging;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Message",
    description =
        "Represents a messaging entity with configurable quality, priority, and metadata attributes.")
public class MessageDTO {

  @NotNull
  @Schema(
      title = "Payload",
      description = "The main payload content of the message, represented as a byte array.",
      example = "[72, 101, 108, 108, 111]" // Example of a "Hello" payload in ASCII bytes
      )
  private byte[] payload;

  @Schema(
      title = "Content Type",
      description = "The MIME type of the message payload, indicating its format.",
      example = "application/json")
  private String contentType;

  @Schema(
      title = "Correlation Data",
      description = "Additional data used for correlating messages, provided as a byte array.",
      example = "[1, 2, 3, 4]")
  private byte[] correlationData;

  @Schema(
      title = "Message Parameters",
      description = "A map containing optional key-value pairs associated with the message.",
      example = "{\"key1\": \"value1\", \"key2\": 42}")
  private Map<String, Object> dataMap;

  @Schema(
      title = "Expiry Time",
      description =
          "The expiry time for the message in milliseconds. Default is -1, indicating no expiry.",
      example = "60000",
      defaultValue = "-1")
  private long expiry = -1;

  @Schema(
      title = "Priority",
      description =
          "The priority level of the message, ranging from 0 (lowest) to 10 (highest). Default is 4 (normal).",
      example = "4",
      defaultValue = "4")
  private int priority = Priority.NORMAL.getValue();

  @Schema(
      title = "Quality of Service",
      description =
          "The Quality of Service level for the message: 0 (at most once), 1 (at least once), or 2 (exactly once).",
      example = "1",
      defaultValue = "0")
  private int qualityOfService = QualityOfService.AT_MOST_ONCE.getLevel();
}
