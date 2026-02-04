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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


class JsonSchemaRoundTripTest {

  private static final Map<String, Class<?>> dtoMap = buildMap();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
      .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

  private final JsonSchemaValidator validator;

  public JsonSchemaRoundTripTest() {
    this.validator = new NetworkNtJsonSchemaValidator(); // replace with real impl
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
    map.put("DiscoveryManager", DiscoveryManagerConfigDTO.class);
    map.put("License", LicenseManagerConfigDTO.class);
    map.put("LoRaDevice",  LoRaDeviceManagerConfigDTO.class);
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
    assertNotNull(dtoClass, () -> schemaName + " has no DTO mapping");

    JsonNode schemaNode = parse(schemaText);

    // b) Build a valid JSON instance from schema
    JsonInstanceGenerator generator = new JsonInstanceGenerator(seed);
    JsonNode inputJson = generator.generateValidInstance(schemaNode);

    // Validate input JSON against schema
    List<String> inputErrors = validator.validate(schemaNode, inputJson);
    assertTrue(inputErrors.isEmpty(), () -> schemaName + " valid JSON failed schema: " + inputErrors + "\nJSON: " + inputJson);

    // c) JSON -> DTO1
    Object dto1;
    try {
      dto1 = OBJECT_MAPPER.treeToValue(inputJson, dtoClass);
    } catch (Exception e) {
      fail(schemaName + " DTO generation failure (" + dtoClass.getName() + "): " + e.getMessage() + "\nJSON: " + inputJson, e);
      return;
    }

    // d) DTO1 -> JSON
    JsonNode outputJson;
    try {
      outputJson = OBJECT_MAPPER.valueToTree(dto1);
    } catch (Exception e) {
      fail(schemaName + " JSON generation from DTO failure: " + e.getMessage(), e);
      return;
    }

    // Validate DTO-produced JSON against schema (still useful)
    List<String> outputErrors = validator.validate(schemaNode, outputJson);
    assertTrue(outputErrors.isEmpty(), () -> schemaName + " DTO->JSON failed schema: " + outputErrors + "\nJSON: " + outputJson);

    // e) JSON -> DTO2
    Object dto2;
    try {
      dto2 = OBJECT_MAPPER.treeToValue(outputJson, dtoClass);
    } catch (Exception e) {
      fail(schemaName + " DTO regeneration failure (" + dtoClass.getName() + "): " + e.getMessage() + "\nJSON: " + outputJson, e);
      return;
    }

    if (!dto1.equals(dto2)) {
      JsonNode dto1Json = OBJECT_MAPPER.valueToTree(dto1);
      JsonNode dto2Json = OBJECT_MAPPER.valueToTree(dto2);

      List<String> diffs = JsonDiff.diff(dto1Json, dto2Json);
      fail(schemaName + " DTO round-trip changed values:\n" + String.join("\n", diffs)
          + "\nDTO1 JSON: " + dto1Json
          + "\nDTO2 JSON: " + dto2Json);
    }
  }


  private void runInvalidMissingRequired(String schemaName, String schemaText, Class<?> dtoClass, long seed) throws Exception {
    JsonNode schemaNode = parse(schemaText);

    JsonInstanceGenerator generator = new JsonInstanceGenerator(seed);
    JsonNode valid = generator.generateValidInstance(schemaNode);

    JsonNode invalid = generator.makeInvalidMissingRequired(schemaNode, valid);
    if(!invalid.equals(valid)) {
      List<String> errors = validator.validate(schemaNode, invalid);
      assertFalse(errors.isEmpty(), () -> schemaName + " expected missing-required to fail schema validation");
    }
  }

  private void runInvalidWrongType(String schemaName, String schemaText, Class<?> dtoClass, long seed) throws Exception {
    JsonNode schemaNode = parse(schemaText);

    JsonInstanceGenerator generator = new JsonInstanceGenerator(seed);
    JsonNode valid = generator.generateValidInstance(schemaNode);
    JsonNode invalid = generator.makeInvalidWrongType(schemaNode, valid);
    if(!invalid.equals(valid)) {
      List<String> errors = validator.validate(schemaNode, invalid);
      assertFalse(errors.isEmpty(), () -> schemaName + " expected wrong-type to fail schema validation");
    }
  }

