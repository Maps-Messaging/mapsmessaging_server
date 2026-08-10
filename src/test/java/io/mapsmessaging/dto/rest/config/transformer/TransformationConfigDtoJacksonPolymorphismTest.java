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

package io.mapsmessaging.dto.rest.config.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.dto.rest.config.transformer.impl.CloudEventEnvelopeTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.CloudEventJsonTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.CloudEventNativeTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.GeoHashResolverTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMapperTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMutateTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonQueryTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToSchemaTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToValueTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToXmlTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.SchemaToJsonTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.XmlToJsonTransformationDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TransformationConfigDtoJacksonPolymorphismTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @ParameterizedTest
  @MethodSource("transformationConfigurations")
  void discriminator_deserializesToExpectedConfigurationDto(
      String json,
      Class<? extends TransformationConfigDTO> expectedClass,
      TransformationType expectedType) throws Exception {

    TransformationConfigDTO dto = objectMapper.readValue(json, TransformationConfigDTO.class);

    assertInstanceOf(expectedClass, dto);
    assertEquals(expectedType, dto.getType());
  }

  private static Stream<Arguments> transformationConfigurations() {
    return Stream.of(
        Arguments.of(
            "{\"type\":\"jsontoxml\"}",
            JsonToXmlTransformationDTO.class,
            TransformationType.JSON_TO_XML),

        Arguments.of(
            "{\"type\":\"xmltojson\"}",
            XmlToJsonTransformationDTO.class,
            TransformationType.XML_TO_JSON),

        Arguments.of(
            "{\"type\":\"jsontoschema\",\"schemaName\":\"example\"}",
            JsonToSchemaTransformationDTO.class,
            TransformationType.JSON_TO_SCHEMA),

        Arguments.of(
            "{\"type\":\"schematojson\"}",
            SchemaToJsonTransformationDTO.class,
            TransformationType.SCHEMA_TO_JSON),

        Arguments.of(
            "{\"type\":\"jsontovalue\"}",
            JsonToValueTransformationDTO.class,
            TransformationType.JSON_TO_VALUE),

        Arguments.of(
            "{\"type\":\"jsonquery\"}",
            JsonQueryTransformationDTO.class,
            TransformationType.JSON_QUERY),

        Arguments.of(
            "{\"type\":\"geohash\"}",
            GeoHashResolverTransformationDTO.class,
            TransformationType.GEOHASH),

        Arguments.of(
            "{\"type\":\"jsonmutate\",\"operations\":[]}",
            JsonMutateTransformationDTO.class,
            TransformationType.JSON_MUTATE),

        Arguments.of(
            "{\"type\":\"jsonmapper\",\"operations\":[]}",
            JsonMapperTransformationDTO.class,
            TransformationType.JSON_MAPPER),

        Arguments.of(
            "{\"type\":\"cloudevent-envelope\"}",
            CloudEventEnvelopeTransformationDTO.class,
            TransformationType.CLOUD_EVENT_ENVELOPE),

        Arguments.of(
            "{\"type\":\"cloudevent-json\"}",
            CloudEventJsonTransformationDTO.class,
            TransformationType.CLOUD_EVENT_JSON),

        Arguments.of(
            "{\"type\":\"cloudevent-native\"}",
            CloudEventNativeTransformationDTO.class,
            TransformationType.CLOUD_EVENT_NATIVE)
    );
  }
}