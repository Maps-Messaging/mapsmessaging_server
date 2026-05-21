package io.mapsmessaging.tools.config.schema;

import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.tools.config.lint.RootDtoResolver;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class RuntimeJsonSchemaService {

  private final RuntimeJsonSchemaGenerator generator;

  public RuntimeJsonSchemaService(RuntimeJsonSchemaGenerator generator) {
    this.generator = generator;
  }

  public Map<String, String> generateAllSchemas() {
    ServiceLoader<ConfigManager> loader = ServiceLoader.load(ConfigManager.class);

    Map<String, String> schemas = new LinkedHashMap<>();

    loader.stream()
        .map(ServiceLoader.Provider::get)
        .sorted(Comparator.comparing(m -> m.getClass().getName()))
        .forEach(manager -> {
          String configName = resolveConfigName(manager);

          Class<? extends BaseConfigDTO> rootDtoClass = RootDtoResolver.resolveRootDto(manager.getClass());
          if (rootDtoClass == null) {
            throw new IllegalStateException("Unable to resolve root DTO for " + manager.getClass().getName());
          }

          String schemaJson = generator.generateSchema(configName, rootDtoClass);
          schemas.put(configName, schemaJson);
        });

    return schemas;
  }

  private String resolveConfigName(ConfigManager manager) {
    String name = manager.getName();
    if (name == null || name.isBlank()) {
      throw new IllegalStateException("ConfigManager.getName() is blank for " + manager.getClass().getName());
    }
    return name;
  }
}
