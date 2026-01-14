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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.tools.config.lint.ReflectionFields;
import io.mapsmessaging.tools.config.lint.ReflectionTypes;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

public class RuntimeJsonSchemaGenerator {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String TYPE_FIELD_NAME = "type";

  private final SchemaGenerationMode mode;
  private final boolean emitWarningsInSchema;

  public RuntimeJsonSchemaGenerator() {
    this(SchemaGenerationMode.RELAXED, false);
  }

  public RuntimeJsonSchemaGenerator(SchemaGenerationMode mode, boolean emitWarningsInSchema) {
    this.mode = mode;
    this.emitWarningsInSchema = emitWarningsInSchema;
  }

  public String generateSchema(String configName, Class<? extends BaseConfigDTO> rootDtoClass) {
    SchemaContext context = new SchemaContext(configName);

    DiscoveryResult discoveryResult = discoverDtoClosureAndDiscriminators(rootDtoClass);
    Set<Class<?>> dtoClosure = discoveryResult.dtoClosure;
    Map<Class<?>, String> discriminatorConstByClass = discoveryResult.discriminatorConstByClass;

    for (Class<?> dtoClass : sortClasses(dtoClosure)) {
      context.ensureDefName(dtoClass);
    }

    for (Class<?> dtoClass : sortClasses(dtoClosure)) {
      SchemaObject defSchema = buildDtoDefinition(dtoClass, context, discriminatorConstByClass);
      context.putDef(dtoClass, defSchema);
    }

    String rootRef = "#/$defs/" + context.defName(rootDtoClass);

    SchemaDocument doc = new SchemaDocument();
    doc.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    doc.put("$id", "urn:mapsmessaging:config-schema:" + configName);
    doc.put("title", configName);

    // IMPORTANT:
    // Do NOT set additionalProperties:false on the wrapper document when $ref is used.
    // Put object-closure rules on the referenced object schemas instead.
    doc.put("unevaluatedProperties", false);

    Schema rootSchemaAnn = rootDtoClass.getAnnotation(Schema.class);
    if (rootSchemaAnn != null && !rootSchemaAnn.description().isBlank()) {
      doc.put("description", rootSchemaAnn.description());
    }

    doc.put("$ref", rootRef);
    doc.put("$defs", context.buildDefsObject());

    if (emitWarningsInSchema && !context.warnings().isEmpty()) {
      doc.put("x-warnings", context.warnings());
    }

    return DeterministicJsonWriter.write(doc.toJsonValue());
  }

  private static final class DiscoveryResult {

    private final Set<Class<?>> dtoClosure;
    private final Map<Class<?>, String> discriminatorConstByClass;

    private DiscoveryResult(Set<Class<?>> dtoClosure, Map<Class<?>, String> discriminatorConstByClass) {
      this.dtoClosure = dtoClosure;
      this.discriminatorConstByClass = discriminatorConstByClass;
    }
  }

  private DiscoveryResult discoverDtoClosureAndDiscriminators(Class<?> rootDto) {
    Set<Class<?>> visited = new HashSet<>();
    Deque<Class<?>> stack = new ArrayDeque<>();

    Map<Class<?>, String> discriminatorConstByClass = new HashMap<>();

    stack.push(rootDto);

    while (!stack.isEmpty()) {
      Class<?> clazz = stack.pop();
      if (clazz == null || clazz == Void.class) {
        continue;
      }
      if (!visited.add(clazz)) {
        continue;
      }

      for (Field field : ReflectionFields.getAllInstanceFields(clazz)) {

        Schema fieldSchemaAnn = field.getAnnotation(Schema.class);
        if (fieldSchemaAnn != null && !fieldSchemaAnn.hidden()) {

          Class<?>[] oneOf = fieldSchemaAnn.oneOf();
          if (oneOf != null && oneOf.length > 0) {
            for (Class<?> c : oneOf) {
              if (c != null && c != Void.class) {
                stack.push(c);
              }
            }
          }

          DiscriminatorMapping[] discriminatorMappings = fieldSchemaAnn.discriminatorMapping();
          if (discriminatorMappings != null && discriminatorMappings.length > 0) {
            for (DiscriminatorMapping dm : discriminatorMappings) {
              if (dm == null) {
                continue;
              }
              Class<?> schemaClass = dm.schema();
              if (schemaClass == null || schemaClass == Void.class) {
                continue;
              }

              String value = dm.value();
              if (value != null && !value.isBlank()) {
                // Invert mapping: subtype class -> discriminator value
                discriminatorConstByClass.put(schemaClass, value);
              }

              stack.push(schemaClass);
            }
          }
        }

        Class<?> rawType = field.getType();

        if (isCollection(rawType)) {
          Type genericType = field.getGenericType();
          if (!(genericType instanceof ParameterizedType)) {
            continue;
          }

          ParameterizedType parameterizedType = (ParameterizedType) genericType;
          Type[] args = parameterizedType.getActualTypeArguments();

          if (List.class.isAssignableFrom(rawType)) {
            if (args.length == 1) {
              Class<?> itemClass = ReflectionTypes.toClass(args[0]);
              if (itemClass != null && BaseConfigDTO.class.isAssignableFrom(itemClass)) {
                stack.push(itemClass);
              }
            }
          }
          else if (Map.class.isAssignableFrom(rawType)) {
            if (args.length == 2) {
              Class<?> valueClass = ReflectionTypes.toClass(args[1]);
              if (valueClass != null && BaseConfigDTO.class.isAssignableFrom(valueClass)) {
                stack.push(valueClass);
              }
            }
          }

          continue;
        }

        if (BaseConfigDTO.class.isAssignableFrom(rawType)) {
          stack.push(rawType);
        }
      }

      JsonTypeInfo typeInfo = clazz.getAnnotation(JsonTypeInfo.class);
      JsonSubTypes subTypes = clazz.getAnnotation(JsonSubTypes.class);
      if (typeInfo != null && subTypes != null) {
        for (JsonSubTypes.Type subtype : subTypes.value()) {
          Class<?> subtypeClass = subtype.value();
          if (subtypeClass != null && subtypeClass != Void.class) {
            stack.push(subtypeClass);
          }
        }
      }
    }

    return new DiscoveryResult(visited, discriminatorConstByClass);
  }

