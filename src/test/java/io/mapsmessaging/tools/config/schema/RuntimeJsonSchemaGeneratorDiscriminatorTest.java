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

package io.mapsmessaging.tools.config.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.dto.rest.config.NetworkManagerConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeJsonSchemaGeneratorDiscriminatorTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void classLevelSwaggerDiscriminatorMappingPinsConcreteManagerType() throws Exception {
    RuntimeJsonSchemaGenerator generator = new RuntimeJsonSchemaGenerator();

    JsonNode schema = OBJECT_MAPPER.readTree(
        generator.generateSchema("NetworkManager", NetworkManagerConfigDTO.class));
    String rootRef = schema.path("$ref").asText();
    String definitionName = rootRef.substring("#/$defs/".length());
    JsonNode typeSchema = schema.path("$defs")
        .path(definitionName)
        .path("properties")
        .path("type");

    assertEquals("NetworkManagerConfigDTO", typeSchema.path("const").asText());
    assertEquals("NetworkManagerConfigDTO", typeSchema.path("enum").path(0).asText());
  }
}
