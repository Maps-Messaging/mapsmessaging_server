package io.mapsmessaging.tools.config.schema;

import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.tools.config.lint.RootDtoResolver;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class JsonSchemaGenerationService {

  private final JsonSchemaGenerator jsonSchemaGenerator;
  private final SchemaLintGate schemaLintGate;

  public JsonSchemaGenerationService(JsonSchemaGenerator jsonSchemaGenerator, SchemaLintGate schemaLintGate) {
    this.jsonSchemaGenerator = jsonSchemaGenerator;
    this.schemaLintGate = schemaLintGate;
  }

  public Map<String, String> generateAll() {
    schemaLintGate.assertLintPassed();

    ServiceLoader<ConfigManager> loader = ServiceLoader.load(ConfigManager.class);

    Map<String, String> schemasByConfigName = new LinkedHashMap<>();

    loader.stream()
        .map(ServiceLoader.Provider::get)
        .sorted(Comparator.comparing(provider -> provider.getClass().getName()))
        .forEach(manager -> {
          String configName = resolveConfigName(manager);
          Class<?> managerClass = manager.getClass();
          Class<? extends BaseConfigDTO> rootDtoClass = RootDtoResolver.resolveRootDto(managerClass);
          if (rootDtoClass == null) {
            throw new IllegalStateException("Unable to resolve root DTO for ConfigManager: " + managerClass.getName());
          }

          String schemaJson = jsonSchemaGenerator.generate(configName, rootDtoClass);
          schemasByConfigName.put(configName, schemaJson);
        });

    return schemasByConfigName;
  }

  private String resolveConfigName(ConfigManager manager) {
    String name = manager.getName();
    if (name == null || name.isBlank()) {
      throw new IllegalStateException("ConfigManager returned blank config name: " + manager.getClass().getName());
    }
    return name;
  }

  @FunctionalInterface
  public interface SchemaLintGate {
    void assertLintPassed();
  }
}
