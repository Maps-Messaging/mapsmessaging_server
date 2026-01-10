package io.mapsmessaging.tools.configschema;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SchemaObject {

  private final Map<String, Object> values = new LinkedHashMap<>();

  public void put(String key, Object value) {
    if (value != null) {
      values.put(key, value);
    }
  }

  public Object toJsonValue() {
    return values;
  }

  public static SchemaObject ref(String ref) {
    SchemaObject s = new SchemaObject();
    s.put("$ref", ref);
    return s;
  }
}
