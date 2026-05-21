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
import io.mapsmessaging.dto.rest.config.transformer.impl.*;
import io.mapsmessaging.dto.rest.config.transformer.jsonmapper.JsonMapOpDTO;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = JsonToXmlTransformationDTO.class, name = "jsontoxml"),
    @JsonSubTypes.Type(value = XmlToJsonTransformationDTO.class, name = "xmltojson"),
    @JsonSubTypes.Type(value = JsonToSchemaTransformationDTO.class, name = "jsontoschema"),
    @JsonSubTypes.Type(value = JsonToValueTransformationDTO.class, name = "jsontovalue"),
    @JsonSubTypes.Type(value = JsonQueryTransformationDTO.class, name = "jsonquery"),
    @JsonSubTypes.Type(value = GeoHashResolverTransformationDTO.class, name = "geohash"),
    @JsonSubTypes.Type(value = JsonMutateTransformationDTO.class, name = "jsonmutate"),
    @JsonSubTypes.Type(value = JsonMapOpDTO.class, name = "jsonmapper"),
    @JsonSubTypes.Type(value = CloudEventEnvelopeTransformationDTO.class, name = "cloudevent-envelope"),
    @JsonSubTypes.Type(value = CloudEventJsonTransformationDTO.class, name = "cloudevent-json"),
    @JsonSubTypes.Type(value = CloudEventNativeTransformationDTO.class, name = "cloudevent-native"),

})
@Schema(
    description = "Abstract base class for all transformation configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "jsontoxml", schema = JsonToXmlTransformationDTO.class),
        @DiscriminatorMapping(value = "xmltojson", schema = XmlToJsonTransformationDTO.class),
        @DiscriminatorMapping(value = "jsontoschema", schema = JsonToSchemaTransformationDTO.class),
        @DiscriminatorMapping(value = "schematojson", schema = SchemaToJsonTransformationDTO.class),
        @DiscriminatorMapping(value = "jsontovalue", schema = JsonToValueTransformationDTO.class),
        @DiscriminatorMapping(value = "jsonquery", schema = JsonQueryTransformationDTO.class),
        @DiscriminatorMapping(value = "geohash", schema = GeoHashResolverTransformationDTO.class),
        @DiscriminatorMapping(value = "jsonmutate", schema = JsonMutateTransformationDTO.class),
        @DiscriminatorMapping(value = "jsonmapper", schema = JsonMapOpDTO.class),
        @DiscriminatorMapping(value = "cloudevent-envelope", schema = CloudEventEnvelopeTransformationDTO.class),
        @DiscriminatorMapping(value = "cloudevent-json", schema = CloudEventJsonTransformationDTO.class),
        @DiscriminatorMapping(value = "cloudevent-native", schema = CloudEventNativeTransformationDTO.class)
    },
    requiredProperties = {"type"},
    additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
    additionalPropertiesSchema = Object.class
)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class TransformationConfigDTO {

  public TransformationConfigDTO(TransformationType type) {
    this.type = type;
  }

  @Schema(
      description = "Type of transformation configuration.",
      allowableValues = {
          "cloudevent-json",
          "cloudevent-native",
          "cloudevent-envelope",
          "jsontoxml",
          "xmltojson",
          "jsontovalue",
          "jsonquery",
          "geohash",
          "schematojson",
          "jsonmutate",
          "jsonmapper",
          "jsontoschema"
      },
      example = "jsontoxml",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected TransformationType type;
}
