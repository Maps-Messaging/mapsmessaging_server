package io.mapsmessaging.tools.configschema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SchemaDocument {

  private final Map<String, Object> values;

  public SchemaDocument() {
    this.values = new LinkedHashMap<>();
  }

  public void put(String key, Object value) {
    if (value == null) {
      return;
    }
    values.put(Objects.requireNonNull(key, "key"), value);
  }

  public boolean containsKey(String key) {
    return values.containsKey(key);
  }

  public Object get(String key) {
    return values.get(key);
  }

  public Map<String, Object> toJsonValue() {
    return values;
  }

  // ---- Convenience helpers (optional but keeps generator readable) ----

  public void schema(String schemaUri) {
    put("$schema", schemaUri);
  }

  public void id(String id) {
    put("$id", id);
  }

  public void title(String title) {
    put("title", title);
  }

  public void description(String description) {
    if (description != null && !description.isBlank()) {
      put("description", description);
    }
  }

  public void ref(String ref) {
    put("$ref", ref);
  }

  public void defs(Map<String, Object> defs) {
    put("$defs", defs);
  }

  public void warnings(Object warnings) {
    put("x-warnings", warnings);
  }
}
