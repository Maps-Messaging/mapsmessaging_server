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
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.dto.rest.config.transformer.impl;

import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashLayout;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashOnMissingPolicy;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashUnits;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Transformation DTO that resolves destination topics based on geo-hash computed from message latitude/longitude.")
public class GeoHashResolverTransformationDTO extends TransformationConfigDTO {

  public GeoHashResolverTransformationDTO() {
    super(TransformationType.GEOHASH);
  }

  @Schema(
      description = "Topic prefix for output destination name.",
      example = "maps/location",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      maxLength = 2048
  )
  protected String prefix = "";

  @Schema(
      description = "Primary latitude key to read from IdentifierResolver.",
      example = "latitude",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minLength = 1,
      maxLength = 2048
  )
  protected String latKey = "latitude";

  @Schema(
      description = "Primary longitude key to read from IdentifierResolver.",
      example = "longitude",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minLength = 1,
      maxLength = 2048
  )
  protected String lonKey = "longitude";

  @Schema(
      description = "GeoHash precision (number of characters).",
      example = "5",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      minimum = "1",
      maximum = "12",
      defaultValue = "5"
  )
  protected int precision = 5;

  @ArraySchema(
      schema = @Schema(
          description = "Fallback latitude keys to try if latKey is missing.",
          example = "lat",
          requiredMode = Schema.RequiredMode.NOT_REQUIRED,
          nullable = true,
          minLength = 1,
          maxLength = 2048
      )
  )
  protected List<String> latKeys = new ArrayList<>();

  @ArraySchema(
      schema = @Schema(
          description = "Fallback longitude keys to try if lonKey is missing.",
          example = "lon",
          requiredMode = Schema.RequiredMode.NOT_REQUIRED,
          nullable = true,
          minLength = 1,
          maxLength = 2048
      )
  )
  protected List<String> lonKeys = new ArrayList<>();

  @Schema(
      description = "Units of the latitude/longitude values.",
      example = "deg",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      implementation = GeoHashUnits.class
  )
  protected GeoHashUnits units = GeoHashUnits.DEG;

  @Schema(
      description = "How the geo-hash is represented in the output topic structure.",
      example = "chars-per-segment",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      implementation = GeoHashLayout.class
  )
  protected GeoHashLayout layout = GeoHashLayout.CHARS_PER_SEGMENT;

  @Schema(
      description = "Behavior when latitude/longitude cannot be extracted.",
      example = "skip",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      implementation = GeoHashOnMissingPolicy.class
  )
  protected GeoHashOnMissingPolicy onMissing = GeoHashOnMissingPolicy.SKIP;

  @Schema(
      description = "Default latitude used only when onMissing is 'default-to'.",
      example = "0.0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minimum = "-90",
      maximum = "90"
  )
  protected Double defaultLatitude;

  @Schema(
      description = "Default longitude used only when onMissing is 'default-to'.",
      example = "0.0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minimum = "-180",
      maximum = "180"
  )
  protected Double defaultLongitude;

  @Schema(
      description = "When true, geo-hash is split into topic segments. " +
          "Deprecated in favour of 'layout'. If 'layout' is set, it takes precedence.",
      example = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      deprecated = true
  )
  protected Boolean splitHash = true;
}
