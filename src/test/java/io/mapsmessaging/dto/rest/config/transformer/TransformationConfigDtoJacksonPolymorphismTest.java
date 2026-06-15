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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMapperTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToSchemaTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.SchemaToJsonTransformationDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformationConfigDtoJacksonPolymorphismTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void jsonMapperDiscriminator_deserializesToConfigurationDto() throws Exception {
    TransformationConfigDTO dto =
        objectMapper.readValue("{\"type\":\"jsonmapper\",\"operations\":[]}", TransformationConfigDTO.class);

    assertInstanceOf(JsonMapperTransformationDTO.class, dto);
    assertEquals(TransformationType.JSON_MAPPER, dto.getType());
  }

  @Test
  void jsonToSchemaDiscriminator_deserializesToConcreteType() throws Exception {
    TransformationConfigDTO dto =
        objectMapper.readValue("{\"type\":\"jsontoschema\",\"schemaName\":\"example\"}", TransformationConfigDTO.class);

    assertInstanceOf(JsonToSchemaTransformationDTO.class, dto);
    assertEquals(TransformationType.JSON_TO_SCHEMA, dto.getType());
  }

  @Test
  void schemaToJsonDiscriminator_deserializesToConcreteType() throws Exception {
    TransformationConfigDTO dto =
        objectMapper.readValue("{\"type\":\"schematojson\"}", TransformationConfigDTO.class);

    assertInstanceOf(SchemaToJsonTransformationDTO.class, dto);
    assertEquals(TransformationType.SCHEMA_TO_JSON, dto.getType());
  }
}