  private SchemaObject buildDtoDefinition(
      Class<?> dtoClass,
      SchemaContext context,
      Map<Class<?>, String> discriminatorConstByClass
  ) {
    context.enterDto(dtoClass);
    try {
      SchemaObject poly = buildJacksonPolymorphicDefinitionIfNeeded(dtoClass, context);
      if (poly != null) {
        return poly;
      }

      SchemaObject schema = new SchemaObject();
      schema.put("type", "object");

      // Close every DTO object. This is where additionalProperties belongs.
      schema.put("additionalProperties", false);

      Schema classSchema = dtoClass.getAnnotation(Schema.class);
      if (classSchema != null && !classSchema.description().isBlank()) {
        schema.put("description", classSchema.description());
      }

      Map<String, Object> properties = new LinkedHashMap<>();
      SortedSet<String> required = new TreeSet<>();

      for (Field field : ReflectionFields.getAllInstanceFields(dtoClass)) {
        context.enterField(field);
        try {
          Schema fieldSchemaAnn = field.getAnnotation(Schema.class);
          if (fieldSchemaAnn == null || fieldSchemaAnn.hidden()) {
            continue;
          }

          String name = field.getName();
          SchemaObject propertySchema = schemaForField(field, context);

          applySwaggerSchema(propertySchema, fieldSchemaAnn, field.getType(), context);

          // If this DTO is a discriminator-mapped subtype, force its "type" field to a const.
          if (TYPE_FIELD_NAME.equals(name)) {
            String discriminatorValue = discriminatorConstByClass.get(dtoClass);
            if (discriminatorValue != null && !discriminatorValue.isBlank()) {
              propertySchema.put("const", discriminatorValue);
              propertySchema.put("enum", List.of(discriminatorValue));
            }
          }

          if (fieldSchemaAnn.requiredMode() == Schema.RequiredMode.REQUIRED) {
            required.add(name);
          }

          properties.put(name, propertySchema.toJsonValue());
        }
        catch (RuntimeException e) {
          if (mode == SchemaGenerationMode.STRICT) {
            throw context.error("Failed while generating field schema", e);
          }
          context.warn("RELAXED: " + e.getMessage());
          String name = field.getName();
          properties.put(
              name,
              unsupportedPlaceholder("Field schema failed", field.getGenericType().getTypeName()).toJsonValue()
          );
        }
        finally {
          context.exitField();
        }
      }

      schema.put("properties", properties);
      if (!required.isEmpty()) {
        schema.put("required", new ArrayList<>(required));
      }

      return schema;
    }
    finally {
      context.exitDto();
    }
  }

