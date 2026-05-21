package io.mapsmessaging.tools.config.schema;

import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;

import java.util.*;

public final class ProtocolTransformationNameRegistry {

  private ProtocolTransformationNameRegistry() {
  }

  public static List<String> getAllowedNames(boolean includeEmpty) {
    Set<String> names = new LinkedHashSet<>();

    if (includeEmpty) {
      names.add("");
    }

    ServiceLoader<ProtocolMessageTransformation> serviceLoader =
        ServiceLoader.load(ProtocolMessageTransformation.class);

    for (ProtocolMessageTransformation transformation : serviceLoader) {
      if (transformation == null) {
        continue;
      }

      String name = transformation.getName();
      if (name == null) {
        continue;
      }

      String trimmed = name.trim();
      if (trimmed.isEmpty()) {
        continue;
      }

      names.add(trimmed);
    }

    List<String> sorted = new ArrayList<>(names);
    Collections.sort(sorted);
    return sorted;
  }
}
