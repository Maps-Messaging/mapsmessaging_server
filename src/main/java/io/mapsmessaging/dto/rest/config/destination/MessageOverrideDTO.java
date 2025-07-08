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

package io.mapsmessaging.dto.rest.config.destination;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Schema(description = "Message override configuration DTO")
public class MessageOverrideDTO extends BaseConfigDTO {

  @Schema(description = "Override message expiry in milliseconds", example = "60000")
  protected Long expiry;

  @Schema(description = "Override message priority", example = "NORMAL")
  protected Priority priority;

  @Schema(description = "Override message quality of service", example = "AT_LEAST_ONCE")
  protected QualityOfService qualityOfService;

  @Schema(description = "Override response topic", example = "/default/response")
  protected String responseTopic;

  @Schema(description = "Override content type", example = "application/json")
  protected String contentType;

  @Schema(description = "Override schema ID", example = "default-schema-id")
  protected String schemaId;

  @Schema(description = "Override retain message flag", example = "true")
  protected Boolean retain;

  @Schema(description = "Metadata to inject if not present in the message")
  protected Map<String, String> meta;

  @Schema(description = "Data map to inject if keys are not present in the message")
  protected Map<String, Object> dataMap;
}
