package io.mapsmessaging.tools.config.schema;

import java.util.Map;

public final class JsonSchemaGeneratorMain {

  public static void main(String[] args) {

    // 1) Construct the runtime generator
    RuntimeJsonSchemaGenerator generator = new RuntimeJsonSchemaGenerator();

    // 2) Construct the service wrapper
    RuntimeJsonSchemaService service = new RuntimeJsonSchemaService(generator);

    // 3) Generate schemas
    Map<String, String> schemas = service.generateAllSchemas();

    // 4) Dump results
    if (schemas.isEmpty()) {
      System.err.println("No ConfigManager implementations discovered.");
      return;
    }

    schemas.forEach((name, schemaJson) -> {
      System.out.println("==================================================");
      System.out.println("Config: " + name);
      System.out.println("--------------------------------------------------");
      System.out.println(schemaJson);
      System.out.println();
    });
  }

  private JsonSchemaGeneratorMain() {
    // no instances, this is not Spring
  }
}
