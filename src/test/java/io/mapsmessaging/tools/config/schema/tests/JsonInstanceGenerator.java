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

  private static final int MAX_DEPTH = 12;
  private static final int OPTIONAL_INCLUDE_PERCENT = 30;

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
    Set<String> required = readRequired(rootSchema);


    for (String requiredName : required) {
      if ("schemaLoadingVersion".equals(requiredName)) {
        continue;
      }
      mutated.remove(requiredName);
      break;
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
    JsonNode fieldSchema = resolveRef(schemaRoot, properties.get(field));

    String type = singleType(fieldSchema);
    if ("string".equals(type)) {
      mutated.set(field, IntNode.valueOf(123));
      return mutated;
    }

    if ("integer".equals(type) || "number".equals(type)) {
      mutated.set(field, TextNode.valueOf("not-a-" + type));
      return mutated;
    }

    if ("boolean".equals(type)) {
      mutated.set(field, TextNode.valueOf("not-a-boolean"));
      return mutated;
    }

    // object/array: still wrong type
    mutated.set(field, TextNode.valueOf("not-a-" + type));
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
      JsonNode fieldSchema = resolveRef(schemaRoot, properties.get(field));

      JsonNode enumNode = fieldSchema.get("enum");
      if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
        String type = singleType(fieldSchema);
        mutated.set(field, invalidEnumValue(type));
        return mutated;
      }

      if (fieldSchema.has("minimum") || fieldSchema.has("maximum")) {
        String type = singleType(fieldSchema);
        if ("integer".equals(type)) {
          long max = fieldSchema.has("maximum") ? fieldSchema.get("maximum").asLong() : Long.MAX_VALUE;
          mutated.set(field, LongNode.valueOf(safePlusOne(max)));
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
    if (depth > MAX_DEPTH) {
      return NullNode.getInstance();
    }

    JsonNode resolved = resolveRef(schemaRoot, schema);

    // If a default is present, prefer it (keeps tests stable and schema-aligned)
    JsonNode def = resolved.get("default");
    if (def != null && !def.isNull()) {
      return def.deepCopy();
    }

    // Handle oneOf (polymorphism) early
    JsonNode oneOf = resolved.get("oneOf");
    if (oneOf != null && oneOf.isArray() && oneOf.size() > 0) {
      return generateFromOneOf(schemaRoot, resolved, depth);
    }

    String type = singleType(resolved);

    if ("object".equals(type) || resolved.has("properties")) {
      return generateObject(schemaRoot, resolved, depth);
    }

    if ("array".equals(type)) {
      return generateArray(schemaRoot, resolved, depth);
    }

    return generatePrimitive(resolved);
  }

  private ObjectNode generateObject(JsonNode schemaRoot, JsonNode schema, int depth) {
    ObjectNode obj = OBJECT_MAPPER.createObjectNode();

    JsonNode properties = schema.get("properties");
    Set<String> required = readRequired(schema);

    if (properties != null && properties.isObject()) {
      // Inject discriminator/type early if schema defines it
      maybeInjectTypeFromSchema(obj, schema);

      Iterator<String> names = properties.fieldNames();
      while (names.hasNext()) {
        String name = names.next();

        JsonNode propSchema = resolveRef(schemaRoot, properties.get(name));

        boolean isRequired = required.contains(name);
        boolean includeOptional = random.nextInt(100) < OPTIONAL_INCLUDE_PERCENT;

        if (!isRequired && !includeOptional) {
          continue;
        }

        JsonNode value = generateForSchema(schemaRoot, propSchema, depth + 1);
        obj.set(name, value);
      }

      enforceSchemaLoadingVersionIfPresent(obj, properties);
    }

    return obj;
  }

  private ArrayNode generateArray(JsonNode schemaRoot, JsonNode schema, int depth) {
    ArrayNode array = OBJECT_MAPPER.createArrayNode();

    JsonNode items = schema.get("items");
    if (items == null) {
      return array;
    }

    JsonNode itemSchema = resolveRef(schemaRoot, items);

    // Keep stable; you can fuzz later
    int count = 1;
    for (int i = 0; i < count; i++) {
      array.add(generateForSchema(schemaRoot, itemSchema, depth + 1));
    }

    return array;
  }

  private JsonNode generatePrimitive(JsonNode schema) {
    String type = singleType(schema);

    if ("string".equals(type)) {
      return TextNode.valueOf(generateString(schema));
    }
    if ("boolean".equals(type)) {
      return BooleanNode.valueOf(generateBoolean(schema));
    }
    if ("integer".equals(type)) {
      return LongNode.valueOf(generateInteger(schema));
    }
    if ("number".equals(type)) {
      return DoubleNode.valueOf(generateNumber(schema));
    }

    // If unknown: best effort
    return NullNode.getInstance();
  }

  // ------------------------- oneOf + discriminator -------------------------

  private JsonNode generateFromOneOf(JsonNode schemaRoot, JsonNode parentSchema, int depth) {
    JsonNode oneOf = parentSchema.get("oneOf");
    int index = random.nextInt(oneOf.size());

    JsonNode chosenRaw = oneOf.get(index);
    JsonNode chosen = resolveRef(schemaRoot, chosenRaw);

    // Generate the chosen branch
    JsonNode generated = generateForSchema(schemaRoot, chosen, depth);

    if (!generated.isObject()) {
      return generated;
    }

    ObjectNode obj = (ObjectNode) generated;

    // If parent has discriminator, inject it
    JsonNode discriminator = parentSchema.get("discriminator");
    if (discriminator != null && discriminator.isObject()) {
      String propertyName = discriminator.has("propertyName")
          ? discriminator.get("propertyName").asText()
          : "type";

      if (!obj.has(propertyName)) {
        String typeValue = inferDiscriminatorValue(discriminator, chosen);
        obj.put(propertyName, typeValue);
      }
    } else {
      // No discriminator block: still inject type if possible from chosen branch
      maybeInjectTypeFromSchema(obj, chosen);
    }

    // schemaLoadingVersion always 1 (if present)
    JsonNode props = chosen.get("properties");
    if (props != null && props.isObject()) {
      enforceSchemaLoadingVersionIfPresent(obj, props);
    }

    return obj;
  }

  private String inferDiscriminatorValue(JsonNode discriminator, JsonNode chosenSchema) {
    // 1) If chosen schema defines type as const/enum, use that
    JsonNode properties = chosenSchema.get("properties");
    if (properties != null && properties.isObject()) {
      JsonNode typeProp = properties.get("type");
      if (typeProp != null) {
        JsonNode c = typeProp.get("const");
        if (c != null && c.isValueNode()) {
          return c.asText();
        }
        JsonNode e = typeProp.get("enum");
        if (e != null && e.isArray() && e.size() > 0) {
          return e.get(0).asText();
        }
      }
    }

    // 2) If mapping exists, pick first key deterministically
    JsonNode mapping = discriminator.get("mapping");
    if (mapping != null && mapping.isObject()) {
      Iterator<String> keys = mapping.fieldNames();
      if (keys.hasNext()) {
        return keys.next();
      }
    }

    // 3) Last resort
    return "tcp";
  }

  private void maybeInjectTypeFromSchema(ObjectNode obj, JsonNode schema) {
    if (obj.has("type")) {
      return;
    }

    JsonNode properties = schema.get("properties");
    if (properties == null || !properties.isObject()) {
      return;
    }

    JsonNode typeProp = properties.get("type");
    if (typeProp == null) {
      return;
    }

    JsonNode c = typeProp.get("const");
    if (c != null && c.isValueNode()) {
      obj.put("type", c.asText());
      return;
    }

    JsonNode e = typeProp.get("enum");
    if (e != null && e.isArray() && e.size() > 0) {
      obj.put("type", e.get(0).asText());
    }
  }

  private void enforceSchemaLoadingVersionIfPresent(ObjectNode obj, JsonNode properties) {
    if (properties.has("schemaLoadingVersion")) {
      obj.put("schemaLoadingVersion", 1);
    }
  }

  // ------------------------- Value generators -------------------------

  private String generateString(JsonNode schema) {
    JsonNode examples = schema.get("examples");
    if (examples != null && examples.isArray() && examples.size() > 0 && examples.get(0).isTextual()) {
      return examples.get(0).asText();
    }

    JsonNode def = schema.get("default");
    if (def != null && def.isTextual()) {
      return def.asText();
    }

    JsonNode enumNode = schema.get("enum");
    if (enumNode != null && enumNode.isArray() && enumNode.size() > 0 && enumNode.get(0).isTextual()) {
      return enumNode.get(0).asText();
    }

    return "value_" + Math.abs(random.nextInt());
  }

  private boolean generateBoolean(JsonNode schema) {
    JsonNode def = schema.get("default");
    if (def != null && def.isBoolean()) {
      return def.asBoolean();
    }
    return random.nextBoolean();
  }

  private long generateInteger(JsonNode schema) {
    JsonNode enumNode = schema.get("enum");
    if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
      return enumNode.get(0).asLong();
    }

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
    JsonNode enumNode = schema.get("enum");
    if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
      return enumNode.get(0).asDouble();
    }

    double min = schema.has("minimum") ? schema.get("minimum").asDouble() : 0.0;
    double max = schema.has("maximum") ? schema.get("maximum").asDouble() : min + 100.0;
    if (max < min) {
      max = min;
    }
    if (Double.compare(max, min) == 0) {
      return min;
    }

    // Avoid endpoint values for stability
    double v = min + (random.nextDouble() * (max - min));
    return v;
  }

  private JsonNode invalidEnumValue(String type) {
    if ("string".equals(type)) {
      return TextNode.valueOf("___NOT_IN_ENUM___");
    }
    if ("integer".equals(type)) {
      return LongNode.valueOf(Long.MAX_VALUE);
    }
    if ("number".equals(type)) {
      return DoubleNode.valueOf(9.223372036854776E18);
    }
    if ("boolean".equals(type)) {
      return TextNode.valueOf("___NOT_A_BOOLEAN___");
    }
    return TextNode.valueOf("___NOT_IN_ENUM___");
  }

  private long safePlusOne(long value) {
    if (value == Long.MAX_VALUE) {
      return Long.MAX_VALUE;
    }
    return value + 1;
  }

  // ------------------------- Schema helpers -------------------------

  private Set<String> readRequired(JsonNode schema) {
    Set<String> required = new LinkedHashSet<>();
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
    return schemaRoot;
  }

  private JsonNode resolveRef(JsonNode schemaRoot, JsonNode node) {
    if (node == null) {
      return NullNode.getInstance();
    }

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
