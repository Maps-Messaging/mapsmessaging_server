# MAPS Messaging Configuration Tools

This document provides comprehensive information about the configuration, schema, and device management features available in MAPS Messaging Server.

## Overview

The Configuration Tools provide a web-based interface and REST API for managing various aspects of the MAPS Messaging Server, including:

- Server configuration management
- Discovery service configuration and control
- Hardware device management and scanning
- Schema management and validation
- ML model management
- LoRa device configuration and monitoring

## Web Interface

Access the configuration tools web interface at:
```
http://your-server:port/config-tools.html
```

### Features

The web interface provides:

1. **Server Configuration Tab**
   - View and edit server configuration in JSON/YAML format
   - Configuration validation with real-time feedback
   - Diff view to compare configuration changes
   - Download/upload configuration files
   - Rollback prompts for configuration changes

2. **Discovery Tab**
   - Start/stop discovery services
   - View discovered servers with status tiles
   - Configure discovery settings
   - Real-time discovery status monitoring

3. **Hardware Tab**
   - Scan for new hardware devices
   - View active devices with status information
   - Configure hardware subsystem settings
   - Device management controls

4. **Schemas Tab**
   - Create, view, update, and delete schemas
   - Schema validation and preview
   - Upload schema definitions
   - View schema implementation details
   - Schema mapping and format information

5. **Models Tab**
   - Upload and manage ML models
   - Download model files
   - Model metadata inspection

6. **LoRa Devices Tab**
   - Add and configure LoRa gateways and nodes
   - Monitor LoRa device statistics
   - Device configuration management
   - Real-time status monitoring

## REST API Endpoints

### Server Configuration

#### Get Server Configuration
```http
GET /api/v1/server/config
```

**Response:**
```json
{
  "name": "maps-server",
  "version": "1.0.0",
  "port": 8080,
  "host": "0.0.0.0",
  "debug": false,
  "features": {
    "auth": true,
    "ssl": false
  }
}
```

#### Update Server Configuration
```http
PUT /api/v1/server/config
Content-Type: application/json

{
  "name": "updated-server",
  "port": 9090,
  "debug": true
}
```

### Discovery Management

#### Get Discovered Servers
```http
GET /api/v1/server/discovery?filter=schemaSupport=TRUE
```

**Response:**
```json
{
  "list": [
    {
      "name": "server-1",
      "hostname": "192.168.1.100",
      "port": 8080,
      "version": "1.0.0",
      "schemaSupport": true,
      "systemTopicPrefix": "$SYS"
    }
  ]
}
```

#### Start Discovery
```http
PUT /api/v1/server/discovery/start
```

#### Stop Discovery
```http
PUT /api/v1/server/discovery/stop
```

#### Get Discovery Configuration
```http
GET /api/v1/server/discovery/config
```

#### Update Discovery Configuration
```http
POST /api/v1/server/discovery/config
Content-Type: application/json

{
  "enabled": true,
  "scanInterval": 30,
  "serviceType": "_maps._tcp.local"
}
```

### Hardware Management

#### Scan for Hardware
```http
GET /api/v1/server/hardware/scan
```

**Response:**
```json
{
  "list": [
    {
      "name": "I2C-Device-1",
      "type": "I2C",
      "address": "0x48",
      "description": "Temperature sensor"
    }
  ]
}
```

#### Get Active Devices
```http
GET /api/v1/server/hardware
```

**Response:**
```json
{
  "list": [
    {
      "name": "temp-sensor-1",
      "type": "I2C",
      "description": "DS18B20 Temperature Sensor",
      "state": "ACTIVE"
    }
  ]
}
```

#### Get Hardware Configuration
```http
GET /api/v1/server/hardware/config
```

#### Update Hardware Configuration
```http
POST /api/v1/server/hardware/config
Content-Type: application/json

{
  "scanInterval": 60,
  "autoDetect": true,
  "deviceTypes": ["I2C", "SPI", "1-Wire"]
}
```

### Schema Management

#### Get All Schemas
```http
GET /api/v1/server/schema?filter=type=JSON
```

**Response:**
```json
{
  "schemas": [
    {
      "uniqueId": "schema-123",
      "name": "Temperature Reading",
      "type": "JSON",
      "version": 1,
      "context": "/sensors/temperature"
    }
  ]
}
```

