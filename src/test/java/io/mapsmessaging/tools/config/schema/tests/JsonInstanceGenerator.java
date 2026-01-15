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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.util.*;

public class JsonInstanceGenerator {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Random random;

  public JsonInstanceGenerator(long seed) {
    this.random = new Random(seed);
  }

  public JsonNode generateValidInstance(JsonNode schemaRoot) {
    JsonNode rootSchema = resolveRootSchema(schemaRoot);
    return generateForSchema(schemaRoot, rootSchema, 0);
  }

  public JsonNode makeInvalidMissingRequired(JsonNode schemaRoot, JsonNode validInstance) {
    JsonNode rootSchema = resolveRootSchema(schemaRoot);
    if (!validInstance.isObject()) {
      return validInstance;
    }

    ObjectNode mutated = ((ObjectNode) validInstance).deepCopy();

    JsonNode requiredNode = rootSchema.get("required");
    if (requiredNode != null && requiredNode.isArray() && requiredNode.size() > 0) {
      String requiredName = requiredNode.get(0).asText();
      mutated.remove(requiredName);
    }
    return mutated;
  }

  public JsonNode makeInvalidWrongType(JsonNode schemaRoot, JsonNode validInstance) {
    JsonNode rootSchema = resolveRootSchema(schemaRoot);
    if (!validInstance.isObject()) {
      return validInstance;
    }

    ObjectNode mutated = ((ObjectNode) validInstance).deepCopy();
    JsonNode properties = rootSchema.get("properties");
    if (properties == null || !properties.isObject()) {
      return mutated;
    }

    Iterator<String> fieldNames = properties.fieldNames();
    if (!fieldNames.hasNext()) {
      return mutated;
    }

    String field = fieldNames.next();
    JsonNode fieldSchema = properties.get(field);
    String type = singleType(fieldSchema);

    // flip type to something incompatible
    if ("string".equals(type)) {
      mutated.set(field, IntNode.valueOf(123));
    } else {
      mutated.set(field, TextNode.valueOf("not-a-" + type));
    }
    return mutated;
  }

  public JsonNode makeInvalidOutOfRangeOrEnum(JsonNode schemaRoot, JsonNode validInstance) {
    JsonNode rootSchema = resolveRootSchema(schemaRoot);
    if (!validInstance.isObject()) {
      return validInstance;
    }

    ObjectNode mutated = ((ObjectNode) validInstance).deepCopy();
    JsonNode properties = rootSchema.get("properties");
    if (properties == null || !properties.isObject()) {
      return mutated;
    }

    for (Iterator<String> it = properties.fieldNames(); it.hasNext(); ) {
      String field = it.next();
      JsonNode fieldSchema = properties.get(field);

      JsonNode enumNode = fieldSchema.get("enum");
      if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
        // set value not in enum
        String type = singleType(fieldSchema);
        if ("string".equals(type)) {
          mutated.set(field, TextNode.valueOf("___NOT_IN_ENUM___"));
        } else if ("integer".equals(type)) {
          mutated.set(field, LongNode.valueOf(Long.MAX_VALUE));
        } else if ("number".equals(type)) {
          mutated.set(field, DoubleNode.valueOf(9.223372036854776E18));
        }
        return mutated;
      }

      if (fieldSchema.has("minimum") || fieldSchema.has("maximum")) {
        String type = singleType(fieldSchema);
        if ("integer".equals(type)) {
          long max = fieldSchema.has("maximum") ? fieldSchema.get("maximum").asLong() : Long.MAX_VALUE;
          mutated.set(field, LongNode.valueOf(max + 1));
          return mutated;
        }
        if ("number".equals(type)) {
          double max = fieldSchema.has("maximum") ? fieldSchema.get("maximum").asDouble() : Double.MAX_VALUE;
          mutated.set(field, DoubleNode.valueOf(max + 1.0));
          return mutated;
        }
      }
    }

