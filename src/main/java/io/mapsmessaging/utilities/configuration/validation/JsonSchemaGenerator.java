/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.utilities.configuration.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates JSON schemas from POJO configuration classes.
 * Uses victools jsonschema-generator with Jackson and Swagger2 modules.
 */
public class JsonSchemaGenerator {

  private static final Logger logger = LoggerFactory.getLogger(JsonSchemaGenerator.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final SchemaGenerator generator;
  private final ValidationConfig config;
  private final Map<Class<?>, JsonNode> schemaCache;
  private final Path schemaCachePath;

  public JsonSchemaGenerator(ValidationConfig config) {
    this.config = config;
    this.schemaCache = new ConcurrentHashMap<>();
    this.schemaCachePath = initializeCachePath();
    this.generator = createSchemaGenerator();

    if (config.isCacheSchemas()) {
      loadCachedSchemas();
    }
  }

  /**
   * Generate JSON schema for a POJO class.
   *
   * @param clazz The POJO class to generate schema for
   * @return JSON schema as JsonNode
   */
  public JsonNode generateSchema(Class<?> clazz) {
    // Check in-memory cache first
    if (schemaCache.containsKey(clazz)) {
      if (config.isVerboseLogging()) {
        logger.log(Logger.INFO, "Using cached schema for " + clazz.getSimpleName());
      }
      return schemaCache.get(clazz);
    }

    // Check disk cache if enabled
    if (config.isCacheSchemas()) {
      JsonNode cached = loadFromDisk(clazz);
      if (cached != null) {
        schemaCache.put(clazz, cached);
        return cached;
      }
    }

    // Generate new schema
    if (config.isVerboseLogging()) {
      logger.log(Logger.INFO, "Generating JSON schema for " + clazz.getSimpleName());
    }

    JsonNode schema = generator.generateSchema(clazz);

    // Cache it
    schemaCache.put(clazz, schema);
    if (config.isCacheSchemas()) {
      saveToDisk(clazz, schema);
    }

    return schema;
  }

  /**
   * Clear all cached schemas (both in-memory and on-disk).
   */
  public void clearCache() {
    schemaCache.clear();
    if (schemaCachePath != null && Files.exists(schemaCachePath)) {
      try {
        Files.walk(schemaCachePath)
            .filter(Files::isRegularFile)
            .forEach(path -> {
              try {
                Files.delete(path);
              } catch (IOException e) {
                logger.log(Logger.WARN, "Failed to delete cached schema: " + path, e);
              }
            });
      } catch (IOException e) {
        logger.log(Logger.WARN, "Failed to clear schema cache", e);
      }
    }
  }

  private SchemaGenerator createSchemaGenerator() {
    SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
        SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);

    // Add Jackson module to respect Jackson annotations
    configBuilder.with(new JacksonModule());

    // Add Swagger2 module to use Swagger @Schema annotations
    configBuilder.with(new Swagger2Module());

    // Configure additional options
    SchemaGeneratorConfig generatorConfig = configBuilder.build();

    return new SchemaGenerator(generatorConfig);
  }

  private Path initializeCachePath() {
    if (!config.isCacheSchemas()) {
      return null;
    }

    String mapsHome = System.getProperty("MAPS_HOME", ".");
    Path cachePath = Paths.get(mapsHome, config.getSchemaCacheDir());

    try {
      Files.createDirectories(cachePath);
      if (config.isVerboseLogging()) {
        logger.log(Logger.INFO, "Schema cache directory: " + cachePath.toAbsolutePath());
      }
    } catch (IOException e) {
      logger.log(Logger.WARN, "Failed to create schema cache directory: " + cachePath, e);
      return null;
    }

    return cachePath;
  }

  private void loadCachedSchemas() {
    if (schemaCachePath == null || !Files.exists(schemaCachePath)) {
      return;
    }

    try {
      Files.walk(schemaCachePath)
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".schema.json"))
          .forEach(path -> {
            try {
              JsonNode schema = objectMapper.readTree(path.toFile());
              String className = path.getFileName().toString()
                  .replace(".schema.json", "");
              if (config.isVerboseLogging()) {
                logger.log(Logger.DEBUG, "Loaded cached schema: " + className);
              }
            } catch (IOException e) {
              logger.log(Logger.WARN, "Failed to load cached schema: " + path, e);
            }
          });
    } catch (IOException e) {
      logger.log(Logger.WARN, "Failed to load cached schemas", e);
    }
  }

  private JsonNode loadFromDisk(Class<?> clazz) {
    if (schemaCachePath == null) {
      return null;
    }

    Path schemaFile = schemaCachePath.resolve(clazz.getSimpleName() + ".schema.json");
    if (!Files.exists(schemaFile)) {
      return null;
    }

    try {
      return objectMapper.readTree(schemaFile.toFile());
    } catch (IOException e) {
      logger.log(Logger.WARN, "Failed to load schema from disk: " + schemaFile, e);
      return null;
    }
  }

  private void saveToDisk(Class<?> clazz, JsonNode schema) {
    if (schemaCachePath == null) {
      return;
    }

    Path schemaFile = schemaCachePath.resolve(clazz.getSimpleName() + ".schema.json");
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(schemaFile.toFile(), schema);
      if (config.isVerboseLogging()) {
        logger.log(Logger.DEBUG, "Saved schema to disk: " + schemaFile);
      }
    } catch (IOException e) {
      logger.log(Logger.WARN, "Failed to save schema to disk: " + schemaFile, e);
    }
  }
}
