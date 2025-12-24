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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.*;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import static io.mapsmessaging.logging.ServerLogMessages.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Validates YAML configuration files against JSON schemas generated from POJOs.
 */
public class YamlValidator {

  private static final Logger logger = LoggerFactory.getLogger(YamlValidator.class);
  private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  private static final ObjectMapper jsonMapper = new ObjectMapper();

  private final JsonSchemaGenerator schemaGenerator;
  private final ValidationConfig config;
  private final JsonSchemaFactory schemaFactory;

  public YamlValidator(ValidationConfig config) {
    this.config = config;
    this.schemaGenerator = new JsonSchemaGenerator(config);
    this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  }

  /**
   * Validate a YAML file against a POJO class schema.
   *
   * @param yamlFile The YAML file to validate
   * @param configClass The POJO class defining the schema
   * @return ValidationResult with success status and any errors
   */
  public ValidationResult validate(File yamlFile, Class<?> configClass) {
    try {
      return validateInternal(yamlFile, configClass);
    } catch (Exception e) {
      return handleException(yamlFile, e);
    }
  }

  /**
   * Validate a YAML input stream against a POJO class schema.
   *
   * @param yamlInput The YAML input stream
   * @param configName Name of the configuration (for logging)
   * @param configClass The POJO class defining the schema
   * @return ValidationResult with success status and any errors
   */
  public ValidationResult validate(InputStream yamlInput, String configName, Class<?> configClass) {
    try {
      return validateInternal(yamlInput, configName, configClass);
    } catch (Exception e) {
      return handleException(configName, e);
    }
  }

  /**
   * Validate all YAML files in a directory against their corresponding config classes.
   *
   * @param directory Directory containing YAML files
   * @param configClassMap Map of config names to their POJO classes
   * @return Map of file names to validation results
   */
  public Map<String, ValidationResult> validateAll(Path directory, Map<String, Class<?>> configClassMap) {
    Map<String, ValidationResult> results = new LinkedHashMap<>();

    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      logger.log(CONFIG_VALIDATION_DIRECTORY_NOT_EXIST, directory);
      return results;
    }

    configClassMap.forEach((configName, configClass) -> {
      File yamlFile = directory.resolve(configName + ".yaml").toFile();
      if (yamlFile.exists()) {
        ValidationResult result = validate(yamlFile, configClass);
        results.put(configName, result);

        if (!result.isValid()) {
          handleValidationFailure(configName, result);
        }
      } else {
        if (config.isVerboseLogging()) {
          logger.log(YAML_FILE_NOT_FOUND, yamlFile);
        }
      }
    });

    return results;
  }

  private ValidationResult validateInternal(File yamlFile, Class<?> configClass) throws IOException {
    if (!yamlFile.exists()) {
      return ValidationResult.error("YAML file not found: " + yamlFile.getAbsolutePath());
    }

    JsonNode yamlJson = yamlMapper.readTree(yamlFile);
    return performValidation(yamlFile.getName(), yamlJson, configClass);
  }

  private ValidationResult validateInternal(InputStream yamlInput, String configName, Class<?> configClass) throws IOException {
    JsonNode yamlJson = yamlMapper.readTree(yamlInput);
    return performValidation(configName, yamlJson, configClass);
  }

  private ValidationResult performValidation(String configName, JsonNode yamlJson, Class<?> configClass) {
    // Generate JSON schema from POJO
    JsonNode schemaNode = schemaGenerator.generateSchema(configClass);

    // Configure strict type validation
    SchemaValidatorsConfig validatorConfig = new SchemaValidatorsConfig();
    validatorConfig.setTypeLoose(false); // Strict type checking - don't allow type coercion
    validatorConfig.setFailFast(false); // Collect all errors

    JsonSchema schema = schemaFactory.getSchema(schemaNode, validatorConfig);

    // Extract the root configuration object
    // Most YAML files have structure: ConfigName: { properties... }
    String rootKey = extractRootKey(configName);
    JsonNode configData = yamlJson.has(rootKey) ? yamlJson.get(rootKey) : yamlJson;

    // Validate
    Set<ValidationMessage> errors = schema.validate(configData);

    if (errors.isEmpty()) {
      if (config.isVerboseLogging()) {
        logger.log(CONFIG_VALIDATION_SUCCESS_FILE, configName);
      }
      return ValidationResult.success();
    } else {
      return ValidationResult.failure(formatErrors(errors));
    }
  }

  private String extractRootKey(String configName) {
    // Remove .yaml extension if present
    if (configName.endsWith(".yaml") || configName.endsWith(".yml")) {
      configName = configName.substring(0, configName.lastIndexOf('.'));
    }
    return configName;
  }

  private List<String> formatErrors(Set<ValidationMessage> errors) {
    List<String> formatted = new ArrayList<>();
    for (ValidationMessage error : errors) {
      formatted.add(error.getMessage());
    }
    return formatted;
  }

  private ValidationResult handleException(File yamlFile, Exception e) {
    return handleException(yamlFile.getName(), e);
  }

  private ValidationResult handleException(String configName, Exception e) {
    logger.log(CONFIG_VALIDATION_EXCEPTION, configName, e.getMessage(), e);
    return ValidationResult.error("Exception during validation of " + configName + ": " + e.getMessage());
  }

  private void handleValidationFailure(String configName, ValidationResult result) {
    String errorMsg = String.join("\n", result.getErrors());

    switch (config.getValidationMode()) {
      case FAIL_FAST:
        logger.log(CONFIG_VALIDATOR_RUNTIME_FAILURE, configName, errorMsg);
        throw new ConfigValidationException("Validation failed for " + configName + ":\n" + errorMsg, result.getErrors());

      case WARN:
        logger.log(CONFIG_VALIDATOR_RUNTIME_FAILURE, configName, errorMsg);
        logger.log(CONFIG_VALIDATION_CONTINUING, configName);
        break;

      case SKIP:
        logger.log(CONFIG_VALIDATION_SKIP_INVALID, configName);
        if (config.isVerboseLogging()) {
          logger.log(CONFIG_VALIDATOR_RUNTIME_FAILURE, configName, errorMsg);
        }
        break;
    }
  }

  /**
   * Result of a YAML validation.
   */
  public static class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
      this.valid = valid;
      this.errors = errors != null ? errors : Collections.emptyList();
    }

    public static ValidationResult success() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult failure(List<String> errors) {
      return new ValidationResult(false, errors);
    }

    public static ValidationResult error(String error) {
      return new ValidationResult(false, Collections.singletonList(error));
    }

    public boolean isValid() {
      return valid;
    }

    public List<String> getErrors() {
      return errors;
    }

    @Override
    public String toString() {
      if (valid) {
        return "Validation: SUCCESS";
      } else {
        return "Validation: FAILED\n" + String.join("\n", errors);
      }
    }
  }

  /**
   * Exception thrown when validation fails in FAIL_FAST mode.
   */
  public static class ConfigValidationException extends RuntimeException {
    private final List<String> validationErrors;

    public ConfigValidationException(String message, List<String> validationErrors) {
      super(message);
      this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
      return validationErrors;
    }
  }
}
