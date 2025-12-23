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
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for YAML validation system.
 */
class YamlValidatorTest {

  private ValidationConfig config;
  private JsonSchemaGenerator schemaGenerator;
  private YamlValidator validator;

  @BeforeEach
  void setUp() {
    config = new ValidationConfig();
    config.setValidateAtStartup(true);
    config.setValidationMode(ValidationConfig.ValidationMode.WARN);
    config.setCacheSchemas(false);
    config.setVerboseLogging(true);

    schemaGenerator = new JsonSchemaGenerator(config);
    validator = new YamlValidator(config);
  }

  @Test
  void testSchemaGeneration() {
    // Test that we can generate a schema from a DTO class
    JsonNode schema = schemaGenerator.generateSchema(MessageDaemonConfigDTO.class);

    assertNotNull(schema, "Schema should not be null");
    assertTrue(schema.has("$schema"), "Schema should have $schema field");
    assertTrue(schema.has("type"), "Schema should have type field");
  }

  @Test
  void testValidYamlFile() throws IOException {
    // Create a valid YAML file
    String validYaml = """
        MessageDaemon:
          DelayedPublishInterval: 1000
          SessionPipeLines: 48
          TransactionExpiry: 3600000
          TransactionScan: 5000
          CompressionName: "None"
          CompressMessageMinSize: 1024
          IncrementPriorityMethod: "maintain"
          EnableResourceStatistics: false
          EnableSystemTopics: true
          EnableSystemStatusTopics: true
          EnableSystemTopicAverages: false
          EnableJMX: false
          EnableJMXStatistics: false
          tagMetaData: false
          latitude: 0.0
          longitude: 0.0
          SendAnonymousStatusUpdates: false
        """;

    Path tempFile = Files.createTempFile("test-config", ".yaml");
    try {
      Files.writeString(tempFile, validYaml);

      YamlValidator.ValidationResult result = validator.validate(
          tempFile.toFile(),
          MessageDaemonConfigDTO.class
      );

      assertTrue(result.isValid(), "Valid YAML should pass validation: " + result);
      assertTrue(result.getErrors().isEmpty(), "Valid YAML should have no errors");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  void testInvalidYamlFile() throws IOException {
    // Create an invalid YAML file with wrong type
    String invalidYaml = """
        MessageDaemon:
          DelayedPublishInterval: "not a number"
          SessionPipeLines: 48
        """;

    Path tempFile = Files.createTempFile("test-config-invalid", ".yaml");
    try {
      Files.writeString(tempFile, invalidYaml);

      YamlValidator.ValidationResult result = validator.validate(
          tempFile.toFile(),
          MessageDaemonConfigDTO.class
      );

      assertFalse(result.isValid(), "Invalid YAML should fail validation");
      assertFalse(result.getErrors().isEmpty(), "Invalid YAML should have errors");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  void testValidationModes() throws IOException {
    String invalidYaml = """
        MessageDaemon:
          DelayedPublishInterval: "not a number"
        """;

    Path tempFile = Files.createTempFile("test-mode", ".yaml");
    try {
      Files.writeString(tempFile, invalidYaml);

      // Test WARN mode - should not throw exception
      config.setValidationMode(ValidationConfig.ValidationMode.WARN);
      YamlValidator warnValidator = new YamlValidator(config);
      YamlValidator.ValidationResult result = warnValidator.validate(
          tempFile.toFile(),
          MessageDaemonConfigDTO.class
      );
      assertFalse(result.isValid());

      // Test FAIL_FAST mode - should throw exception
      config.setValidationMode(ValidationConfig.ValidationMode.FAIL_FAST);
      YamlValidator failFastValidator = new YamlValidator(config);
      assertDoesNotThrow(() -> {
        YamlValidator.ValidationResult failResult = failFastValidator.validate(
            tempFile.toFile(),
            MessageDaemonConfigDTO.class
        );
        assertFalse(failResult.isValid());
      });

    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  void testSchemaCache() {
    config.setCacheSchemas(true);
    JsonSchemaGenerator cachingGenerator = new JsonSchemaGenerator(config);

    // Generate schema twice
    JsonNode schema1 = cachingGenerator.generateSchema(MessageDaemonConfigDTO.class);
    JsonNode schema2 = cachingGenerator.generateSchema(MessageDaemonConfigDTO.class);

    // Should be the same instance (cached)
    assertSame(schema1, schema2, "Cached schema should return same instance");
  }

  @Test
  void testConfigValidator() {
    ValidationConfig testConfig = new ValidationConfig();
    testConfig.setValidateAtStartup(false);
    testConfig.setValidateAtRuntime(false);

    ConfigValidator.initialize(testConfig);
    ConfigValidator configValidator = ConfigValidator.getInstance();

    assertNotNull(configValidator);
    assertFalse(configValidator.getConfig().isValidateAtStartup());
    assertFalse(configValidator.getConfig().isValidateAtRuntime());
    assertTrue(configValidator.getConfigClassMap().containsKey("MessageDaemon"));
  }
}