  private void runInvalidOutOfRangeOrEnum(String schemaName, String schemaText, Class<?> dtoClass, long seed) throws Exception {
    JsonNode schemaNode = parse(schemaText);

    JsonInstanceGenerator generator = new JsonInstanceGenerator(seed);
    JsonNode valid = generator.generateValidInstance(schemaNode);

    JsonNode invalid = generator.makeInvalidOutOfRangeOrEnum(schemaNode, valid);
    if(!invalid.equals(valid)) {
      List<String> errors = validator.validate(schemaNode, invalid);
      assertFalse(errors.isEmpty(), () -> schemaName + " expected out-of-range/enum to fail schema validation");
    }
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


  public final class NetworkNtJsonSchemaValidator implements JsonSchemaRoundTripTest.JsonSchemaValidator {


    private final SchemaRegistry schemaRegistry;

    public NetworkNtJsonSchemaValidator() {
      this.schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012());
    }

    @Override
    public List<String> validate(JsonNode schemaNode, JsonNode instance) {
      JsonNode effectiveSchema = resolveTopLevelRef(schemaNode);

      Schema schema = schemaRegistry.getSchema(effectiveSchema);

      // instance is already a JsonNode; no need to re-materialize it
      List<Error> errors = schema.validate(instance);

      return errors.stream()
          .map(Error::getMessage)
          .collect(Collectors.toList());
    }

    private JsonNode resolveTopLevelRef(JsonNode schemaRoot) {
      JsonNode refNode = schemaRoot.get("$ref");
      if (refNode == null || !refNode.isTextual()) {
        return schemaRoot;
      }

      JsonNode resolved = resolveRef(schemaRoot, schemaRoot);
      if (resolved == null || !resolved.isObject()) {
        return schemaRoot;
      }

      // Merge: resolved subschema + carry over $defs so nested refs keep working
      ObjectNode merged = ((ObjectNode) resolved).deepCopy();

      JsonNode defs = schemaRoot.get("$defs");
      if (defs != null && defs.isObject()) {
        merged.set("$defs", defs);
      }

      JsonNode schemaDecl = schemaRoot.get("$schema");
      if (schemaDecl != null) {
        merged.set("$schema", schemaDecl);
      }

      // Keep $id if present (helps some validators that use ids internally)
      JsonNode id = schemaRoot.get("$id");
      if (id != null) {
        merged.set("$id", id);
      }

      return merged;
    }

    private JsonNode resolveRef(JsonNode schemaRoot, JsonNode node) {
      JsonNode ref = node.get("$ref");
      if (ref == null || !ref.isTextual()) {
        return node;
      }

      String refText = ref.asText();
      if (!refText.startsWith("#/")) {
        return node;
      }

      String[] parts = refText.substring(2).split("/");
      JsonNode current = schemaRoot;

      for (String part : parts) {
        current = current.get(part);
        if (current == null) {
          return node;
        }
      }

      return current;
    }
  }
  static final class JsonDiff {

    static List<String> diff(JsonNode left, JsonNode right) {
      List<String> out = new ArrayList<>();
      diffInto("$", left, right, out);
      return out;
    }

    private static void diffInto(String path, JsonNode left, JsonNode right, List<String> out) {
      if (left == null && right == null) {
        return;
      }
      if (left == null || right == null) {
        out.add(path + " one side is null. left=" + left + " right=" + right);
        return;
      }

      if (!left.getNodeType().equals(right.getNodeType())) {
        out.add(path + " type differs. left=" + left.getNodeType() + " right=" + right.getNodeType()
            + " leftVal=" + left + " rightVal=" + right);
        return;
      }

      if (left.isObject()) {
        Set<String> names = new TreeSet<>();
        left.fieldNames().forEachRemaining(names::add);
        right.fieldNames().forEachRemaining(names::add);

        for (String name : names) {
          JsonNode l = left.get(name);
          JsonNode r = right.get(name);
          diffInto(path + "." + name, l, r, out);
        }
        return;
      }

      if (left.isArray()) {
        int max = Math.max(left.size(), right.size());
        for (int i = 0; i < max; i++) {
          JsonNode l = i < left.size() ? left.get(i) : null;
          JsonNode r = i < right.size() ? right.get(i) : null;
          diffInto(path + "[" + i + "]", l, r, out);
        }
        return;
      }

      // value nodes
      if (!left.equals(right)) {
        out.add(path + " value differs. left=" + left + " right=" + right);
      }
    }
  }

}
