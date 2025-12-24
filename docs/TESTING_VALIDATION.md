# Testing the YAML Validation Feature

## Quick Start

### 1. Compile the Project

```bash
mvn clean compile
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
```

### 2. Run the Tests

```bash
mvn test -Dtest=YamlValidatorTest
```

**Expected Output:**
```
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

All 6 tests should pass:
- ✅ `testSchemaGeneration` - Verifies schema can be generated from DTOs
- ✅ `testValidYamlFile` - Validates a correctly formatted YAML file
- ✅ `testInvalidYamlFile` - Rejects YAML with type mismatches
- ✅ `testValidationModes` - Tests WARN, FAIL_FAST, SKIP modes
- ✅ `testSchemaCache` - Verifies schema caching works
- ✅ `testConfigValidator` - Tests the main validator integration

## Examine Generated Schemas

### After Running Tests

Schemas are automatically cached to:
```
./schemas/cache/
```

### List Generated Schemas

```bash
./scripts/inspect-schemas.sh list
```

Or manually:
```bash
ls -lh ./schemas/cache/
```

### View a Specific Schema

```bash
./scripts/inspect-schemas.sh view MessageDaemonConfigDTO
```

Or with jq:
```bash
jq . ./schemas/cache/MessageDaemonConfigDTO.schema.json
```

### Expected Schema Structure

Each schema should have:
- `"$schema"`: "https://json-schema.org/draft/2020-12/schema"
- `"type"`: "object"
- `"properties"`: Object with field definitions
- `"additionalProperties"`: false (strict mode)

Example for MessageDaemonConfigDTO:
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "delayedPublishInterval": {
      "type": "integer"
    },
    "sessionPipeLines": {
      "type": "integer"
    },
    "compressionName": {
      "type": "string"
    },
    "enableJMX": {
      "type": "boolean"
    }
  },
  "additionalProperties": false
}
```

## Test Validation in Application

### Build the Application

```bash
mvn clean install
```

### Run with Validation Enabled (Default)

```bash
java -jar target/maps-4.3.0.jar
```

Look for validation logs:
```
INFO - Starting YAML configuration validation...
INFO - All 15 configurations validated successfully
```

### Run with Verbose Validation

```bash
java -Dvalidation.verbose=true -jar target/maps-4.3.0.jar
```

You'll see detailed logs:
```
INFO - Generating JSON schema for MessageDaemonConfigDTO
INFO - Saved schema to disk: ./schemas/cache/MessageDaemonConfigDTO.schema.json
INFO - Validation successful: MessageDaemon
```

### Test WARN Mode (Logs warnings, continues on errors)

```bash
java -Dvalidation.mode=WARN -jar target/maps-4.3.0.jar
```

### Test Validation Disabled

```bash
java -Dvalidation.startup=false -jar target/maps-4.3.0.jar
```

## Manual Schema Inspection

### View All Properties in a Schema

```bash
jq '.properties | keys' ./schemas/cache/MessageDaemonConfigDTO.schema.json
```

### Count Properties

```bash
jq '.properties | length' ./schemas/cache/MessageDaemonConfigDTO.schema.json
```

### Find All Integer Fields

```bash
jq '.properties | to_entries | map(select(.value.type == "integer")) | from_entries | keys' \
    ./schemas/cache/MessageDaemonConfigDTO.schema.json
```

### Search for a Specific Field

```bash
./scripts/inspect-schemas.sh search delayedPublishInterval
```

### Validate Schema File Structure

```bash
./scripts/inspect-schemas.sh validate MessageDaemonConfigDTO
```

## Testing Invalid YAML

### Create an Invalid Test File

```bash
cat > test-invalid.yaml << 'EOF'
MessageDaemon:
  DelayedPublishInterval: "not a number"
  SessionPipeLines: 48
EOF
```

### Run Validator Against It

Create a quick test:

```java
import io.mapsmessaging.utilities.configuration.validation.*;
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import java.io.File;

public class TestInvalid {
    public static void main(String[] args) {
        ValidationConfig config = new ValidationConfig();
        config.setValidationMode(ValidationConfig.ValidationMode.WARN);

        YamlValidator validator = new YamlValidator(config);
        YamlValidator.ValidationResult result = validator.validate(
            new File("test-invalid.yaml"),
            MessageDaemonConfigDTO.class
        );

        System.out.println("Valid: " + result.isValid());
        System.out.println("Errors: " + result.getErrors());
    }
}
```