#### Get Specific Schema
```http
GET /api/v1/server/schema/{schemaId}
```

#### Get Schema by Context
```http
GET /api/v1/server/schema/context/{context}
```

#### Get Schema by Type
```http
GET /api/v1/server/schema/type/{type}
```

#### Get Schema Implementation Details
```http
GET /api/v1/server/schema/impl/{schemaId}
```

**Response:**
```json
{
  "schemaId": "schema-123",
  "schemaType": "JsonSchemaConfig",
  "schemaName": "Temperature Reading",
  "schemaVersion": 1,
  "interfaceDescription": "json",
  "resourceType": "message",
  "formatterAvailable": true,
  "formatterType": "JsonFormatter"
}
```

#### Create Schema
```http
POST /api/v1/server/schema
Content-Type: application/json

{
  "context": "/sensors/temperature",
  "schema": {
    "type": "object",
    "properties": {
      "temperature": {"type": "number"},
      "unit": {"type": "string"},
      "timestamp": {"type": "string", "format": "date-time"}
    },
    "required": ["temperature", "timestamp"]
  }
}
```

#### Delete Schema
```http
DELETE /api/v1/server/schema/{schemaId}
```

#### Get Supported Formats
```http
GET /api/v1/server/schema/formats
```

**Response:**
```json
["JSON", "XML", "AVRO", "PROTOBUF", "CSV"]
```

#### Get Schema Mapping
```http
GET /api/v1/server/schema/map
```

#### Get Link Format
```http
GET /api/v1/server/schema/link-format
```

### Model Management

#### List All Models
```http
GET /api/v1/server/models
```

**Response:**
```json
["temperature-prediction.onnx", "anomaly-detection.pkl", "classification.model"]
```

#### Get Model Details
```http
GET /api/v1/server/model/{modelName}
```

#### Upload Model
```http
POST /api/v1/server/model/{modelName}
Content-Type: multipart/form-data

file: <model-file>
```

#### Download Model
```http
GET /api/v1/server/model/{modelName}
```

#### Delete Model
```http
DELETE /api/v1/server/model/{modelName}
```

#### Check if Model Exists
```http
HEAD /api/v1/server/model/{modelName}
```

### LoRa Device Management

#### Get All LoRa Devices
```http
GET /api/v1/device/lora
```

**Response:**
```json
{
  "list": [
    {
      "name": "lora-gateway-1",
      "radio": "SX1276",
      "packetsSent": 1500,
      "packetsReceived": 1200,
      "bytesSent": 45000,
      "bytesReceived": 36000,
      "endPointInfoList": [
        {
          "nodeId": 1,
          "lastRSSI": -85,
          "incomingQueueSize": 5,
          "connectionSize": 3
        }
      ]
    }
  ]
}
```

#### Get Specific LoRa Device
```http
GET /api/v1/device/lora/{deviceName}
```

#### Get LoRa Device Configuration
```http
GET /api/v1/device/lora/{deviceName}/config
```

#### Get LoRa Node Connections
```http
GET /api/v1/device/lora/{deviceName}/{nodeId}
```

#### Create LoRa Device Configuration
```http
POST /api/v1/device/lora/config
Content-Type: application/json

{
  "name": "new-lora-device",
  "power": 20,
  "frequency": 868.1,
  "bandwidth": 125000,
  "spreadingFactor": 7,
  "codingRate": 5
}
```

#### Delete LoRa Device Configuration
```http
DELETE /api/v1/device/lora/{deviceName}/config
```

## Configuration Formats

### Server Configuration

The server configuration supports both JSON and YAML formats:

#### JSON Format
```json
{
  "name": "maps-messaging-server",
  "version": "1.0.0",
  "network": {
    "port": 8080,
    "host": "0.0.0.0",
    "ssl": {
      "enabled": false,
      "keystore": "keystore.jks",
      "password": "changeit"
    }
  },
  "authentication": {
    "enabled": true,
    "type": "basic",
    "users": [
      {
        "username": "admin",
        "password": "admin",
        "roles": ["admin"]
      }
    ]
  },
  "features": {
    "discovery": true,
    "hardware": true,
    "ml": false
  }
}
```

