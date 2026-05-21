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

package io.mapsmessaging.dto.rest.schema;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import io.mapsmessaging.schemas.model.OffsetDateTimeAdapter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Schema(description = "Schema configuration as exposed via the REST API")
public class SchemaConfigDTO {

  @Schema(
      description = "Unique identifier for the schema",
      example = "it_019c21a1-0626-7258-ae03-78fd8247d4f4",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String uniqueId;

  @Schema(
      description = "Schema version identifier",
      example = "1",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String versionId;

  @Schema(
      description = "Epoch value associated with the schema",
      example = "1700000000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private Long epoch;

  @Schema(
      description = "Human-readable schema title",
      example = "Engine Telemetry",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String name;

  @Schema(
      description = "Schema description",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String description;

  @Schema(
      description = "Documentation reference or embedded documentation",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String documentation;

  @Schema(
      description = "Labels/metadata attached to the schema",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private Map<String, String> labels;

  @Schema(
      description = "Unique ID of the ancestor schema (if versioned/derived)",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String ancestor;

  @Schema(
      description = "Schema format identifier",
      example = "json",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String format;

  @Schema(
      description = "Schema URL reference (optional)",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String schemaUrl;

  @Schema(
      description = "Schema definition as JSON. Either schema or schemaBase64 must be provided.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private JsonObject schema;

  @Schema(
      description = "Schema definition encoded as Base64. Either schema or schemaBase64 must be provided.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String schemaBase64;

  @JsonAdapter(OffsetDateTimeAdapter.class)
  @Schema(
      description = "Creation time in ISO-8601 with offset",
      format = "date-time",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private OffsetDateTime createdAt;

  @JsonAdapter(OffsetDateTimeAdapter.class)
  @Schema(
      description = "Last modification time in ISO-8601 with offset",
      format = "date-time",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private OffsetDateTime modifiedAt;

  @JsonAdapter(OffsetDateTimeAdapter.class)
  @Schema(
      description = "Schema is not valid before this time (optional)",
      format = "date-time",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private OffsetDateTime notBefore;

  @JsonAdapter(OffsetDateTimeAdapter.class)
  @Schema(
      description = "Schema expires after this time (optional)",
      format = "date-time",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private OffsetDateTime expiresAfter;
}