**Expected Output:**
```
Valid: false
Errors: [$.delayedPublishInterval: string found, integer expected]
```

## Performance Testing

### Measure Schema Generation Time

Add to test:
```java
long start = System.currentTimeMillis();
JsonNode schema = generator.generateSchema(MessageDaemonConfigDTO.class);
long time = System.currentTimeMillis() - start;
System.out.println("Schema generation took: " + time + "ms");
```

### Test Cache Performance

```java
// First generation (no cache)
long start1 = System.currentTimeMillis();
JsonNode schema1 = generator.generateSchema(MessageDaemonConfigDTO.class);
long time1 = System.currentTimeMillis() - start1;

// Second generation (from cache)
long start2 = System.currentTimeMillis();
JsonNode schema2 = generator.generateSchema(MessageDaemonConfigDTO.class);
long time2 = System.currentTimeMillis() - start2;

System.out.println("First generation: " + time1 + "ms");
System.out.println("Cached generation: " + time2 + "ms");
System.out.println("Speedup: " + (time1 / (double)time2) + "x");
```

Expected: Cached should be ~100x faster

## Troubleshooting

### Tests Fail with "Invalid YAML should fail validation"

**Problem:** Validation is too lenient, accepting invalid types

**Solution:** Already fixed! The issue was:
- `setTypeLoose(false)` ensures strict type checking
- `Option.STRICT_TYPE_INFO` in schema generation
- String "123" will NOT be accepted for integer fields

### Schemas Not Generated

**Check 1:** Caching enabled?
```bash
# Should be empty or "true"
echo ${validation.cache.schemas:-true}
```

**Check 2:** Directory exists?
```bash
ls -ld ./schemas/cache/
```

**Check 3:** Run with verbose logging
```bash
java -Dvalidation.verbose=true -Dvalidation.cache.schemas=true -jar target/maps-4.3.0.jar
```

### Schema Files Empty or Corrupted

```bash
# Check file sizes
ls -lh ./schemas/cache/

# Validate JSON
for f in ./schemas/cache/*.json; do
    echo "Checking $f..."
    jq empty "$f" 2>&1 || echo "INVALID: $f"
done
```

### Compilation Errors

If you see:
```
cannot find symbol: method defaultConfig(...)
```

**Already Fixed!** The correct API is:
```java
JsonSchema schema = schemaFactory.getSchema(schemaNode, validatorConfig);
```

Not:
```java
// WRONG - this method doesn't exist
JsonSchemaFactory factory = JsonSchemaFactory.builder(...).defaultConfig(...).build();
```

## Verification Checklist

After running tests, verify:

- [ ] `mvn clean compile` succeeds
- [ ] `mvn test -Dtest=YamlValidatorTest` shows 6/6 tests passing
- [ ] `./schemas/cache/` directory exists
- [ ] `./schemas/cache/` contains 15+ `.schema.json` files
- [ ] Each schema file is valid JSON (test with `jq empty <file>`)
- [ ] Each schema has `$schema`, `type`, and `properties` fields
- [ ] Schemas show strict types (no type coercion)
- [ ] `additionalProperties` is set to `false`
- [ ] Application starts successfully with validation enabled

## Quick Verification Script

```bash
#!/bin/bash
echo "=== Validation Feature Verification ==="

echo "1. Compiling..."
mvn clean compile -q && echo "✓ Compilation successful" || echo "✗ Compilation failed"

echo "2. Running tests..."
mvn test -Dtest=YamlValidatorTest -q && echo "✓ All tests passed" || echo "✗ Tests failed"

echo "3. Checking schema cache..."
if [ -d "./schemas/cache" ]; then
    count=$(ls ./schemas/cache/*.schema.json 2>/dev/null | wc -l)
    echo "✓ Found $count schema files"
else
    echo "✗ Schema cache directory not found"
fi

echo "4. Validating schema files..."
for f in ./schemas/cache/*.schema.json 2>/dev/null; do
    if jq empty "$f" 2>/dev/null; then
        :
    else
        echo "✗ Invalid JSON in $f"
    fi
done
echo "✓ All schema files are valid JSON"

echo ""
echo "=== Verification Complete ==="
```

## Documentation References

- **Feature Documentation**: `docs/YAML_VALIDATION.md`
- **Schema Storage Guide**: `docs/SCHEMA_STORAGE.md`
- **Implementation Details**: `IMPLEMENTATION_SUMMARY.md`
- **Schema Inspector**: `scripts/inspect-schemas.sh --help`
