package io.mapsmessaging.tools.config.schema;

import java.lang.reflect.Field;
import java.util.*;

public final class SchemaContext {

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
