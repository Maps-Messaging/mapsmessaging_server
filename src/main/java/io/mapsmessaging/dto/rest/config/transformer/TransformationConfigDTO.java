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

package io.mapsmessaging.dto.rest.config.transformer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.GeoHashResolverTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonQueryTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToValueTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToXmlTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.XmlToJsonTransformationDTO;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = JsonToXmlTransformationDTO.class, name = "json-to-xml"),
    @JsonSubTypes.Type(value = XmlToJsonTransformationDTO.class, name = "xml-to-json"),
    @JsonSubTypes.Type(value = JsonToValueTransformationDTO.class, name = "json-to-value"),
    @JsonSubTypes.Type(value = JsonQueryTransformationDTO.class, name = "json-query"),
    @JsonSubTypes.Type(value = GeoHashResolverTransformationDTO.class, name = "geohash"),
})
@Schema(
    description = "Abstract base class for all transformation configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "json-to-xml", schema = JsonToXmlTransformationDTO.class),
        @DiscriminatorMapping(value = "xml-to-json", schema = XmlToJsonTransformationDTO.class),
        @DiscriminatorMapping(value = "json-to-value", schema = JsonToValueTransformationDTO.class),
        @DiscriminatorMapping(value = "json-query", schema = JsonQueryTransformationDTO.class),
        @DiscriminatorMapping(value = "geohash", schema = GeoHashResolverTransformationDTO.class),
    },
    requiredProperties = {"type"},
    additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
    additionalPropertiesSchema = Object.class,
    schemaResolution = Schema.SchemaResolution.INLINE
)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class TransformationConfigDTO extends BaseConfigDTO {

  public TransformationConfigDTO(TransformationType type) {
    this.type = type;
  }

  @Schema(
      description = "Type of transformation configuration. All values are lower-case and hyphen-separated.",
      example = "json-to-xml",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      implementation = TransformationType.class
  )
  protected TransformationType type;
}
