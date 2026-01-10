package io.mapsmessaging.tools.configschema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.tools.configlint.ReflectionFields;
import io.mapsmessaging.tools.configlint.ReflectionTypes;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

public class RuntimeJsonSchemaGenerator {

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

    Set<Class<?>> dtoClosure = discoverDtoClosure(rootDtoClass);

    for (Class<?> dtoClass : sortClasses(dtoClosure)) {
      context.ensureDefName(dtoClass);
    }

    for (Class<?> dtoClass : sortClasses(dtoClosure)) {
      SchemaObject defSchema = buildDtoDefinition(dtoClass, context);
      context.putDef(dtoClass, defSchema);
    }

    String rootRef = "#/$defs/" + context.defName(rootDtoClass);

    SchemaDocument doc = new SchemaDocument();
    doc.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    doc.put("$id", "urn:mapsmessaging:config-schema:" + configName);
    doc.put("title", configName);

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

  private Set<Class<?>> discoverDtoClosure(Class<? extends BaseConfigDTO> rootDto) {
    Set<Class<?>> visited = new HashSet<>();
    Deque<Class<?>> stack = new ArrayDeque<>();
    stack.push(rootDto);

    while (!stack.isEmpty()) {
      Class<?> clazz = stack.pop();
      if (!visited.add(clazz)) {
        continue;
      }

      for (Field field : ReflectionFields.getAllInstanceFields(clazz)) {
        Class<?> rawType = field.getType();

        if (isCollection(rawType)) {
          Type genericType = field.getGenericType();
          if (!(genericType instanceof ParameterizedType)) {
            continue;
          }

          ParameterizedType pt = (ParameterizedType) genericType;
          Type[] args = pt.getActualTypeArguments();

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
          if (subtypeClass != null && BaseConfigDTO.class.isAssignableFrom(subtypeClass)) {
            stack.push(subtypeClass);
          }
        }
      }
    }

    return visited;
  }

  private SchemaObject buildDtoDefinition(Class<?> dtoClass, SchemaContext context) {
    context.enterDto(dtoClass);
    try {
      SchemaObject poly = buildPolymorphicDefinitionIfNeeded(dtoClass, context);
      if (poly != null) {
        return poly;
      }

      SchemaObject schema = new SchemaObject();
      schema.put("type", "object");

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

          applySwaggerSchema(propertySchema, fieldSchemaAnn, field.getType());

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
          properties.put(name, unsupportedPlaceholder("Field schema failed", field.getGenericType().getTypeName()).toJsonValue());
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

  private SchemaObject buildPolymorphicDefinitionIfNeeded(Class<?> dtoClass, SchemaContext context) {
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

  private void applySwaggerSchema(SchemaObject propertySchema, Schema schemaAnn, Class<?> javaType) {
    if (!schemaAnn.description().isBlank()) {
      propertySchema.put("description", schemaAnn.description());
    }

    if (!schemaAnn.example().isBlank()) {
      propertySchema.put("examples", List.of(coerceExample(schemaAnn.example(), javaType)));
    }

    if (!schemaAnn.defaultValue().isBlank()) {
      propertySchema.put("default", coerceExample(schemaAnn.defaultValue(), javaType));
    }

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

    String[] allowable = schemaAnn.allowableValues();
    if (allowable != null && allowable.length > 0) {
      List<String> vals = new ArrayList<>();
      for (String v : allowable) {
        if (v != null && !v.isBlank()) {
          vals.add(v);
        }
      }
      if (!vals.isEmpty()) {
        propertySchema.put("enum", vals);
      }
    }
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
}