    return mutated;
  }

  // ------------------------- Core generation -------------------------

  private JsonNode generateForSchema(JsonNode schemaRoot, JsonNode schema, int depth) {
    if (depth > 20) {
      return NullNode.getInstance();
    }

    // oneOf: choose first option
    JsonNode oneOf = schema.get("oneOf");
    if (oneOf != null && oneOf.isArray() && oneOf.size() > 0) {
      JsonNode chosen = resolveRef(schemaRoot, oneOf.get(0));
      return generateForSchema(schemaRoot, chosen, depth + 1);
    }

    // enum: choose random enum value
    JsonNode enumNode = schema.get("enum");
    if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
      JsonNode choice = enumNode.get(random.nextInt(enumNode.size()));
      return choice.deepCopy();
    }

    String type = singleType(schema);
    if (type == null) {
      // If schema doesn't specify type, best-effort based on structure
      if (schema.has("properties") || schema.has("additionalProperties")) {
        type = "object";
      }
    }

    if ("object".equals(type)) {
      return generateObject(schemaRoot, schema, depth + 1);
    }
    if ("array".equals(type)) {
      return generateArray(schemaRoot, schema, depth + 1);
    }
    if ("string".equals(type)) {
      return TextNode.valueOf(generateString(schema));
    }
    if ("integer".equals(type)) {
      return LongNode.valueOf(generateInteger(schema));
    }
    if ("number".equals(type)) {
      return DoubleNode.valueOf(generateNumber(schema));
    }
    if ("boolean".equals(type)) {
      return BooleanNode.valueOf(random.nextBoolean());
    }

    return NullNode.getInstance();
  }

  private JsonNode generateObject(JsonNode schemaRoot, JsonNode schema, int depth) {
    ObjectNode obj = OBJECT_MAPPER.createObjectNode();

    JsonNode properties = schema.get("properties");
    Set<String> required = readRequired(schema);

    if (properties != null && properties.isObject()) {
      Iterator<String> names = properties.fieldNames();
      while (names.hasNext()) {
        String name = names.next();
        JsonNode propSchema = resolveRef(schemaRoot, properties.get(name));

        boolean isRequired = required.contains(name);
        boolean includeOptional = random.nextInt(100) < 30;

        if (isRequired || includeOptional) {
          JsonNode value = generateForSchema(schemaRoot, propSchema, depth + 1);
          obj.set(name, value);
        }
      }
    }

    // Enforce schemaLoadingVersion == 1 ALWAYS (valid instances)
    if (properties != null && properties.has("schemaLoadingVersion")) {
      obj.put("schemaLoadingVersion", 1);
    }

    return obj;
  }


  private JsonNode generateArray(JsonNode schemaRoot, JsonNode schema, int depth) {
    ArrayNode array = OBJECT_MAPPER.createArrayNode();
    JsonNode items = schema.get("items");
    if (items == null) {
      return array;
    }

    JsonNode itemSchema = resolveRef(schemaRoot, items);
    int count = 1; // keep stable for round-trip comparisons
    for (int i = 0; i < count; i++) {
      array.add(generateForSchema(schemaRoot, itemSchema, depth + 1));
    }
    return array;
  }

  private String generateString(JsonNode schema) {
    JsonNode examples = schema.get("examples");
    if (examples != null && examples.isArray() && examples.size() > 0 && examples.get(0).isTextual()) {
      return examples.get(0).asText();
    }
    JsonNode def = schema.get("default");
    if (def != null && def.isTextual()) {
      return def.asText();
    }
    return "value_" + Math.abs(random.nextInt());
  }

  private long generateInteger(JsonNode schema) {
    long min = schema.has("minimum") ? schema.get("minimum").asLong() : 0;
    long max = schema.has("maximum") ? schema.get("maximum").asLong() : min + 100;
    if (max < min) {
      max = min;
    }
    if (max == min) {
      return min;
    }
    long bound = (max - min) + 1;
    long r = Math.floorMod(random.nextLong(), bound);
    return min + r;
  }

  private double generateNumber(JsonNode schema) {
    double min = schema.has("minimum") ? schema.get("minimum").asDouble() : 0.0;
    double max = schema.has("maximum") ? schema.get("maximum").asDouble() : min + 100.0;
    if (max < min) {
      max = min;
    }
    if (Double.compare(max, min) == 0) {
      return min;
    }
    return min + (random.nextDouble() * (max - min));
  }

  private Set<String> readRequired(JsonNode schema) {
    Set<String> required = new HashSet<>();
    JsonNode requiredNode = schema.get("required");
    if (requiredNode != null && requiredNode.isArray()) {
      for (JsonNode n : requiredNode) {
        if (n.isTextual()) {
          required.add(n.asText());
        }
      }
    }
    return required;
  }

  private String singleType(JsonNode schema) {
    JsonNode type = schema.get("type");
    if (type == null) {
      return null;
    }
    if (type.isTextual()) {
      return type.asText();
    }
    // If you ever emit union types, handle them properly. For now, pick first textual.
    if (type.isArray() && type.size() > 0 && type.get(0).isTextual()) {
      return type.get(0).asText();
    }
    return null;
  }

  private JsonNode resolveRootSchema(JsonNode schemaRoot) {
    JsonNode ref = schemaRoot.get("$ref");
    if (ref != null && ref.isTextual()) {
      return resolveRef(schemaRoot, schemaRoot);
    }
    // else: treat schemaRoot as root schema
    return schemaRoot;
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
    for (String p : parts) {
      current = current.get(p);
      if (current == null) {
        return node;
      }
    }
    return current;
  }
}
