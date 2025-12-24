# JSON Schema Storage and Examination Guide

## Overview

When the YAML validation system generates JSON schemas from POJO classes, the schemas can be stored both in-memory and on disk for inspection and caching.

## Schema Storage Locations

### 1. In-Memory Cache

Schemas are always cached in-memory during runtime in the `JsonSchemaGenerator` class:
- **Location**: `ConcurrentHashMap<Class<?>, JsonNode> schemaCache`
- **Lifetime**: Exists only during application runtime
- **Purpose**: Fast access without regeneration

### 2. Disk Cache (Configurable)

When `validation.cache.schemas=true` (default), schemas are persisted to disk.

#### Default Location
```
${MAPS_HOME}/schemas/cache/
```

Where `${MAPS_HOME}` is determined by:
1. System property `MAPS_HOME` if set
2. Otherwise, current working directory (`.`)

#### Full Default Path
```
./schemas/cache/
```

#### Schema File Naming Convention
```
<DTOClassName>.schema.json
```

### 3. Example Schema Files

When validation runs, you'll find files like:

```
./schemas/cache/MessageDaemonConfigDTO.schema.json
./schemas/cache/NetworkManagerConfigDTO.schema.json
./schemas/cache/AuthManagerConfigDTO.schema.json
./schemas/cache/DestinationManagerConfigDTO.schema.json
./schemas/cache/DeviceManagerConfigDTO.schema.json
./schemas/cache/DiscoveryManagerConfigDTO.schema.json
./schemas/cache/NetworkConnectionManagerConfigDTO.schema.json
./schemas/cache/SchemaManagerConfigDTO.schema.json
./schemas/cache/SecurityManagerDTO.schema.json
./schemas/cache/TenantManagementConfigDTO.schema.json
./schemas/cache/RestApiManagerConfigDTO.schema.json
./schemas/cache/MLModelManagerDTO.schema.json
./schemas/cache/JolokiaConfigDTO.schema.json
./schemas/cache/RoutingManagerConfigDTO.schema.json
./schemas/cache/LoRaDeviceManagerConfigDTO.schema.json
```

## How to Examine Generated Schemas

### Method 1: Check Disk Cache After Running Tests

1. Run the tests:
   ```bash
   mvn test -Dtest=YamlValidatorTest
   ```

2. Navigate to the cache directory:
   ```bash
   cd ./schemas/cache/
   ```

3. List all generated schemas:
   ```bash
   ls -la *.schema.json
   ```

4. View a specific schema:
   ```bash
   cat MessageDaemonConfigDTO.schema.json
   # Or use jq for pretty formatting:
   jq . MessageDaemonConfigDTO.schema.json
   ```

### Method 2: Run Application with Verbose Logging

Enable verbose logging to see schema generation in real-time:

```bash
java -Dvalidation.verbose=true \
     -Dvalidation.cache.schemas=true \
     -jar target/maps-4.3.0.jar
```

You'll see log messages like:
```
INFO - Generating JSON schema for MessageDaemonConfigDTO
INFO - Saved schema to disk: ./schemas/cache/MessageDaemonConfigDTO.schema.json
```

### Method 3: Programmatically Generate and Print Schema

Create a test or utility class:

```java
import io.mapsmessaging.utilities.configuration.validation.*;
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import com.fasterxml.jackson.databind.JsonNode;

public class SchemaInspector {
    public static void main(String[] args) {
        ValidationConfig config = new ValidationConfig();
        config.setCacheSchemas(true);
        config.setVerboseLogging(true);

        JsonSchemaGenerator generator = new JsonSchemaGenerator(config);
        JsonNode schema = generator.generateSchema(MessageDaemonConfigDTO.class);

        System.out.println(schema.toPrettyString());
    }
}
```

## Configuration Options for Schema Storage

### Disable Disk Caching

```bash
java -Dvalidation.cache.schemas=false -jar target/maps-4.3.0.jar
```

Schemas will only exist in-memory.

### Custom Cache Directory

```bash
java -DMAPS_HOME=/custom/path -jar target/maps-4.3.0.jar
```

Schemas will be stored in:
```
/custom/path/schemas/cache/
```

### Change Cache Subdirectory

Modify `ValidationConfig`:
```java
config.setSchemaCacheDir("custom/schema/path");
```

Schemas will be stored in:
```
${MAPS_HOME}/custom/schema/path/
```

## Schema File Format

