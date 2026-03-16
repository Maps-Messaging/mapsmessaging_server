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

package io.mapsmessaging.dto.rest.config.protocol;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Link Configuration DTO")
public class LinkConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Direction of the link",
      example = "pull",
      allowableValues = {"pull", "push"},
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String direction;

  @Schema(
      description = "Remote namespace (source). Typically a topic/namespace filter. " +
          "For MQTT-style namespaces, + and # may be used as wildcards.",
      example = "/+/1/1/GPS_RAW_INT",
      minLength = 1,
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String remoteNamespace;

  @Schema(
      description = "Local namespace (destination). Typically a topic/namespace.",
      example = "/",
      minLength = 1,
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String localNamespace;

  @Schema(
      description = "Message selector expression (JMS selector syntax). If not set, all messages match.",
      example = "temperature > 30 AND humidityPercent < 70",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String selector;

  @Schema(
      description = "If true, include schema information when forwarding messages (where supported)",
      example = "true",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean includeSchema = false;

  @Schema(
      description = "Transformer chain configuration (array of objects). Each entry specifies transformer name and parameters.",
      type = "array",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "[{\"name\":\"JsonQuery\",\"parameters\":{\"query\":\"[\\\"object\\\",{\\\"latitude\\\":[\\\"divide\\\",[\\\"get\\\",\\\"payload\\\",\\\"decoded\\\",\\\"lat\\\"],10000000]}]\"}}]"
  )
  protected List<TransformationConfigDTO> transformer;

  @Schema(
      description = "Configure statistical analysis of data flowing through the link",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected StatisticsConfigDTO statistics;

  @Schema(
      description = "Specific filtering applied to namespaces",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected List<NamespaceFilterDTO> namespaceFilters;

  @Schema(
      description = "Requested QoS for the link.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      type = "string",
      allowableValues = {
          "AT_MOST_ONCE",
          "AT_LEAST_ONCE",
          "EXACTLY_ONCE",
          "MQTT_SN_REGISTERED"
      },
      example = "AT_LEAST_ONCE"
  )
  protected QualityOfService qualityOfService;
}