  private SchemaObject buildJacksonPolymorphicDefinitionIfNeeded(Class<?> dtoClass, SchemaContext context) {
    JsonTypeInfo typeInfo = dtoClass.getAnnotation(JsonTypeInfo.class);
    JsonSubTypes subTypes = dtoClass.getAnnotation(JsonSubTypes.class);
    if (typeInfo == null || subTypes == null) {
      return null;
    }

    String discriminatorProperty = typeInfo.property();
    if (discriminatorProperty == null || discriminatorProperty.isBlank()) {
      throw new IllegalStateException("@JsonTypeInfo.property() must be set for " + dtoClass.getName());
    }

    List<JsonSubTypes.Type> subtypeList = Arrays.asList(subTypes.value());
    subtypeList.sort(Comparator.comparing(t -> t.value().getName()));

    List<Object> oneOf = new ArrayList<>();
    Map<String, Object> mapping = new LinkedHashMap<>();

    for (JsonSubTypes.Type subtype : subtypeList) {
      Class<?> subtypeClass = subtype.value();
      if (subtypeClass == null || !dtoClass.isAssignableFrom(subtypeClass)) {
        continue;
      }

      String ref = "#/$defs/" + context.defName(subtypeClass);
      oneOf.add(SchemaObject.ref(ref).toJsonValue());

      String name = subtype.name();
      String discriminatorValue = (name == null || name.isBlank())
          ? subtypeClass.getSimpleName()
          : name;

      mapping.put(discriminatorValue, ref);
    }

    SchemaObject schema = new SchemaObject();
    schema.put("oneOf", oneOf);

    Map<String, Object> discriminator = new LinkedHashMap<>();
    discriminator.put("propertyName", discriminatorProperty);
    discriminator.put("mapping", mapping);
    schema.put("discriminator", discriminator);

    return schema;
  }

  private SchemaObject schemaForField(Field field, SchemaContext context) {
    Schema fieldSchemaAnn = field.getAnnotation(Schema.class);
    SchemaObject swaggerPoly = buildSwaggerPolymorphicFieldSchemaIfPresent(fieldSchemaAnn, context);
    if (swaggerPoly != null) {
      return swaggerPoly;
    }

    Class<?> rawType = field.getType();

    if (List.class.isAssignableFrom(rawType)) {
      Type genericType = field.getGenericType();
      if (!(genericType instanceof ParameterizedType)) {
        return unsupportedOrThrow("List generic type erased", context);
      }

      Type itemType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
      SchemaObject items = schemaForType(itemType, context);

      SchemaObject array = new SchemaObject();
      array.put("type", "array");
      array.put("items", items.toJsonValue());
      return array;
    }

    if (Map.class.isAssignableFrom(rawType)) {
      Type genericType = field.getGenericType();
      if (!(genericType instanceof ParameterizedType)) {
        return unsupportedOrThrow("Map generic type erased", context);
      }

      Type[] args = ((ParameterizedType) genericType).getActualTypeArguments();
      if (args.length != 2) {
        return unsupportedOrThrow("Map must have 2 type args", context);
      }

      Class<?> keyClass = ReflectionTypes.toClass(args[0]);
      if (keyClass != String.class) {
        return unsupportedOrThrow("Only Map<String,V> supported", context);
      }

      SchemaObject valueSchema = schemaForType(args[1], context);

      SchemaObject obj = new SchemaObject();
      obj.put("type", "object");
      obj.put("additionalProperties", valueSchema.toJsonValue());
      return obj;
    }

    return schemaForType(field.getGenericType(), context);
  }

  private SchemaObject buildSwaggerPolymorphicFieldSchemaIfPresent(Schema schemaAnn, SchemaContext context) {
    if (schemaAnn == null) {
      return null;
    }

    Class<?>[] oneOfClasses = schemaAnn.oneOf();
    String discriminatorProperty = schemaAnn.discriminatorProperty();
    DiscriminatorMapping[] discriminatorMappings = schemaAnn.discriminatorMapping();

    boolean hasOneOf = oneOfClasses != null && oneOfClasses.length > 0;
    boolean hasDiscriminator = discriminatorProperty != null && !discriminatorProperty.isBlank();

    if (!hasOneOf && !hasDiscriminator) {
      return null;
    }

    SchemaObject out = new SchemaObject();

    if (hasOneOf) {
      List<Class<?>> classes = new ArrayList<>();
      for (Class<?> c : oneOfClasses) {
        if (c != null && c != Void.class) {
          classes.add(c);
        }
      }
      classes.sort(Comparator.comparing(Class::getName));

      List<Object> oneOf = new ArrayList<>();
      for (Class<?> c : classes) {
        String ref = "#/$defs/" + context.defName(c);
        oneOf.add(SchemaObject.ref(ref).toJsonValue());
      }
      out.put("oneOf", oneOf);
    }

    if (hasDiscriminator) {
      Map<String, Object> disc = new LinkedHashMap<>();
      disc.put("propertyName", discriminatorProperty);

      if (discriminatorMappings != null && discriminatorMappings.length > 0) {
        List<DiscriminatorMapping> dmList = new ArrayList<>(Arrays.asList(discriminatorMappings));
        dmList.sort(Comparator.comparing(DiscriminatorMapping::value));

        Map<String, Object> mapping = new LinkedHashMap<>();
        for (DiscriminatorMapping dm : dmList) {
          if (dm == null) {
            continue;
          }
          Class<?> schemaClass = dm.schema();
          if (schemaClass == null || schemaClass == Void.class) {
            continue;
          }
          mapping.put(dm.value(), "#/$defs/" + context.defName(schemaClass));
        }

        if (!mapping.isEmpty()) {
          disc.put("mapping", mapping);
        }
      }

      out.put("discriminator", disc);
    }

    return out;
  }

