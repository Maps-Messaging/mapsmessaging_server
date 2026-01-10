package io.mapsmessaging.tools.config.schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class JsonSchemaGenerator {

  public String generate(String configName, Class<? extends BaseConfigDTO> rootDtoClass) {
    SchemaContext context = new SchemaContext(configName);

    SchemaObject rootRef = schemaForDto(rootDtoClass, context);

    SchemaDocument doc = new SchemaDocument();
    doc.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    doc.put("$id", "urn:mapsmessaging:config-schema:" + configName);
    doc.put("title", configName);

    Schema rootSchemaAnn = rootDtoClass.getAnnotation(Schema.class);
    if (rootSchemaAnn != null && !rootSchemaAnn.description().isBlank()) {
      doc.put("description", rootSchemaAnn.description());
    }

    doc.put("$ref", "#/$defs/" + context.defName(rootDtoClass));
    doc.put("$defs", context.buildDefsObject());

    return DeterministicJsonWriter.write(doc.toJsonValue());
  }

  private SchemaObject schemaForDto(Class<?> dtoClass, SchemaContext context) {
    if (!BaseConfigDTO.class.isAssignableFrom(dtoClass)) {
      throw new IllegalArgumentException("Not a BaseConfigDTO: " + dtoClass.getName());
    }

    String defName = context.ensureDefName(dtoClass);

    context.enterDto(dtoClass);
    try {
      Optional<SchemaObject> polymorphic = schemaForPolymorphicBase(dtoClass, context);
      if (polymorphic.isPresent()) {
        return SchemaObject.ref("#/$defs/" + defName);
      }

      SchemaObject objectSchema = new SchemaObject();
      objectSchema.put("type", "object");

      Schema classSchema = dtoClass.getAnnotation(Schema.class);
      if (classSchema != null && !classSchema.description().isBlank()) {
        objectSchema.put("description", classSchema.description());
      }

      Map<String, Object> properties = new LinkedHashMap<>();
      SortedSet<String> required = new TreeSet<>();

      for (Field field : getAllInstanceFields(dtoClass)) {
        // If you want to avoid BaseConfigDTO/internal fields, uncomment this:
        // Schema fieldSchema = field.getAnnotation(Schema.class);
        // if (fieldSchema == null || fieldSchema.hidden()) { continue; }

        context.enterField(field);
        try {
          String propertyName = field.getName();
          SchemaObject propertySchema = schemaForField(field, context);

          Schema fieldSchema = field.getAnnotation(Schema.class);
          if (fieldSchema != null) {
            applySwaggerSchemaMetadata(propertySchema, fieldSchema, field.getType());
            if (fieldSchema.requiredMode() == Schema.RequiredMode.REQUIRED) {
              required.add(propertyName);
            }
          }

          properties.put(propertyName, propertySchema.toJsonValue());
        }
        finally {
          context.exitField();
        }
      }

      objectSchema.put("properties", properties);
      if (!required.isEmpty()) {
        objectSchema.put("required", new ArrayList<>(required));
      }

      return SchemaObject.ref("#/$defs/" + defName);
    }
    catch (RuntimeException e) {
      throw context.error("Failed building DTO schema", e);
    }
    finally {
      context.exitDto();
    }
  }

  private SchemaObject schemaForField(Field field, SchemaContext context) {
    try {
      Class<?> rawType = field.getType();

      if (List.class.isAssignableFrom(rawType)) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
          throw new IllegalStateException("List generic type erased");
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
          throw new IllegalStateException("Map generic type erased");
        }

        Type[] args = ((ParameterizedType) genericType).getActualTypeArguments();
        if (args.length != 2) {
          throw new IllegalStateException("Map must have 2 type args");
        }

        Class<?> keyClass = toClass(args[0]);
        if (keyClass != String.class) {
          throw new IllegalStateException("Only Map<String,V> supported");
        }

        SchemaObject valueSchema = schemaForType(args[1], context);

        SchemaObject obj = new SchemaObject();
        obj.put("type", "object");
        obj.put("additionalProperties", valueSchema.toJsonValue());
        return obj;
      }

      return schemaForType(field.getGenericType(), context);
    }
    catch (RuntimeException e) {
      throw context.error("Failed generating schema for field", e);
    }
  }

  private SchemaObject schemaForType(Type type, SchemaContext context) {
    Class<?> clazz = toClass(type);
    if (clazz == null) {
      throw new IllegalStateException("Unsupported type: " + type.getTypeName());
    }

    if (BaseConfigDTO.class.isAssignableFrom(clazz)) {
      return schemaForDto(clazz, context);
    }

    if (clazz.isEnum()) {
      SchemaObject enumSchema = new SchemaObject();
      enumSchema.put("type", "string");

      Object[] constants = clazz.getEnumConstants();
      List<String> values = new ArrayList<>(constants.length);
      for (Object c : constants) {
        values.add(((Enum<?>) c).name());
      }
      enumSchema.put("enum", values);
      return enumSchema;
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

    if (isIntegerType(clazz)) {
      SchemaObject s = new SchemaObject();
      s.put("type", "integer");
      return s;
    }

    if (isNumberType(clazz)) {
      SchemaObject s = new SchemaObject();
      s.put("type", "number");
      return s;
    }

    throw new IllegalStateException("Unsupported field type (no guessing): " + clazz.getName());
  }

  private Optional<SchemaObject> schemaForPolymorphicBase(Class<?> baseType, SchemaContext context) {
    JsonTypeInfo typeInfo = baseType.getAnnotation(JsonTypeInfo.class);
    JsonSubTypes subTypes = baseType.getAnnotation(JsonSubTypes.class);

    if (typeInfo == null || subTypes == null) {
      return Optional.empty();
    }

    String discriminatorProperty = typeInfo.property();
    if (discriminatorProperty == null || discriminatorProperty.isBlank()) {
      throw new IllegalStateException("@JsonTypeInfo.property() must be set for: " + baseType.getName());
    }

    List<JsonSubTypes.Type> subtypeList = Arrays.asList(subTypes.value());
    if (subtypeList.isEmpty()) {
      throw new IllegalStateException("@JsonSubTypes is empty for: " + baseType.getName());
    }

    List<JsonSubTypes.Type> sortedSubtypes = subtypeList.stream()
        .sorted(Comparator.comparing(t -> t.value().getName()))
        .collect(Collectors.toList());

    List<Object> oneOf = new ArrayList<>();
    Map<String, Object> mapping = new LinkedHashMap<>();

    for (JsonSubTypes.Type subtype : sortedSubtypes) {
      Class<?> subtypeClass = subtype.value();
      if (!baseType.isAssignableFrom(subtypeClass)) {
        continue;
      }

      SchemaObject subtypeRef = schemaForDto(subtypeClass, context);
      oneOf.add(subtypeRef.toJsonValue());

      String name = subtype.name();
      String discriminatorValue = (name == null || name.isBlank())
          ? subtypeClass.getSimpleName()
          : name;

      mapping.put(discriminatorValue, "#/$defs/" + context.ensureDefName(subtypeClass));
    }

    SchemaObject schema = new SchemaObject();
    schema.put("oneOf", oneOf);

    Map<String, Object> discriminator = new LinkedHashMap<>();
    discriminator.put("propertyName", discriminatorProperty);
    if (!mapping.isEmpty()) {
      discriminator.put("mapping", mapping);
    }
    schema.put("discriminator", discriminator);

    return Optional.of(schema);
  }

  private void applySwaggerSchemaMetadata(SchemaObject propertySchema, Schema schema, Class<?> javaFieldType) {
    if (!schema.description().isBlank()) {
      propertySchema.put("description", schema.description());
    }

    if (!schema.example().isBlank()) {
      Object exampleValue = coerceExample(schema.example(), javaFieldType);
      propertySchema.put("examples", List.of(exampleValue));
    }

    if (!schema.defaultValue().isBlank()) {
      Object defaultValue = coerceExample(schema.defaultValue(), javaFieldType);
      propertySchema.put("default", defaultValue);
    }

    if (!schema.minimum().isBlank()) {
      propertySchema.put("minimum", coerceNumber(schema.minimum(), javaFieldType, "minimum"));
    }

    if (!schema.maximum().isBlank()) {
      propertySchema.put("maximum", coerceNumber(schema.maximum(), javaFieldType, "maximum"));
    }

    if (schema.multipleOf()<0.0) {
      propertySchema.put("multipleOf", schema.multipleOf());
    }

    String[] allowableValues = schema.allowableValues();
    if (allowableValues != null && allowableValues.length > 0) {
      List<String> enums = Arrays.stream(allowableValues)
          .filter(v -> v != null && !v.isBlank())
          .collect(Collectors.toList());
      if (!enums.isEmpty()) {
        propertySchema.put("enum", enums);
      }
    }
  }

  private Object coerceExample(String raw, Class<?> javaFieldType) {
    if (raw == null) {
      return null;
    }

    if (javaFieldType == boolean.class || javaFieldType == Boolean.class) {
      if ("true".equalsIgnoreCase(raw)) {
        return true;
      }
      if ("false".equalsIgnoreCase(raw)) {
        return false;
      }
      return raw;
    }

    if (isIntegerType(javaFieldType)) {
      try {
        return Long.parseLong(raw);
      }
      catch (NumberFormatException e) {
        return raw;
      }
    }

    if (isNumberType(javaFieldType)) {
      try {
        return new BigDecimal(raw);
      }
      catch (NumberFormatException e) {
        return raw;
      }
    }

    return raw;
  }

  private Object coerceNumber(String raw, Class<?> javaFieldType, String keyword) {
    if (isIntegerType(javaFieldType)) {
      return Long.parseLong(raw);
    }

    if (isNumberType(javaFieldType)) {
      return new BigDecimal(raw);
    }

    throw new IllegalStateException(keyword + " is not valid for non-numeric field type: " + javaFieldType.getName());
  }

  private static boolean isIntegerType(Class<?> clazz) {
    return clazz == byte.class || clazz == Byte.class
        || clazz == short.class || clazz == Short.class
        || clazz == int.class || clazz == Integer.class
        || clazz == long.class || clazz == Long.class;
  }

  private static boolean isNumberType(Class<?> clazz) {
    return clazz == float.class || clazz == Float.class
        || clazz == double.class || clazz == Double.class
        || clazz == BigDecimal.class;
  }

  private static Class<?> toClass(Type t) {
    if (t instanceof Class<?>) {
      return (Class<?>) t;
    }
    if (t instanceof ParameterizedType) {
      Type raw = ((ParameterizedType) t).getRawType();
      return raw instanceof Class<?> ? (Class<?>) raw : null;
    }
    return null;
  }

  private static List<Field> getAllInstanceFields(Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      for (Field field : current.getDeclaredFields()) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || field.isSynthetic()) {
          continue;
        }
        field.setAccessible(true);
        fields.add(field);
      }
      current = current.getSuperclass();
    }

    fields.sort(Comparator
        .comparing((Field f) -> f.getDeclaringClass().getName())
        .thenComparing(Field::getName));

    return fields;
  }
}
