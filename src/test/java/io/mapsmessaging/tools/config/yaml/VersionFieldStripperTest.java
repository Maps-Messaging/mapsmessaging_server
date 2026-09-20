package io.mapsmessaging.tools.config.yaml;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VersionFieldStripperTest {

  @Test
  void keepsRootVersionButRemovesNestedMapVersions() {
    Map<String, Object> child = new LinkedHashMap<>();
    child.put("schemaLoadingVersion", 2);
    child.put("value", "child");

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("schemaLoadingVersion", 7);
    root.put("child", child);

    new VersionFieldStripper("schemaLoadingVersion")
        .removeVersionFromChildrenOnly(root);

    assertEquals(7, root.get("schemaLoadingVersion"));
    assertFalse(child.containsKey("schemaLoadingVersion"));
    assertEquals("child", child.get("value"));
  }

  @Test
  void removesVersionsRecursivelyInsideIterableValues() {
    Map<String, Object> first = new LinkedHashMap<>();
    first.put("schemaLoadingVersion", 1);
    first.put("name", "first");

    Map<String, Object> deep = new LinkedHashMap<>();
    deep.put("schemaLoadingVersion", 3);

    Map<String, Object> second = new LinkedHashMap<>();
    second.put("schemaLoadingVersion", 2);
    second.put("deep", deep);

    List<Object> children = new ArrayList<>();
    children.add(first);
    children.add(second);

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("schemaLoadingVersion", 9);
    root.put("children", children);

    new VersionFieldStripper("schemaLoadingVersion")
        .removeVersionFromChildrenOnly(root);

    assertEquals(9, root.get("schemaLoadingVersion"));
    assertFalse(first.containsKey("schemaLoadingVersion"));
    assertFalse(second.containsKey("schemaLoadingVersion"));
    assertFalse(deep.containsKey("schemaLoadingVersion"));
  }

  @Test
  void absentVersionFieldsAreLeftAlone() {
    Map<String, Object> nested = new LinkedHashMap<>();
    nested.put("nested", "ok");

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("value", nested);

    new VersionFieldStripper("schemaLoadingVersion")
        .removeVersionFromChildrenOnly(root);

    assertEquals(Map.of("nested", "ok"), root.get("value"));
  }
}