#### YAML Format
```yaml
name: maps-messaging-server
version: "1.0.0"
network:
  port: 8080
  host: "0.0.0.0"
  ssl:
    enabled: false
    keystore: keystore.jks
    password: changeit
authentication:
  enabled: true
  type: basic
  users:
    - username: admin
      password: admin
      roles:
        - admin
features:
  discovery: true
  hardware: true
  ml: false
```

### Schema Definitions

Schemas are defined using JSON Schema format:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "title": "Temperature Sensor Reading",
  "description": "Schema for temperature sensor data",
  "properties": {
    "sensorId": {
      "type": "string",
      "description": "Unique identifier for the sensor"
    },
    "temperature": {
      "type": "number",
      "minimum": -50,
      "maximum": 150,
      "description": "Temperature in Celsius"
    },
    "humidity": {
      "type": "number",
      "minimum": 0,
      "maximum": 100,
      "description": "Humidity percentage"
    },
    "timestamp": {
      "type": "string",
      "format": "date-time",
      "description": "Timestamp of the reading"
    },
    "location": {
      "type": "object",
      "properties": {
        "latitude": {"type": "number"},
        "longitude": {"type": "number"}
      }
    }
  },
  "required": ["sensorId", "temperature", "timestamp"]
}
```

### LoRa Device Configuration

LoRa devices are configured with specific radio parameters:

```json
{
  "name": "lora-gateway-1",
  "type": "SX1276",
  "power": 20,
  "frequency": 868.1,
  "bandwidth": 125000,
  "spreadingFactor": 7,
  "codingRate": 5,
  "preambleLength": 8,
  "syncWord": 0x12,
  "crc": true,
  "enableContinuous": false
}
```

## Validation and Error Handling

### Configuration Validation

All configuration submissions are validated before being applied:

1. **JSON/YAML Syntax Validation**
   - Ensures proper syntax parsing
   - Provides detailed error messages for syntax errors

2. **Schema Validation**
   - Validates against expected structure
   - Checks for required fields
   - Validates data types and ranges

3. **Business Logic Validation**
   - Validates port numbers are in valid range
   - Ensures file paths are accessible
   - Checks feature dependencies

### Error Responses

The API returns standard HTTP status codes:

- `200 OK` - Request successful
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `405 Method Not Allowed` - HTTP method not supported
- `415 Unsupported Media Type` - Invalid content type
- `500 Internal Server Error` - Server error

Error response format:
```json
{
  "error": "Configuration validation failed",
  "message": "Port must be between 1 and 65535",
  "details": {
    "field": "network.port",
    "value": 70000,
    "constraint": "min: 1, max: 65535"
  }
}
```

## Pagination and Large Payloads

For endpoints that return large datasets, pagination is supported:

### Query Parameters

- `page` - Page number (default: 1)
- `limit` - Items per page (default: 50, max: 1000)
- `sort` - Sort field
- `order` - Sort order (asc/desc)

### Example

```http
GET /api/v1/server/schema?page=2&limit=20&sort=name&order=asc
```

**Response:**
```json
{
  "items": [...],
  "pagination": {
    "page": 2,
    "limit": 20,
    "total": 150,
    "pages": 8,
    "hasNext": true,
    "hasPrev": true
  }
}
```

## Testing

### Unit Tests

Unit tests are located in:
- `src/test/java/io/mapsmessaging/rest/ConfigToolsTest.java`

Run unit tests:
```bash
mvn test -Dtest=ConfigToolsTest
```

### Integration Tests

Integration tests are located in:
- `src/test/python/config_tools_test.py`

Run Python tests:
```bash
cd src/test/python
python config_tools_test.py
```

### Performance Tests

Performance tests are included in the Python test suite and can be run separately:
```bash
python -c "
from config_tools_test import run_performance_tests
run_performance_tests()
"
```

## Manual Deployment Steps

### Schema Files

Schema files should be deployed to the server's schema directory:

1. **Default Schema Directory**: `{MAPS_HOME}/schemas/`
2. **Custom Schema Directory**: Configure in server config

#### Manual Schema Deployment

1. Create schema file in JSON format
2. Place in schema directory
3. Restart server or use API to reload schemas

Example schema file structure:
```
schemas/
├── temperature.json
├── humidity.json
├── location.json
└── custom/
    ├── sensor-data.json
    └── alerts.json
