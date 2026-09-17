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
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttSnConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeJsonSchemaGeneratorPropertyTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void protocolDiscriminatorConstDoesNotRetainConflictingBaseExample() throws Exception {
    JsonNode schema = OBJECT_MAPPER.readTree(
        new RuntimeJsonSchemaGenerator().generateSchema("NetworkManager", NetworkManagerConfigDTO.class));

    String definitionName = MqttSnConfigDTO.class.getName().replace('.', '_');
    JsonNode typeSchema = schema.path("$defs")
        .path(definitionName)
        .path("properties")
        .path("type");

    assertEquals("mqtt-sn", typeSchema.path("const").asText());
    assertEquals("mqtt-sn", typeSchema.path("enum").path(0).asText());
    assertFalse(typeSchema.has("examples"));
    assertFalse(typeSchema.has("default"));
  }

  @Test
  void twinSchemaIncludesJacksonVisibleComputedAndOptionalProperties() throws Exception {
    JsonNode schema = OBJECT_MAPPER.readTree(
        new RuntimeJsonSchemaGenerator().generateSchema("TwinManager", TwinManagerConfigDTO.class));

    String rootRef = schema.path("$ref").asText();
    String definitionName = rootRef.substring("#/$defs/".length());
    JsonNode properties = schema.path("$defs").path(definitionName).path("properties");

    assertTrue(properties.has("cot"));
    assertEquals("array", properties.path("cot").path("type").asText());
    assertTrue(properties.has("n2KTwinConfig"));
    assertTrue(properties.path("n2KTwinConfig").has("anyOf"));
  }
}
