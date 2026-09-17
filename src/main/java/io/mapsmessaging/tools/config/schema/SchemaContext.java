package io.mapsmessaging.tools.config.schema;

import io.mapsmessaging.tools.config.lint.ReflectionTypes;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public final class SchemaContext {

  private static final String TYPE_FIELD_NAME = "type";

  private final String configName;

  private final Map<Class<?>, String> defNamesByClass = new HashMap<>();
  private final Map<String, Object> defs = new HashMap<>();
  private final List<String> warnings = new ArrayList<>();

  private Class<?> currentDto;
  private Field currentField;

  public SchemaContext(String configName) {
    this.configName = configName;
  }

  public void enterDto(Class<?> dtoClass) {
    this.currentDto = dtoClass;
  }

  public void exitDto() {
    this.currentDto = null;
  }

  public void enterField(Field field) {
    this.currentField = field;
  }

  public void exitField() {
    this.currentField = null;
  }

  public String ensureDefName(Class<?> clazz) {
    return defNamesByClass.computeIfAbsent(clazz, c -> c.getName().replace('.', '_'));
  }

  public String defName(Class<?> clazz) {
    return ensureDefName(clazz);
  }

  public void putDef(Class<?> clazz, SchemaObject schemaObject) {
    applyInheritedSwaggerDiscriminator(clazz, schemaObject);
    applyAnnotatedGetterProperties(clazz, schemaObject);
    defs.put(defName(clazz), schemaObject.toJsonValue());
  }

  public Map<String, Object> buildDefsObject() {
    List<String> keys = new ArrayList<>(defs.keySet());
    Collections.sort(keys);

    Map<String, Object> sorted = new LinkedHashMap<>();
    for (String k : keys) {
      sorted.put(k, defs.get(k));
    }
    return sorted;
  }

  public void warn(String message) {
    warnings.add(contextPrefix() + message);
  }

  public List<String> warnings() {
    return List.copyOf(warnings);
  }

  public IllegalStateException error(String message, Throwable cause) {
    StringBuilder sb = new StringBuilder();
    sb.append("Config schema generation error");
    sb.append("\n  config: ").append(configName);

    if (currentDto != null) {
      sb.append("\n  dto: ").append(currentDto.getName());
    }

    if (currentField != null) {
      sb.append("\n  field: ")
          .append(currentField.getDeclaringClass().getName())
          .append(".")
          .append(currentField.getName());
      sb.append("\n  fieldType: ").append(currentField.getGenericType().getTypeName());
    }

    sb.append("\n  reason: ").append(message);

    return new IllegalStateException(sb.toString(), cause);
  }

  private void applyInheritedSwaggerDiscriminator(Class<?> clazz, SchemaObject schemaObject) {
    Object propertiesObject = schemaObject.get("properties");
    if (!(propertiesObject instanceof Map<?, ?> properties)) {
      return;
    }

    Object typeObject = properties.get(TYPE_FIELD_NAME);
    if (!(typeObject instanceof Map<?, ?> rawTypeSchema)) {
      return;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> typeSchema = (Map<String, Object>) rawTypeSchema;
    if (typeSchema.containsKey("const")) {
      removeConflictingDiscriminatorHints(typeSchema);
      return;
    }

    String discriminatorValue = findInheritedSwaggerDiscriminatorValue(clazz);
    if (discriminatorValue == null || discriminatorValue.isBlank()) {
      return;
    }

    typeSchema.put("const", discriminatorValue);
    typeSchema.put("enum", List.of(discriminatorValue));
    removeConflictingDiscriminatorHints(typeSchema);
  }

  private void removeConflictingDiscriminatorHints(Map<String, Object> typeSchema) {
    typeSchema.remove("examples");
    typeSchema.remove("default");
  }

  private String findInheritedSwaggerDiscriminatorValue(Class<?> clazz) {
    for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
      Schema schema = current.getDeclaredAnnotation(Schema.class);
      if (schema == null) {
        continue;
      }

      for (DiscriminatorMapping mapping : schema.discriminatorMapping()) {
        if (mapping != null && mapping.schema() == clazz) {
          return mapping.value();
        }
      }
    }
    return null;
  }

  private void applyAnnotatedGetterProperties(Class<?> clazz, SchemaObject schemaObject) {
    Object propertiesObject = schemaObject.get("properties");
    if (!(propertiesObject instanceof Map<?, ?> rawProperties)) {
      return;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) rawProperties;

    for (Method method : clazz.getMethods()) {
      Schema schema = method.getAnnotation(Schema.class);
      if (schema == null || schema.hidden() || method.getParameterCount() != 0 || method.getReturnType() == Void.TYPE) {
        continue;
      }

      String propertyName = getterPropertyName(method);
      if (propertyName == null || properties.containsKey(propertyName)) {
        continue;
      }

      Map<String, Object> propertySchema = schemaForGetter(method.getGenericReturnType());
      if (!schema.description().isBlank()) {
        propertySchema.put("description", schema.description());
      }

      if (schema.nullable()) {
        Map<String, Object> nullableSchema = new LinkedHashMap<>();
        nullableSchema.put("anyOf", List.of(propertySchema, Map.of("type", "null")));
        if (!schema.description().isBlank()) {
          nullableSchema.put("description", schema.description());
        }
        properties.put(propertyName, nullableSchema);
      } else {
        properties.put(propertyName, propertySchema);
      }

      if (schema.requiredMode() == Schema.RequiredMode.REQUIRED) {
        addRequired(schemaObject, propertyName);
      }
    }
  }

  private String getterPropertyName(Method method) {
    String name = method.getName();
    if (name.startsWith("get") && name.length() > 3) {
      return Introspector.decapitalize(name.substring(3));
    }
    if (name.startsWith("is") && name.length() > 2 &&
        (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
      return Introspector.decapitalize(name.substring(2));
    }
    return null;
  }

  private Map<String, Object> schemaForGetter(Type type) {
    Class<?> rawType = ReflectionTypes.toClass(type);
    Map<String, Object> schema = new LinkedHashMap<>();

    if (rawType == null) {
      return schema;
    }

    if (List.class.isAssignableFrom(rawType)) {
      schema.put("type", "array");
      if (type instanceof ParameterizedType parameterizedType) {
        Type[] arguments = parameterizedType.getActualTypeArguments();
        if (arguments.length == 1) {
          schema.put("items", schemaForGetter(arguments[0]));
        }
      }
      return schema;
    }

    if (Map.class.isAssignableFrom(rawType)) {
      schema.put("type", "object");
      schema.put("additionalProperties", true);
      return schema;
    }

    if (rawType == String.class || rawType == Character.class || rawType == char.class || rawType == UUID.class) {
      schema.put("type", "string");
      return schema;
    }

    if (rawType == boolean.class || rawType == Boolean.class) {
      schema.put("type", "boolean");
      return schema;
    }

    if (rawType == byte.class || rawType == Byte.class || rawType == short.class || rawType == Short.class ||
        rawType == int.class || rawType == Integer.class || rawType == long.class || rawType == Long.class) {
      schema.put("type", "integer");
      return schema;
    }

    if (rawType == float.class || rawType == Float.class || rawType == double.class || rawType == Double.class) {
      schema.put("type", "number");
      return schema;
    }

    if (rawType.isEnum()) {
      schema.put("type", "string");
      List<String> values = Arrays.stream(rawType.getEnumConstants())
          .map(value -> ((Enum<?>) value).name())
          .toList();
      schema.put("enum", values);
      return schema;
    }

    schema.put("type", "object");
    schema.put("additionalProperties", true);
    return schema;
  }

  private void addRequired(SchemaObject schemaObject, String propertyName) {
    Object requiredObject = schemaObject.get("required");
    List<String> required = new ArrayList<>();
    if (requiredObject instanceof List<?> existing) {
      for (Object value : existing) {
        if (value instanceof String stringValue) {
          required.add(stringValue);
        }
      }
    }
    if (!required.contains(propertyName)) {
      required.add(propertyName);
      Collections.sort(required);
      schemaObject.put("required", required);
    }
  }

  private String contextPrefix() {
    StringBuilder sb = new StringBuilder();
    sb.append("[config=").append(configName);
    if (currentDto != null) {
      sb.append(" dto=").append(currentDto.getName());
    }
    if (currentField != null) {
      sb.append(" field=").append(currentField.getDeclaringClass().getSimpleName())
          .append(".")
          .append(currentField.getName());
    }
    sb.append("] ");
    return sb.toString();
  }
}