Generated schemas follow JSON Schema Draft 2020-12 format. Example structure:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "delayedPublishInterval": {
      "type": "integer",
      "description": "Interval for delayed publish in milliseconds",
      "examples": [1000]
    },
    "sessionPipeLines": {
      "type": "integer",
      "description": "Number of session pipelines",
      "examples": [48]
    },
    "compressionName": {
      "type": "string",
      "description": "Compression algorithm name",
      "examples": ["None"],
      "enum": ["inflator", "none"]
    },
    "enableJMX": {
      "type": "boolean",
      "description": "Enable JMX monitoring",
      "examples": [false]
    }
  },
  "additionalProperties": false
}
```

## Schema Features

The generated schemas include:

### 1. Strict Type Validation
- **Type coercion disabled**: String `"123"` will NOT be accepted for an integer field
- **Strict types**: Field types must match exactly (int, string, boolean, etc.)

### 2. Property Constraints
- **additionalProperties**: false (no extra fields allowed)
- **Required fields**: Based on POJO annotations
- **Enums**: From Swagger `allowableValues` annotations
- **Descriptions**: From Swagger `@Schema` descriptions

### 3. Swagger Integration
- Descriptions from `@Schema(description = "...")`
- Examples from `@Schema(example = "...")`
- Enums from `@Schema(allowableValues = {...})`

## Inspecting Schema During Tests

The unit test `YamlValidatorTest.java` includes a test that generates schemas. You can add debug output:

```java
@Test
void testSchemaGeneration() {
    JsonNode schema = schemaGenerator.generateSchema(MessageDaemonConfigDTO.class);

    // Print schema to console
    System.out.println("Generated Schema:");
    System.out.println(schema.toPrettyString());

    assertNotNull(schema);
    assertTrue(schema.has("$schema"));
    assertTrue(schema.has("type"));
}
```

## Troubleshooting Schema Storage

### Schema Files Not Created

**Check 1**: Verify caching is enabled
```bash
# Should see: true
echo $VALIDATION_CACHE_SCHEMAS
```

**Check 2**: Check directory permissions
```bash
# Should be writable
ls -ld ./schemas/cache/
```

**Check 3**: Check logs for errors
```bash
# Look for schema-related errors
grep -i "schema" logs/application.log
```

### Schema Directory Not Found

**Solution**: The directory is created automatically. If it fails:

```bash
# Manually create it
mkdir -p ./schemas/cache/
chmod 755 ./schemas/cache/
```

## Clearing Schema Cache

### Method 1: Delete Cache Directory
```bash
rm -rf ./schemas/cache/*
```

### Method 2: Programmatically
```java
JsonSchemaGenerator generator = new JsonSchemaGenerator(config);
generator.clearCache(); // Clears both in-memory and disk cache
```

## Schema Versioning

Schemas are regenerated if:
1. The POJO class changes (fields added/removed/modified)
2. Swagger annotations are updated
3. Cache is manually cleared
4. Application restarts with caching disabled

## Quick Reference Commands

```bash
# View all generated schemas
ls -lh ./schemas/cache/

# Count total schemas
ls ./schemas/cache/*.schema.json | wc -l

# Find schemas modified recently
find ./schemas/cache -name "*.schema.json" -mtime -1

# Pretty print a schema
jq . ./schemas/cache/MessageDaemonConfigDTO.schema.json

# Search for a specific property in all schemas
grep -r "delayedPublishInterval" ./schemas/cache/

# Check total cache size
du -sh ./schemas/cache/
```

## Integration with IDEs

### IntelliJ IDEA
1. Install "JSON Schema" plugin
2. Open any `.schema.json` file
3. Right-click → "Validate JSON Schema"

### VS Code
1. Install "JSON Schema Validator" extension
2. Open schema file
3. Auto-validation will occur

## Example: Examining MessageDaemon Schema

After running tests or the application:

```bash
# Navigate to cache
cd ./schemas/cache/

# View the MessageDaemon schema
cat MessageDaemonConfigDTO.schema.json | jq '.'
```

Expected output structure:
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "delayedPublishInterval": { "type": "integer" },
    "sessionPipeLines": { "type": "integer" },
    "transactionExpiry": { "type": "integer" },
    "transactionScan": { "type": "integer" },
    "compressionName": { "type": "string" },
    "compressMessageMinSize": { "type": "integer" },
    "incrementPriorityMethod": { "type": "string" },
    "enableResourceStatistics": { "type": "boolean" },
    "enableSystemTopics": { "type": "boolean" },
    "enableSystemStatusTopics": { "type": "boolean" },
    "enableSystemTopicAverages": { "type": "boolean" },
    "enableJMX": { "type": "boolean" },
    "enableJMXStatistics": { "type": "boolean" },
    "tagMetaData": { "type": "boolean" },
    "latitude": { "type": "number" },
    "longitude": { "type": "number" },
    "sendAnonymousStatusUpdates": { "type": "boolean" }
  },
  "additionalProperties": false
}
```

## Summary

- **Default Location**: `./schemas/cache/<DTOClassName>.schema.json`
- **Configurable**: Via `MAPS_HOME` system property
- **Format**: JSON Schema Draft 2020-12
- **Purpose**: Validation and inspection
- **Caching**: Enabled by default, can be disabled
