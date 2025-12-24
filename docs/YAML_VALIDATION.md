# YAML Configuration Validation

## Overview

The Maps Messaging Server now includes automatic YAML configuration validation using JSON Schema. This feature validates all YAML configuration files against schemas automatically generated from POJO (Plain Old Java Object) configuration DTOs.

## Features

- **Automatic Schema Generation**: JSON schemas are automatically generated from configuration DTO classes using their field types and Swagger annotations
- **Startup Validation**: Validates all YAML files when the server starts
- **Runtime Validation**: Optionally validates configuration updates at runtime
- **Configurable Behavior**: Choose how to handle validation failures (fail fast, warn, or skip)
- **Schema Caching**: Generated schemas can be cached to disk for better performance
- **Detailed Error Messages**: Clear error messages showing exactly what validation failed

## Configuration

The validation system is configured via Java system properties:

### System Properties

| Property | Default | Description |
|----------|---------|-------------|
| `validation.startup` | `true` | Enable validation at startup |
| `validation.runtime` | `false` | Enable validation when configs are updated at runtime |
| `validation.mode` | `FAIL_FAST` | Validation failure mode: `FAIL_FAST`, `WARN`, or `SKIP` |
| `validation.cache.schemas` | `true` | Cache generated JSON schemas to disk |
| `validation.verbose` | `false` | Enable verbose validation logging |

### Validation Modes

#### FAIL_FAST (default)
- Server startup is blocked if any YAML file fails validation
- Runtime config updates are rejected if validation fails
- **Use this in production** to ensure all configs are valid

#### WARN
- Validation failures are logged as warnings
- Server continues with default/existing values
- Use this for development or gradual migration

#### SKIP
- Invalid configurations are silently skipped
- Only minimal logging
- Use with caution

## Usage Examples

### Enable Validation at Startup (Default Behavior)

No configuration needed - validation is enabled by default.

```bash
java -jar maps.jar
```

### Disable Startup Validation

```bash
java -Dvalidation.startup=false -jar maps.jar
```

### Enable Runtime Validation

```bash
java -Dvalidation.runtime=true -jar maps.jar
```

### Change Validation Mode to WARN

```bash
java -Dvalidation.mode=WARN -jar maps.jar
```

### Enable Verbose Logging

```bash
java -Dvalidation.verbose=true -jar maps.jar
```

### Disable Schema Caching

```bash
java -Dvalidation.cache.schemas=false -jar maps.jar
```

### Combined Configuration Example

```bash
java -Dvalidation.startup=true \
     -Dvalidation.runtime=true \
     -Dvalidation.mode=FAIL_FAST \
     -Dvalidation.cache.schemas=true \
     -Dvalidation.verbose=true \
     -jar maps.jar
```

## Validated Configuration Files

The following main configuration files are validated:

1. `MessageDaemon.yaml` - Main daemon configuration
2. `AuthManager.yaml` - Authentication configuration
3. `DestinationManager.yaml` - Destination management
4. `DeviceManager.yaml` - Device management
5. `DiscoveryManager.yaml` - Service discovery
6. `NetworkManager.yaml` - Network configuration
7. `NetworkConnectionManager.yaml` - Connection management
8. `SchemaManager.yaml` - Schema configuration
9. `SecurityManager.yaml` - Security settings
10. `TenantManagement.yaml` - Multi-tenant configuration
11. `RestApi.yaml` - REST API configuration
12. `MLModelManager.yaml` - ML model configuration
13. `jolokia.yaml` - JMX configuration
14. `routing.yaml` - Message routing
15. `LoRaDevice.yaml` - LoRa device configuration

## How It Works

1. **Schema Generation**:
   - Configuration DTO classes (e.g., `MessageDaemonConfigDTO`) define the structure
   - Swagger `@Schema` annotations provide descriptions and constraints
   - JSON schemas are automatically generated using the victools jsonschema-generator
   - Schemas respect field types (int, String, boolean, etc.)

