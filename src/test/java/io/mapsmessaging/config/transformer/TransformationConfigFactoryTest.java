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

package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonQueryTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToSchemaTransformationDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformationConfigFactoryTest {

  @Test
  void loadChain_returnsEmptyListForNull() {
    assertTrue(TransformationConfigFactory.loadChain(null).isEmpty());
  }

  @Test
  void loadChain_acceptsMapEntriesAndPreservesOrder() {
    List<TransformationConfigDTO> result = TransformationConfigFactory.loadChain(List.of(
        Map.of("type", "jsontoxml"),
        Map.of("type", "xmltojson")));

    assertEquals(List.of(TransformationType.JSON_TO_XML, TransformationType.XML_TO_JSON),
        result.stream().map(TransformationConfigDTO::getType).toList());
  }

  @Test
  void loadSingle_acceptsLegacyNameAndNestedParameters() {
    ConfigurationProperties parameters = new ConfigurationProperties();
    parameters.put("query", "$.position.latitude");
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "jsonquery");
    properties.put("parameters", parameters);

    TransformationConfigDTO result = TransformationConfigFactory.loadSingle(properties);

    JsonQueryTransformationDTO query = assertInstanceOf(JsonQueryTransformationDTO.class, result);
    assertEquals("$.position.latitude", query.getQuery());
  }

  @Test
  void loadSingle_buildsJsonToSchemaFromNestedParameters() {
    ConfigurationProperties parameters = new ConfigurationProperties();
    parameters.put("schema", "telemetry");
    parameters.put("format", "avro");
    parameters.put("messageName", "Position");

    JsonToSchemaTransformationDTO result = assertInstanceOf(JsonToSchemaTransformationDTO.class,
        TransformationConfigFactory.loadSingle(Map.of("type", "jsontoschema", "parameters", parameters)));

    assertEquals("telemetry", result.getSchemaName());
    assertEquals("avro", result.getFormat());
    assertEquals("Position", result.getMessageName());
  }

  @Test
  void loadSingle_rejectsNullUnsupportedMissingAndUnknownEntries() {
    assertThrows(IllegalArgumentException.class, () -> TransformationConfigFactory.loadSingle(null));
    assertThrows(IllegalArgumentException.class, () -> TransformationConfigFactory.loadSingle("json-to-xml"));
    assertThrows(IllegalArgumentException.class, () -> TransformationConfigFactory.loadSingle(Map.of("query", "$")));
    assertThrows(IllegalArgumentException.class, () -> TransformationConfigFactory.loadSingle(Map.of("type", "unknown")));
  }
}