```

### Configuration Files

Configuration files are typically located in:
- `{MAPS_HOME}/config/`
- `/etc/maps-messaging/`

#### Manual Configuration Deployment

1. Backup existing configuration
2. Update configuration file
3. Validate configuration syntax
4. Restart server

### Model Files

ML models should be deployed to:
- `{MAPS_HOME}/models/`
- Custom directory configured in server config

#### Manual Model Deployment

1. Ensure model file is in supported format (.onnx, .pkl, .h5, etc.)
2. Place in models directory
3. Use API or web interface to register model

## Security Considerations

### Authentication and Authorization

1. **Enable Authentication**: Configure authentication in server config
2. **Role-Based Access**: Assign appropriate roles to users
3. **API Keys**: Use API keys for programmatic access
4. **HTTPS**: Enable SSL/TLS for secure communication

### Configuration Security

1. **Sensitive Data**: Avoid storing passwords in plain text
2. **File Permissions**: Restrict access to configuration files
3. **Backup Security**: Encrypt backup configurations
4. **Audit Logging**: Enable audit logging for configuration changes

### Network Security

1. **Firewall Rules**: Restrict access to management ports
2. **VPN Access**: Use VPN for remote management
3. **IP Whitelisting**: Restrict API access to known IPs
4. **Rate Limiting**: Implement rate limiting for API endpoints

## Troubleshooting

### Common Issues

#### Configuration Not Loading

**Symptoms**: Configuration changes not applied
**Solutions**:
1. Check configuration file syntax
2. Verify file permissions
3. Check server logs for errors
4. Ensure configuration is in correct location

#### Schema Validation Errors

**Symptoms**: Schema creation fails with validation errors
**Solutions**:
1. Validate JSON schema syntax
2. Check required fields
3. Verify data types and constraints
4. Use online JSON Schema validator

#### Hardware Detection Issues

**Symptoms**: Hardware devices not detected
**Solutions**:
1. Check hardware connections
2. Verify device drivers
3. Check user permissions
4. Review hardware configuration

#### LoRa Device Communication Issues

**Symptoms**: LoRa devices not communicating
**Solutions**:
1. Check radio configuration
2. Verify frequency settings
3. Check antenna connections
4. Review power settings

### Logging

Enable debug logging for troubleshooting:

```json
{
  "logging": {
    "level": "DEBUG",
    "packages": [
      "io.mapsmessaging.rest",
      "io.mapsmessaging.engine.schema",
      "io.mapsmessaging.hardware"
    ]
  }
}
```

### Health Checks

Monitor system health using:

```http
GET /api/v1/server/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "database": "UP",
    "discovery": "UP",
    "hardware": "UP",
    "schemaManager": "UP"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Best Practices

### Configuration Management

1. **Version Control**: Store configuration files in version control
2. **Environment Separation**: Use different configs for dev/test/prod
3. **Backup Strategy**: Regular backup of configurations
4. **Change Management**: Document all configuration changes

### Schema Management

1. **Schema Versioning**: Use version numbers for schemas
2. **Backward Compatibility**: Maintain backward compatibility when possible
3. **Documentation**: Document schema fields and constraints
4. **Testing**: Test schemas with sample data

### Device Management

1. **Regular Monitoring**: Monitor device health and status
2. **Maintenance Schedule**: Regular hardware maintenance
3. **Alerting**: Set up alerts for device failures
4. **Documentation**: Document device configurations and connections

### Performance Optimization

1. **Caching**: Enable caching for frequently accessed data
2. **Pagination**: Use pagination for large datasets
3. **Lazy Loading**: Implement lazy loading where appropriate
4. **Monitoring**: Monitor API response times and resource usage

## API Versioning

The configuration tools API follows semantic versioning:

- **v1**: Current stable version
- **v2**: Next major version (breaking changes)

Backward compatibility is maintained within major versions.

## Support and Contributing

For support, questions, or contributions:

1. **Documentation**: Check this documentation first
2. **Issues**: Report issues on the project repository
3. **Community**: Join the community forums
4. **Contributing**: Follow the contribution guidelines

---

*This documentation is for MAPS Messaging Server Configuration Tools v1.0.0*