2. **Validation**:
   - YAML files are parsed and converted to JSON
   - The JSON is validated against the generated schema
   - Validation errors are collected and reported

3. **Caching** (if enabled):
   - Generated schemas are saved to `${MAPS_HOME}/schemas/cache/`
   - Subsequent startups reuse cached schemas for faster validation
   - Cache is automatically invalidated when DTO classes change

## Example Validation Error

If a YAML file has an invalid configuration:

```yaml
MessageDaemon:
  DelayedPublishInterval: "not a number"  # Should be an integer
  SessionPipeLines: 48
```

You'll see an error like:

```
ERROR - Validation failed for MessageDaemon:
$.DelayedPublishInterval: string found, integer expected
```

## Programmatic Usage

### Validate a Single Configuration

```java
import io.mapsmessaging.utilities.configuration.validation.*;
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import java.io.File;

// Create validator with custom config
ValidationConfig config = new ValidationConfig();
config.setValidationMode(ValidationConfig.ValidationMode.FAIL_FAST);
YamlValidator validator = new YamlValidator(config);

// Validate a file
File yamlFile = new File("MessageDaemon.yaml");
YamlValidator.ValidationResult result = validator.validate(
    yamlFile,
    MessageDaemonConfigDTO.class
);

if (result.isValid()) {
    System.out.println("Configuration is valid");
} else {
    System.out.println("Validation errors:");
    result.getErrors().forEach(System.out::println);
}
```

### Generate a JSON Schema

```java
import io.mapsmessaging.utilities.configuration.validation.*;
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import com.fasterxml.jackson.databind.JsonNode;

ValidationConfig config = new ValidationConfig();
JsonSchemaGenerator generator = new JsonSchemaGenerator(config);

// Generate schema
JsonNode schema = generator.generateSchema(MessageDaemonConfigDTO.class);
System.out.println(schema.toPrettyString());
```

## Architecture

### Classes

- **`ValidationConfig`**: Configuration for validation behavior
- **`JsonSchemaGenerator`**: Generates JSON schemas from POJO classes
- **`YamlValidator`**: Validates YAML files against schemas
- **`ConfigValidator`**: High-level validator integrated with ConfigurationManager
- **`ConfigValidator.ValidationResult`**: Result object containing validation status and errors
- **`YamlValidator.ConfigValidationException`**: Exception thrown in FAIL_FAST mode

### Dependencies

The validation system uses:
- `victools/jsonschema-generator` - Schema generation from Java classes
- `victools/jsonschema-module-jackson` - Jackson annotation support
- `victools/jsonschema-module-swagger-2` - Swagger annotation support
- `networknt/json-schema-validator` - JSON Schema validation
- `jackson-dataformat-yaml` - YAML parsing

## Best Practices

1. **Use FAIL_FAST in production** to catch configuration errors early
2. **Enable verbose logging during development** to understand validation details
3. **Keep schemas cached** for better performance unless actively modifying DTOs
4. **Add Swagger annotations to DTOs** for better schema documentation and validation
5. **Test configuration changes** in a dev environment with validation enabled

## Troubleshooting

### Validation Always Passes Even With Invalid YAML

- Check that validation is enabled: `validation.startup=true`
- Ensure the YAML file is in the resources directory
- Verify the configuration name matches the DTO class mapping

### Validation Fails on Startup

- Review the error messages to identify the invalid field
- Check the DTO class to see the expected type
- Correct the YAML file and restart

### Schema Cache Issues

- Clear the cache: `rm -rf ${MAPS_HOME}/schemas/cache/*`
- Restart with cache disabled: `-Dvalidation.cache.schemas=false`
- Rebuild schemas: restart the server

## Future Enhancements

Potential future improvements:

- Web UI for viewing validation errors
- Auto-correction suggestions
- Schema documentation generation
- Custom validation rules via annotations
- Real-time validation in configuration editor
