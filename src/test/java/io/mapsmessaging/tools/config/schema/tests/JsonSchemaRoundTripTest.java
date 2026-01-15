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

package io.mapsmessaging.tools.config.schema.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.mapsmessaging.dto.rest.auth.SecurityManagerDTO;
import io.mapsmessaging.dto.rest.config.*;
import io.mapsmessaging.dto.rest.config.lora.LoRaDeviceConfigDTO;
import io.mapsmessaging.dto.rest.config.ml.MLModelManagerDTO;
import io.mapsmessaging.dto.rest.schema.SchemaManagerConfigDTO;
import io.mapsmessaging.tools.config.schema.RuntimeJsonSchemaGenerator;
import io.mapsmessaging.tools.config.schema.RuntimeJsonSchemaService;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


class JsonSchemaRoundTripTest {

  private static Map<String, Class<?>> dtoMap = buildMap();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
      .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

  private final JsonSchemaValidator validator;

  public JsonSchemaRoundTripTest() {
    this.validator = new NoopJsonSchemaValidator(); // replace with real impl
  }

  @TestFactory
  public Stream<DynamicTest> roundTripAllSchemas() {
    // a) Create schemas
    RuntimeJsonSchemaGenerator generator = new RuntimeJsonSchemaGenerator();
    RuntimeJsonSchemaService service = new RuntimeJsonSchemaService(generator);
    Map<String, String> schemas = service.generateAllSchemas();

    // Deterministic randomness (so failures are reproducible)
    long seed = 123456789L;

    return schemas.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .flatMap(entry -> {
          String schemaName = entry.getKey();
          String schemaText = entry.getValue();

          Class<?> dtoClass =dtoMap.get(schemaName);
          return Stream.of(
              DynamicTest.dynamicTest(schemaName + " :: valid round-trip", () ->
                  runValidRoundTrip(schemaName, schemaText, dtoClass, seed)),
              DynamicTest.dynamicTest(schemaName + " :: invalid (missing required)", () ->
                  runInvalidMissingRequired(schemaName, schemaText, dtoClass, seed)),
              DynamicTest.dynamicTest(schemaName + " :: invalid (wrong type)", () ->
                  runInvalidWrongType(schemaName, schemaText, dtoClass, seed)),
              DynamicTest.dynamicTest(schemaName + " :: invalid (out of range/enum)", () ->
                  runInvalidOutOfRangeOrEnum(schemaName, schemaText, dtoClass, seed))
          );
        });

  }

  private static Map<String, Class<?>> buildMap(){
    Map<String, Class<?>> map = new HashMap<>();
    map.put("AuthManager", AuthManagerConfigDTO.class);
    map.put("DestinationManager",  DestinationManagerConfigDTO.class);
    map.put("DeviceManager", DeviceManagerConfigDTO.class);
    map.put("License", LicenseManagerConfigDTO.class);
    map.put("LoRaDevice",  LoRaDeviceConfigDTO.class);
    map.put("MLModelManager", MLModelManagerDTO.class);
    map.put("MessageDaemon", MessageDaemonConfigDTO.class);
    map.put("NetworkConnectionManager", NetworkConnectionManagerConfigDTO.class);
    map.put("NetworkManager",  NetworkManagerConfigDTO.class);
    map.put("RestApi", RestApiManagerConfigDTO.class);
    map.put("SchemaManager", SchemaManagerConfigDTO.class);
    map.put("SecurityManager", SecurityManagerDTO.class);
    map.put("TenantManagement", TenantManagementConfigDTO.class);
    map.put("jolokia", JolokiaConfigDTO.class);
    map.put("routing", RoutingManagerConfigDTO.class);
    return map;
  }
  private void runValidRoundTrip(String schemaName, String schemaText, Class<?> dtoClass, long seed) throws Exception {
    JsonNode schemaNode = parse(schemaText);

    // b) Build a valid JSON instance from schema
    JsonInstanceGenerator generator = new JsonInstanceGenerator(seed);
    JsonNode inputJson = generator.generateValidInstance(schemaNode);

    // validate JSON against schema
    List<String> inputErrors = validator.validate(schemaNode, inputJson);
    assertTrue(inputErrors.isEmpty(), () -> schemaName + " valid JSON failed schema: " + inputErrors);

    // c) JSON -> DTO
    Object dto;
    try {
      System.err.println(schemaText);
      System.err.println(inputJson);
      dto = OBJECT_MAPPER.treeToValue(inputJson, dtoClass);
    } catch (Exception e) {
      fail(schemaName + " DTO generation failure (" + dtoClass.getName() + "): " + e.getMessage(), e);
      return;
    }

    // d) DTO -> JSON
    JsonNode outputJson;
    try {
      outputJson = OBJECT_MAPPER.valueToTree(dto);
    } catch (Exception e) {
      fail(schemaName + " JSON generation from DTO failure: " + e.getMessage(), e);
      return;
    }

    // validate DTO-produced JSON against schema
    List<String> outputErrors = validator.validate(schemaNode, outputJson);
    assertTrue(outputErrors.isEmpty(), () -> schemaName + " DTO->JSON failed schema: " + outputErrors);

    // e) Compare canonical JSON
    // NOTE: to make this strict, our generator emits all required fields and (where possible) optional defaults explicitly.
    assertEquals(inputJson, outputJson, () -> schemaName + " round-trip changed JSON.\nIN : " + inputJson + "\nOUT: " + outputJson);
  }