  private SchemaObject schemaForType(Type type, SchemaContext context) {
    Class<?> clazz = ReflectionTypes.toClass(type);
    if (clazz == null) {
      return unsupportedOrThrow("Unsupported type: " + type.getTypeName(), context);
    }

    if (BaseConfigDTO.class.isAssignableFrom(clazz)) {
      return SchemaObject.ref("#/$defs/" + context.defName(clazz));
    }

    if (clazz.isEnum()) {
      SchemaObject e = new SchemaObject();
      e.put("type", "string");

      Object[] constants = clazz.getEnumConstants();
      List<String> values = new ArrayList<>(constants.length);
      for (Object c : constants) {
        values.add(((Enum<?>) c).name());
      }
      e.put("enum", values);
      return e;
    }

    if (clazz == String.class) {
      SchemaObject s = new SchemaObject();
      s.put("type", "string");
      return s;
    }

    if (clazz == boolean.class || clazz == Boolean.class) {
      SchemaObject s = new SchemaObject();
      s.put("type", "boolean");
      return s;
    }

    if (isInteger(clazz)) {
      SchemaObject s = new SchemaObject();
      s.put("type", "integer");
      return s;
    }

    if (isNumber(clazz)) {
      SchemaObject s = new SchemaObject();
      s.put("type", "number");
      return s;
    }

    return unsupportedOrThrow("Unsupported field type (no guessing): " + clazz.getName(), context);
  }

  private void applySwaggerSchema(
      SchemaObject propertySchema,
      Schema schemaAnn,
      Class<?> javaType,
      SchemaContext context
  ) {
    Map<?, ?> current = (Map<?, ?>) propertySchema.toJsonValue();
    boolean isPolymorphic = current.containsKey("oneOf") || current.containsKey("discriminator");

    if (!schemaAnn.description().isBlank()) {
      propertySchema.put("description", schemaAnn.description());
    }

    // examples
    // Skip string examples for polymorphic objects (prevents nonsense like "File" on an object schema).
    if (!isPolymorphic && !schemaAnn.example().isBlank()) {
      Object exampleValue = coerceSwaggerValue(propertySchema, schemaAnn.example(), javaType);
      if (exampleValue != null) {
        propertySchema.put("examples", List.of(exampleValue));
      }
    }

    // default
    if (!schemaAnn.defaultValue().isBlank()) {
      Object defaultValue = coerceSwaggerValue(propertySchema, schemaAnn.defaultValue(), javaType);
      if (defaultValue != null) {
        propertySchema.put("default", defaultValue);
      }
      else if (mode == SchemaGenerationMode.RELAXED) {
        context.warn("Ignored defaultValue that could not be coerced for " + javaType.getName());
      }
    }

    // const
    if (!schemaAnn._const().isBlank()) {
      Object constValue = coerceSwaggerValue(propertySchema, schemaAnn._const(), javaType);
      if (constValue != null) {
        propertySchema.put("const", constValue);
        // JSON Schema enum must match the const value type, not the raw annotation string.
        propertySchema.put("enum", List.of(constValue));
      }
      else if (mode == SchemaGenerationMode.RELAXED) {
        context.warn("Ignored const that could not be coerced for " + javaType.getName());
      }
    }

    // allowableValues
    List<String> enumValues = sanitizeAllowableValues(schemaAnn.allowableValues());
    if (!enumValues.isEmpty()) {
      propertySchema.put("enum", enumValues);
    }

    // numeric constraints
    boolean numeric = isInteger(javaType) || isNumber(javaType);
    if (numeric) {
      if (!schemaAnn.minimum().isBlank()) {
        propertySchema.put("minimum", coerceNumber(schemaAnn.minimum(), javaType, "minimum"));
      }

      if (!schemaAnn.maximum().isBlank()) {
        propertySchema.put("maximum", coerceNumber(schemaAnn.maximum(), javaType, "maximum"));
      }

      double multipleOf = schemaAnn.multipleOf();
      if (multipleOf > 0.0d) {
        propertySchema.put("multipleOf", multipleOf);
      }
    }
    else if (mode == SchemaGenerationMode.RELAXED) {
      if (!schemaAnn.minimum().isBlank() || !schemaAnn.maximum().isBlank() || schemaAnn.multipleOf() > 0.0d) {
        context.warn("Ignored numeric constraints on non-numeric field type " + javaType.getName());
      }
    }
  }


