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

import com.google.gson.JsonElement;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMapperTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMutateTransformationDTO;
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

  @Test
  void jsonMapper_readsOperationsDirectlyOnTheEntry() {
    ConfigurationProperties op = new ConfigurationProperties();
    op.put("from", "body.identifier");
    op.put("to", "entity.contains[0].value");
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("type", "jsonmapper");
    props.put("operations", List.of(op));

    JsonMapperTransformationDTO result =
        assertInstanceOf(JsonMapperTransformationDTO.class, TransformationConfigFactory.loadSingle(props));
    assertEquals(1, result.getOperations().size());
    assertEquals("body.identifier", result.getOperations().get(0).getFrom());
    assertEquals("entity.contains[0].value", result.getOperations().get(0).getTo());
  }

  @Test
  void jsonMapper_stillAcceptsLegacyNestedForm() {
    ConfigurationProperties op = new ConfigurationProperties();
    op.put("to", "entity.name");
    op.put("defaultValue", "NSIL_CARD");
    ConfigurationProperties nested = new ConfigurationProperties();
    nested.put("operations", List.of(op));
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("type", "jsonmapper");
    props.put("jsonMapper", nested);

    JsonMapperTransformationDTO result =
        assertInstanceOf(JsonMapperTransformationDTO.class, TransformationConfigFactory.loadSingle(props));
    assertEquals(1, result.getOperations().size());
    assertEquals("entity.name", result.getOperations().get(0).getTo());
  }

  @Test
  void jsonMapper_withNoOperationsDoesNotThrow() {
    assertInstanceOf(JsonMapperTransformationDTO.class,
        TransformationConfigFactory.loadSingle(Map.of("type", "jsonmapper")));
  }

  @Test
  void jsonMutate_setAcceptsBareStringValues() {
    ConfigurationProperties setStatus = new ConfigurationProperties();
    setStatus.put("op", "SET");
    setStatus.put("path", "entity.contains[0].value");
    setStatus.put("value", "NEW");
    ConfigurationProperties setNs = new ConfigurationProperties();
    setNs.put("op", "SET");
    setNs.put("path", "entity.@xmlns:xsi");
    setNs.put("value", "http://www.w3.org/2001/XMLSchema-instance");
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("type", "jsonmutate");
    props.put("operations", List.of(setStatus, setNs));

    JsonMutateTransformationDTO result =
        assertInstanceOf(JsonMutateTransformationDTO.class, TransformationConfigFactory.loadSingle(props));
    assertEquals(2, result.getOperations().size());
    assertEquals("NEW", ((JsonElement) result.getOperations().get(0).getValue()).getAsString());
    assertEquals("http://www.w3.org/2001/XMLSchema-instance",
        ((JsonElement) result.getOperations().get(1).getValue()).getAsString());
  }

  @Test
  void jsonMutate_setStillParsesJsonScalars() {
    ConfigurationProperties setNum = new ConfigurationProperties();
    setNum.put("op", "SET");
    setNum.put("path", "count");
    setNum.put("value", "42");
    ConfigurationProperties setBool = new ConfigurationProperties();
    setBool.put("op", "SET");
    setBool.put("path", "flag");
    setBool.put("value", "true");
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("type", "jsonmutate");
    props.put("operations", List.of(setNum, setBool));

    JsonMutateTransformationDTO result =
        assertInstanceOf(JsonMutateTransformationDTO.class, TransformationConfigFactory.loadSingle(props));
    assertEquals(42, ((JsonElement) result.getOperations().get(0).getValue()).getAsInt());
    assertTrue(((JsonElement) result.getOperations().get(1).getValue()).getAsBoolean());
  }
}
