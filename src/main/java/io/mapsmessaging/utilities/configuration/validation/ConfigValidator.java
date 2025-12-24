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

import io.mapsmessaging.dto.rest.auth.SecurityManagerDTO;
import io.mapsmessaging.dto.rest.config.*;
import io.mapsmessaging.dto.rest.config.ml.MLModelManagerDTO;
import io.mapsmessaging.dto.rest.schema.SchemaManagerConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import static io.mapsmessaging.logging.ServerLogMessages.*;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Central configuration validator that manages validation of all YAML configuration files.
 * Integrates with ConfigurationManager to provide startup and runtime validation.
 */
public class ConfigValidator {

  private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);
  private static ConfigValidator instance;

  private final ValidationConfig config;
  private final YamlValidator validator;
  private final Map<String, Class<?>> configClassMap;

  private ConfigValidator(ValidationConfig config) {
    this.config = config;
    this.validator = new YamlValidator(config);
    this.configClassMap = buildConfigClassMap();
  }

  /**
   * Get or create the singleton instance with default configuration.
   */
  public static synchronized ConfigValidator getInstance() {
    if (instance == null) {
      instance = new ConfigValidator(loadValidationConfig());
    }
    return instance;
  }

  /**
   * Initialize with custom configuration (for testing).
   */
  public static synchronized void initialize(ValidationConfig config) {
    instance = new ConfigValidator(config);
  }

  /**
   * Validate all YAML configuration files at startup.
   *
   * @param resourcePath Path to directory containing YAML files
   * @return true if all validations passed or validation is disabled
   */
  public boolean validateAtStartup(String resourcePath) {
    if (!config.isValidateAtStartup()) {
      logger.log(CONFIG_VALIDATION_DISABLED);
      return true;
    }

    logger.log(CONFIG_VALIDATION_STARTING);
    Path configPath = Paths.get(resourcePath);

    Map<String, YamlValidator.ValidationResult> results = validator.validateAll(configPath, configClassMap);

    long failureCount = results.values().stream().filter(r -> !r.isValid()).count();

    if (failureCount == 0) {
      logger.log(CONFIG_VALIDATION_SUCCESS, results.size());
      return true;
    } else {
      logger.log(CONFIG_VALIDATION_FAILURES, failureCount);
      // Exception will be thrown by validator if in FAIL_FAST mode
      return config.getValidationMode() != ValidationConfig.ValidationMode.FAIL_FAST;
    }
  }

  /**
   * Validate a single configuration at runtime.
   *
   * @param configName Name of the configuration (e.g., "MessageDaemon")
   * @param yamlInput YAML input stream
   * @return true if validation passed or runtime validation is disabled
   */
  public boolean validateAtRuntime(String configName, InputStream yamlInput) {
    if (!config.isValidateAtRuntime()) {
      if (config.isVerboseLogging()) {
        logger.log(CONFIG_VALIDATOR_RUNTIME_DISABLED, configName);
      }
      return true;
    }

    Class<?> configClass = configClassMap.get(configName);
    if (configClass == null) {
      logger.log(CONFIG_VALIDATOR_NO_CLASS, configName);
      return true; // Don't block unknown configs
    }

    YamlValidator.ValidationResult result = validator.validate(yamlInput, configName, configClass);

    if (!result.isValid()) {
      handleRuntimeValidationFailure(configName, result);
      return config.getValidationMode() != ValidationConfig.ValidationMode.FAIL_FAST;
    }

    return true;
  }

  /**
   * Validate a single YAML file.
   *
   * @param configName Name of the configuration
   * @param yamlFile YAML file to validate
   * @return ValidationResult
   */
  public YamlValidator.ValidationResult validateFile(String configName, File yamlFile) {
    Class<?> configClass = configClassMap.get(configName);
    if (configClass == null) {
      return YamlValidator.ValidationResult.error("No configuration class found for: " + configName);
    }

    return validator.validate(yamlFile, configClass);
  }

  private void handleRuntimeValidationFailure(String configName, YamlValidator.ValidationResult result) {
    String errorMsg = String.join("\n", result.getErrors());

    switch (config.getValidationMode()) {
      case FAIL_FAST:
        logger.log(CONFIG_VALIDATOR_RUNTIME_FAILURE, configName, errorMsg);
        throw new YamlValidator.ConfigValidationException("Runtime validation failed for " + configName + ":\n" + errorMsg, result.getErrors());

      case WARN:
        logger.log(CONFIG_VALIDATOR_RUNTIME_FAILURE, configName, errorMsg);
        logger.log(CONFIG_VALIDATOR_RUNTIME_WARN, configName);
        break;

      case SKIP:
        logger.log(CONFIG_VALIDATOR_SKIP, configName);
        if (config.isVerboseLogging()) {
          logger.log(CONFIG_VALIDATOR_RUNTIME_FAILURE, configName, errorMsg);
        }
        break;
    }
  }

  /**
   * Build the mapping between configuration names and their DTO classes.
   * This maps the main 17 configuration files to their corresponding DTOs.
   */
  private Map<String, Class<?>> buildConfigClassMap() {
    Map<String, Class<?>> map = new HashMap<>();

    // Main configuration files
    map.put("MessageDaemon", MessageDaemonConfigDTO.class);
    map.put("AuthManager", AuthManagerConfigDTO.class);
    map.put("DestinationManager", DestinationManagerConfigDTO.class);
    map.put("DeviceManager", DeviceManagerConfigDTO.class);
    map.put("DiscoveryManager", DiscoveryManagerConfigDTO.class);
    map.put("NetworkManager", NetworkManagerConfigDTO.class);
    map.put("NetworkConnectionManager", NetworkConnectionManagerConfigDTO.class);
    map.put("SchemaManager", SchemaManagerConfigDTO.class);
    map.put("SecurityManager", SecurityManagerDTO.class);
    map.put("TenantManagement", TenantManagementConfigDTO.class);
    map.put("RestApi", RestApiManagerConfigDTO.class);
    map.put("MLModelManager", MLModelManagerDTO.class);
    map.put("jolokia", JolokiaConfigDTO.class);
    map.put("routing", RoutingManagerConfigDTO.class);
    map.put("LoRaDevice", LoRaDeviceManagerConfigDTO.class);

    return map;
  }

  /**
   * Load validation configuration from system properties or use defaults.
   */
  private static ValidationConfig loadValidationConfig() {
    ValidationConfig config = new ValidationConfig();

    // Load from system properties if present
    String validateStartup = System.getProperty("validation.startup", "true");
    String validateRuntime = System.getProperty("validation.runtime", "false");
    String validationMode = System.getProperty("validation.mode", "FAIL_FAST");
    String cacheSchemas = System.getProperty("validation.cache.schemas", "true");
    String verboseLogging = System.getProperty("validation.verbose", "false");

    config.setValidateAtStartup(Boolean.parseBoolean(validateStartup));
    config.setValidateAtRuntime(Boolean.parseBoolean(validateRuntime));
    config.setCacheSchemas(Boolean.parseBoolean(cacheSchemas));
    config.setVerboseLogging(Boolean.parseBoolean(verboseLogging));

    try {
      config.setValidationMode(ValidationConfig.ValidationMode.valueOf(validationMode));
    } catch (IllegalArgumentException e) {
      LoggerFactory.getLogger(ConfigValidator.class).log(CONFIG_VALIDATION_INVALID_MODE, validationMode);
      config.setValidationMode(ValidationConfig.ValidationMode.FAIL_FAST);
    }

    return config;
  }

  /**
   * Get the current validation configuration.
   */
  public ValidationConfig getConfig() {
    return config;
  }

  /**
   * Get the map of configuration names to their DTO classes.
   */
  public Map<String, Class<?>> getConfigClassMap() {
    return new HashMap<>(configClassMap);
  }
}