  private List<String> sanitizeAllowableValues(String[] allowable) {
    if (allowable == null || allowable.length == 0) {
      return List.of();
    }

    List<String> vals = new ArrayList<>();
    for (String v : allowable) {
      if (v != null && !v.isBlank()) {
        vals.add(v);
      }
    }
    return vals;
  }

  private Object coerceExample(String raw, Class<?> javaType) {
    if (javaType == boolean.class || javaType == Boolean.class) {
      if ("true".equalsIgnoreCase(raw)) {
        return true;
      }
      if ("false".equalsIgnoreCase(raw)) {
        return false;
      }
      return raw;
    }

    if (isInteger(javaType)) {
      try {
        return Long.parseLong(raw);
      }
      catch (NumberFormatException e) {
        return raw;
      }
    }

    if (isNumber(javaType)) {
      try {
        return new BigDecimal(raw);
      }
      catch (NumberFormatException e) {
        return raw;
      }
    }

    return raw;
  }

  private Object coerceNumber(String raw, Class<?> javaType, String keyword) {
    if (isInteger(javaType)) {
      return Long.parseLong(raw);
    }
    if (isNumber(javaType)) {
      return new BigDecimal(raw);
    }
    throw new IllegalStateException(keyword + " used on non-numeric type " + javaType.getName());
  }

  private SchemaObject unsupportedOrThrow(String reason, SchemaContext context) {
    if (mode == SchemaGenerationMode.STRICT) {
      throw new IllegalStateException(reason);
    }

    context.warn("UNSUPPORTED: " + reason);
    return unsupportedPlaceholder("UNSUPPORTED", reason);
  }

  private SchemaObject unsupportedPlaceholder(String label, String detail) {
    SchemaObject placeholder = new SchemaObject();
    placeholder.put("type", "object");
    placeholder.put("description", label + ": " + detail);
    placeholder.put("additionalProperties", true);
    return placeholder;
  }

  private boolean isCollection(Class<?> rawType) {
    return List.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType);
  }

  private boolean isInteger(Class<?> t) {
    return t == byte.class || t == Byte.class
        || t == short.class || t == Short.class
        || t == int.class || t == Integer.class
        || t == long.class || t == Long.class;
  }

  private boolean isNumber(Class<?> t) {
    return t == float.class || t == Float.class
        || t == double.class || t == Double.class
        || t == BigDecimal.class;
  }

  private List<Class<?>> sortClasses(Set<Class<?>> classes) {
    List<Class<?>> list = new ArrayList<>(classes);
    list.sort(Comparator.comparing(Class::getName));
    return list;
  }

  private Object coerceSwaggerValue(SchemaObject propertySchema, String rawValue, Class<?> javaType) {
    if (rawValue == null || rawValue.isBlank()) {
      return null;
    }

    String trimmed = rawValue.trim();

    Map<?, ?> current = (Map<?, ?>) propertySchema.toJsonValue();
    Object typeObj = current.get("type");

    if (typeObj instanceof String) {
      String schemaType = ((String) typeObj).toLowerCase(Locale.ROOT);

      if ("array".equals(schemaType)) {
        if (trimmed.startsWith("[")) {
          try {
            Object parsed = OBJECT_MAPPER.readValue(trimmed, new TypeReference<Object>() {});
            if (parsed instanceof List) {
              return parsed;
            }
          }
          catch (Exception ignored) {
            // fall through to null
          }
        }
        return null;
      }

      if ("object".equals(schemaType)) {
        if (trimmed.startsWith("{")) {
          try {
            Object parsed = OBJECT_MAPPER.readValue(trimmed, new TypeReference<Object>() {});
            if (parsed instanceof Map) {
              return parsed;
            }
          }
          catch (Exception ignored) {
            // fall through to null
          }
        }
        return null;
      }

      // Scalars: keep your existing behavior
      return coerceExample(rawValue, javaType);
    }

    // If you later emit `type: ["string","null"]`, handle it here.
    // For now, safest is: don't emit a default/example/const we can't type-check.
    return null;
  }
}