  private void runInvalidMissingRequired(String schemaName, String schemaText, Class<?> dtoClass, long seed) throws Exception {
    JsonNode schemaNode = parse(schemaText);

    JsonInstanceGenerator generator = new JsonInstanceGenerator(seed);
    JsonNode valid = generator.generateValidInstance(schemaNode);

    JsonNode invalid = generator.makeInvalidMissingRequired(schemaNode, valid);

    List<String> errors = validator.validate(schemaNode, invalid);
    assertFalse(errors.isEmpty(), () -> schemaName + " expected missing-required to fail schema validation");
  }

  private void runInvalidWrongType(String schemaName, String schemaText, Class<?> dtoClass, long seed) throws Exception {
    JsonNode schemaNode = parse(schemaText);

    JsonInstanceGenerator generator = new JsonInstanceGenerator(seed);
    JsonNode valid = generator.generateValidInstance(schemaNode);

    JsonNode invalid = generator.makeInvalidWrongType(schemaNode, valid);

    List<String> errors = validator.validate(schemaNode, invalid);
    assertFalse(errors.isEmpty(), () -> schemaName + " expected wrong-type to fail schema validation");
  }

  private void runInvalidOutOfRangeOrEnum(String schemaName, String schemaText, Class<?> dtoClass, long seed) throws Exception {
    JsonNode schemaNode = parse(schemaText);

    JsonInstanceGenerator generator = new JsonInstanceGenerator(seed);
    JsonNode valid = generator.generateValidInstance(schemaNode);

    JsonNode invalid = generator.makeInvalidOutOfRangeOrEnum(schemaNode, valid);

    List<String> errors = validator.validate(schemaNode, invalid);
    assertFalse(errors.isEmpty(), () -> schemaName + " expected out-of-range/enum to fail schema validation");
  }

  private static JsonNode parse(String json) throws JsonProcessingException {
    return OBJECT_MAPPER.readTree(json);
  }

  // ------------------------- Registry -------------------------

  public interface SchemaToDtoRegistry {
    Class<?> getDtoClass(String schemaName);
  }

  public static final class MapBackedSchemaToDtoRegistry implements SchemaToDtoRegistry {
    private final Map<String, Class<?>> mapping;

    public MapBackedSchemaToDtoRegistry(Map<String, Class<?>> mapping) {
      this.mapping = new HashMap<>(mapping);
    }

    @Override
    public Class<?> getDtoClass(String schemaName) {
      return mapping.get(schemaName);
    }
  }

  // ------------------------- Validator (pluggable) -------------------------

  public interface JsonSchemaValidator {
    List<String> validate(JsonNode schema, JsonNode instance);
  }

  /**
   * Replace this with a real validator implementation.
   * This is here so the framework compiles while you wire your chosen schema validator.
   */
  public static final class NoopJsonSchemaValidator implements JsonSchemaValidator {
    @Override
    public List<String> validate(JsonNode schema, JsonNode instance) {
      return Collections.emptyList();
    }
  }
}
