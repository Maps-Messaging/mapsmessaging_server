# Maps Messaging Server - Documentation Gap Analysis Report

**Generated:** 2026-02-04
**Source Code Version:** 4.1.1
**Analysis Scope:** Complete server codebase, configuration files, and in-repository documentation

---

## Executive Summary

This report provides a comprehensive analysis of the Maps Messaging Server documentation completeness by comparing the source code implementation against available documentation. The analysis reveals that while the server has extensive capabilities (15+ protocols, 100+ REST endpoints, advanced IoT features), the documentation coverage has significant gaps.

**Key Findings:**
- ✅ **Strong Areas:** Protocol overview (PROTOCOLS.md), basic getting started
- ⚠️ **Partial Coverage:** Configuration examples in YAML files
- ❌ **Critical Gaps:** REST API documentation, hardware integration guides, ML features, complete configuration reference, end-to-end tutorials

**Documentation Site Access Note:** The docs.mapsmessaging.io site returned a 403 error during analysis. This report is based on in-repository documentation and references to the docs site found in README.md.

---

## Table of Contents

1. [Current Documentation Inventory](#current-documentation-inventory)
2. [Source Code Capabilities](#source-code-capabilities)
3. [Gap Analysis by Category](#gap-analysis-by-category)
4. [Priority Recommendations](#priority-recommendations)
5. [Suggested Documentation Content](#suggested-documentation-content)

---

## 1. Current Documentation Inventory

### In-Repository Documentation

| File | Lines | Status | Coverage |
|------|-------|--------|----------|
| **README.md** | 100 | ✅ Complete | Good overview, feature highlights, basic example |
| **PROTOCOLS.md** | 175 | ✅ Complete | Excellent protocol reference with code pointers |
| **satellite_modem_overview.md** | 123 | ✅ Complete | Comprehensive satellite integration architecture |
| **SECURITY.md** | 12 | ⚠️ Minimal | Security policy only, no security configuration guide |
| **docs/Architecture.md** | 0 | ❌ Empty | Placeholder file with no content |
| **debian_package_readme.md** | - | ⚠️ Partial | Deployment-specific |
| **buildkite_agent_setup.md** | - | ⚠️ Partial | CI/CD only |

### Configuration Files (Embedded Documentation)

All 17 YAML configuration files contain inline comments explaining options:
- ✅ NetworkManager.yaml (243 lines) - Well commented
- ✅ MessageDaemon.yaml (115 lines) - Well commented
- ✅ DestinationManager.yaml (96 lines) - Well commented
- ✅ AuthManager.yaml (45 lines) - Well commented
- ✅ RestApi.yaml (65 lines) - Well commented
- ✅ DeviceManager.yaml (92 lines) - Well commented
- ✅ LoRaDevice.yaml (50 lines) - Well commented
- ✅ MLModelManager.yaml (65 lines) - Well commented
- ✅ NetworkConnectionManager.yaml (103 lines) - Well commented with examples
- ✅ TenantManagement.yaml (52 lines) - Well commented
- ✅ TransformationManager.yaml (30 lines) - Well commented
- Plus 6 more configuration files

### Referenced External Documentation

README.md references docs.mapsmessaging.io with links to:
- Introduction and supported protocols
- Server integrations overview
- Various GitHub repositories for subsystems

---

## 2. Source Code Capabilities

### 2.1 Protocol Support (15+ Protocols)

**Messaging Protocols:**
1. MQTT v3.1.1 (tcp://:::1883/, ssl://0.0.0.0:1892/)
2. MQTT v5.0 (shared endpoints with v3.1.1)
3. AMQP 1.0 (tcp://0.0.0.0:5672/, ssl://0.0.0.0:5692/)
4. STOMP 1.1/1.2 (tcp://0.0.0.0:8674/, ssl://0.0.0.0:8695/)
5. NATS v2.0 (tcp://0.0.0.0:4222/)
6. CoAP (udp://0.0.0.0:5683/, dtls://0.0.0.0:5684/)
7. MQTT-SN v1.2 & v2.0 (udp://0.0.0.0:1884/, dtls://0.0.0.0:1886/)
8. WebSocket (RFC 6455) - wraps MQTT, AMQP, STOMP

**IoT Protocols:**
9. LoRaWAN Semtech Gateway
10. LoRa Direct (native RFM95 radio)
11. NMEA-0183 (GPS/Marine data)
12. Satellite Modems (Inmarsat IoT nano, OrbComm OGWS)

**Utility Protocols:**
13. REST API
14. Local Loop
15. Echo
16. Proxy

### 2.2 REST API Endpoints (100+ Endpoints)

**Management Categories:**
- Authentication & Authorization (12 endpoints)
- Destination Management (2 endpoints)
- Connection Management (3 endpoints)
- Server Management (10 endpoints)
- Interface Management (18 endpoints)
- Discovery Management (5 endpoints)
- Hardware Management (4 endpoints)
- Integration Management (16 endpoints)
- LoRa Device Management (9 endpoints)
- Logging Monitor (3 endpoints with SSE)
- Messaging Interface (9 endpoints)
- Schema Management (11 endpoints)
- ML Model Store (5 endpoints)

**Total:** 107 documented REST endpoints

### 2.3 Advanced Features

**Hardware Integration:**
- I2C bus support (multiple buses, auto-scan, manual configuration)
- 1-Wire devices (temperature sensors, iButtons)
- SPI devices (MCP3y0x ADCs, custom devices)
- Serial devices (NMEA, custom sensors)
- LoRa radios (RFM95 via GPIO or serial)
- Trigger system (cron-based or periodic)

**Machine Learning:**
- Model store (Nexus/S3/File repository)
- Event stream processing
- Anomaly detection (isolation forest)
- Auto-retraining with threshold-based updates
- LLM integration (GPT-4.1)

**Server-to-Server:**
- Multi-protocol outbound connections
- Namespace mapping (local ↔ remote)
- Bidirectional data flow
- Schema propagation
- Auto-discovery via mDNS

**Storage & Persistence:**
- Three storage types: Partition, Memory, MemoryTier
- S3 archival with digest verification
- Configurable sync modes
- Cache layers (WeakReference or JCS)
- Write-through caching

**Security:**
- JAAS-based authentication
- Multiple auth providers (Encrypted-Auth, LDAP, JWT, Auth0)
- TLS/DTLS support
- Client certificate authentication
- ACL-based authorization
- Security domains per protocol

**Monitoring:**
- JMX beans
- Jolokia (JMX over HTTP)
- Prometheus metrics
- $SYS topics (MQTT-compatible)
- Health endpoints
- SSE log streaming

---

## 3. Gap Analysis by Category

### 3.1 CRITICAL GAPS - HIGH PRIORITY

#### ❌ REST API Documentation
**Current State:** No comprehensive REST API documentation found
**Gap:** 107 REST endpoints are undocumented externally
**Impact:** Users cannot effectively use management APIs

**What's Needed:**
- Complete REST API reference with all 107 endpoints
- Request/response schemas for each endpoint
- Authentication requirements
- Example requests using curl/Postman
- Error codes and troubleshooting
- OpenAPI/Swagger UI (enabled in config but not documented)

#### ❌ Configuration Reference Guide
**Current State:** Inline YAML comments only
**Gap:** No consolidated configuration reference
**Impact:** Users must read source code to understand all options

**What's Needed:**
- Complete configuration reference for all 17 YAML files
- Parameter descriptions with default values
- Value ranges and validation rules
- Configuration examples for common scenarios
- Token substitution guide ({{MAPS_DATA}}, {{MAPS_HOME}}, {user}, {protocol})
- Configuration precedence (file vs Consul vs environment variables)

#### ❌ Hardware Integration Guide
**Current State:** Configuration examples only
**Gap:** No tutorial on setting up I2C/SPI/1-Wire devices
**Impact:** Users cannot leverage hardware integration features

**What's Needed:**
- Step-by-step hardware setup guide
- Wiring diagrams for common devices
- Device driver installation
- Trigger configuration (cron vs periodic)
- Topic namespace mapping
- Filter configuration (ON_CHANGE vs ALWAYS_SEND)
- Troubleshooting hardware issues

#### ❌ Machine Learning Features
**Current State:** MLModelManager.yaml has examples, no guide
**Gap:** No documentation on ML capabilities
**Impact:** Advanced ML features are unknown to users

**What's Needed:**
- ML feature overview
- Model store configuration (Nexus/S3/File)
- Event stream processing setup
- Anomaly detection tutorial
- Model training and retraining
- LLM integration guide
- Example use cases

#### ❌ Complete Getting Started Guide
**Current State:** Basic MQTT example only
**Gap:** No comprehensive getting started for other protocols
**Impact:** Users limited to MQTT, don't know about other features

**What's Needed:**
- Installation guide (JAR, Docker, Debian package)
- First-time setup (authentication, configuration)
- "Hello World" for each major protocol (MQTT, AMQP, STOMP, NATS, CoAP)
- Web UI access guide
- REST API quick start
- Troubleshooting common startup issues

### 3.2 MODERATE GAPS - MEDIUM PRIORITY

#### ⚠️ Security Configuration Guide
**Current State:** SECURITY.md has policy only, AuthManager.yaml has examples
**Gap:** No comprehensive security configuration guide
**Impact:** Users may misconfigure security

**What's Needed:**
- Authentication setup guide (all providers)
- TLS/DTLS certificate configuration
- Client certificate authentication
- Authorization and ACL configuration
- Security domains per protocol
- Password encryption and management
- JAAS configuration file setup

#### ⚠️ Server-to-Server Integration
**Current State:** README mentions it, NetworkConnectionManager.yaml has examples
**Gap:** No complete integration guide
**Impact:** Users cannot set up server bridging

**What's Needed:**
- Server-to-server architecture overview
- Outbound connection configuration
- Namespace mapping strategies
- Bidirectional data flow setup
- Schema propagation
- Auto-discovery configuration
- Load balancing and failover

#### ⚠️ LoRa and LoRaWAN Documentation
**Current State:** LoRaDevice.yaml has examples
**Gap:** No LoRa setup guide
**Impact:** IoT users cannot leverage LoRa features

**What's Needed:**
- LoRa vs LoRaWAN explanation
- Hardware setup (RFM95 wiring)
- LoRaDevice.yaml configuration guide
- Semtech gateway protocol setup
- Frequency and power configuration
- CAD (Channel Activity Detection) tuning
- Troubleshooting LoRa connectivity

#### ⚠️ Storage and Persistence Guide
**Current State:** DestinationManager.yaml has examples
**Gap:** No storage architecture guide
**Impact:** Users don't understand storage options

**What's Needed:**
- Storage types comparison (Partition vs Memory vs MemoryTier)
- Performance characteristics
- S3 archival configuration
- Cache configuration (WeakReference vs JCS)
- Sync modes and durability
- Storage quotas and limits
- Backup and restore procedures

#### ⚠️ Monitoring and Observability
**Current State:** MessageDaemon.yaml mentions JMX, jolokia.yaml exists
**Gap:** No monitoring setup guide
**Impact:** Users cannot monitor production servers

**What's Needed:**
- JMX bean reference
- Jolokia setup and configuration
- Prometheus integration guide
- $SYS topics documentation
- Health check endpoints
- Log management (Logback configuration)
- Performance tuning guide

### 3.3 MINOR GAPS - LOW PRIORITY

#### ⚠️ Namespace and Tenant Management
**Current State:** TenantManagement.yaml has examples
**Gap:** No multi-tenancy guide
**Impact:** Enterprise users may not use isolation features

**What's Needed:**
- Namespace architecture
- Tenant isolation strategies
- Global vs user vs group namespaces
- Token substitution patterns
- Access control per namespace

#### ⚠️ Message Transformation
**Current State:** TransformationManager.yaml has pattern examples
**Gap:** No transformation guide
**Impact:** Users don't know about transformation capabilities

**What's Needed:**
- Transformation overview
- Built-in transformers (JSON↔XML, JSON to Value)
- Pattern matching syntax
- Custom transformer development
- Use cases and examples

#### ⚠️ Protocol-Specific Documentation
**Current State:** PROTOCOLS.md lists protocols, no deep dives
**Gap:** No protocol-specific guides
**Impact:** Users may not understand protocol-specific features

**What's Needed:**
- MQTT 5.0 features (enhanced auth, shared subscriptions)
- AMQP 1.0 link addressing
- STOMP transaction support
- NATS stream configuration
- CoAP block transfer
- MQTT-SN gateway advertisement

#### ⚠️ Architecture Documentation
**Current State:** docs/Architecture.md is empty
**Gap:** No system architecture guide
**Impact:** Developers and architects lack system understanding

**What's Needed:**
- High-level architecture diagram
- Component interaction
- Threading model and async design
- Protocol parsing pipeline
- Session management
- Destination routing
- Extensibility points (ServiceLoader)

---

## 4. Priority Recommendations

### Immediate Actions (Week 1-2)

1. **Create REST API Documentation**
   - Generate from OpenAPI/Swagger annotations
   - Host Swagger UI (already enabled at /api/v1)
   - Document authentication flow
   - Provide curl examples for top 20 endpoints

2. **Complete Getting Started Guide**
   - Installation steps for all platforms
   - Default port reference table
   - First-time authentication setup
   - One example per major protocol

3. **Configuration Quick Reference**
   - Single-page config reference
   - All parameters with defaults
   - Common configuration scenarios
   - Port and endpoint table

### Short-Term Actions (Month 1)

4. **Hardware Integration Tutorial**
   - I2C device setup walkthrough
   - At least 3 common sensor examples
   - Troubleshooting section

5. **Security Configuration Guide**
   - TLS setup step-by-step
   - Authentication provider comparison
   - Production security checklist

6. **Server-to-Server Integration Guide**
   - Complete tutorial with examples
   - Namespace mapping patterns
   - Common integration scenarios

### Medium-Term Actions (Month 2-3)

7. **ML Features Documentation**
   - Model store setup
   - Anomaly detection tutorial
   - Event stream processing

8. **Monitoring and Operations Guide**
   - JMX/Jolokia setup
   - Prometheus integration
   - Production deployment guide

9. **Protocol Deep Dives**
   - MQTT 5.0 features
   - AMQP 1.0 guide
   - CoAP and MQTT-SN guides

10. **Architecture Documentation**
    - System architecture overview
    - Component diagrams
    - Extensibility guide

---

## 5. Suggested Documentation Content

### 5.1 REST API Reference - User Management Example

```markdown
## User Management API

### Get All Users

Retrieve a list of all users with optional filtering.

**Endpoint:** `GET /api/v1/auth/users`

**Authentication:** Required (JWT Bearer token or Basic Auth)

**Query Parameters:**
- `filter` (optional): SQL-like filter expression
  - Example: `username = 'bill'`
  - Example: `username LIKE 'admin%'`

**Response:** `200 OK`
```json
{
  "users": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "username": "admin",
      "groups": ["administrators"],
      "created": "2024-01-15T10:30:00Z"
    },
    {
      "uuid": "660e8400-e29b-41d4-a716-446655440001",
      "username": "user1",
      "groups": ["users"],
      "created": "2024-02-01T14:22:00Z"
    }
  ],
  "total": 2
}
```

**Error Responses:**
- `400 Bad Request`: Invalid filter syntax
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Insufficient permissions

**Example Request:**
```bash
curl -X GET "https://localhost:8080/api/v1/auth/users?filter=username%20%3D%20%27admin%27" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Create New User

Create a new user account.

**Endpoint:** `POST /api/v1/auth/users`

**Authentication:** Required (admin role)

**Request Body:**
```json
{
  "username": "newuser",
  "password": "SecurePassword123!"
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "message": "User created successfully",
  "uuid": "770e8400-e29b-41d4-a716-446655440002"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid username or password
- `401 Unauthorized`: Not authenticated
- `403 Forbidden`: Not authorized (requires admin role)
- `409 Conflict`: Username already exists

**Password Requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- Special characters recommended

**Example Request:**
```bash
curl -X POST "https://localhost:8080/api/v1/auth/users" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"SecurePassword123!"}'
```
```

---

### 5.2 Configuration Reference - NetworkManager Example

```markdown
## NetworkManager Configuration Reference

**File:** `NetworkManager.yaml`
**Location:** `src/main/resources/NetworkManager.yaml`

### Overview

The NetworkManager configuration defines all network endpoints (listeners) and protocol-specific settings. The server can listen on multiple ports with different protocols simultaneously.

### Configuration Structure

```yaml
NetworkManager:
  global:
    # Global settings applied to all endpoints

  data:
    # List of individual endpoint configurations
```

### Global Configuration

#### TCP Layer Settings

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `receiveBufferSize` | Integer | 128000 | TCP receive buffer size in bytes |
| `sendBufferSize` | Integer | 128000 | TCP send buffer size in bytes |
| `timeout` | Integer | 60000 | Socket timeout in milliseconds |
| `backlog` | Integer | 100 | TCP accept backlog queue size |
| `soLingerDelaySec` | Integer | 10 | Socket linger delay in seconds |
| `readDelayOnFragmentation` | Integer | 100 | Delay in ms when packet fragmentation detected |
| `fragmentationLimit` | Integer | 5 | Max fragments before applying delay |
| `enableReadDelayOnFragmentation` | Boolean | true | Enable fragmentation backpressure |

**Example:**
```yaml
global:
  receiveBufferSize: 128000
  sendBufferSize: 128000
  timeout: 60000
  backlog: 100
```

#### Protocol Buffer Settings

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `serverReadBufferSize` | String | "100K" | Protocol read buffer (supports K, M, G suffixes) |
| `serverWriteBufferSize` | String | "100K" | Protocol write buffer |
| `selectorThreadCount` | Integer | 2 | Number of selector threads for async I/O |

**Buffer Size Format:**
- Numeric value: bytes
- With suffix: `100K` = 100 KB, `10M` = 10 MB, `1G` = 1 GB

#### MQTT Protocol Settings

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `maximumSessionExpiry` | Integer | 86400 | Max session expiry in seconds (24 hours) |
| `maximumBufferSize` | String | "10M" | Max message buffer size |
| `serverReceiveMaximum` | Integer | 10 | Server's receive maximum (in-flight messages) |
| `clientReceiveMaximum` | Integer | 65535 | Client's receive maximum limit |
| `clientMaximumTopicAlias` | Integer | 32767 | Max topic aliases for client |
| `serverMaximumTopicAlias` | Integer | 0 | Max topic aliases for server (0 = disabled) |
| `strictClientId` | Boolean | false | Enforce strict client ID validation |

**MQTT 5.0 Specific:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `minServerKeepAlive` | Integer | 0 | Minimum keep-alive value (seconds, 0 = no min) |
| `maxServerKeepAlive` | Integer | 60 | Maximum keep-alive value (seconds) |

#### LoRa Gateway Settings

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `LoRaMaxTransmissionRate` | Integer | 20 | Max packets per second to prevent radio flooding |

#### MQTT-SN Settings

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `maxInFlightEvents` | Integer | 1 | Max in-flight events for MQTT-SN |
| `dropQoS0Events` | Boolean | true | Drop QoS 0 events if queue full |
| `eventQueueTimeout` | Integer | 0 | Event queue timeout in ms (0 = infinite) |
| `advertiseGateway` | Boolean | false | Send gateway advertisement packets |
| `enablePortChanges` | Boolean | true | Allow client port changes (mobile clients) |
| `enableAddressChanges` | Boolean | true | Allow client address changes |
| `packetReuseTimeout` | Integer | 1000 | Packet reuse timeout in ms |

#### Discovery Settings

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `discoverable` | Boolean | true | Register mDNS entry for endpoint |
| `preferIPv6Addresses` | Boolean | true | Prefer IPv6 over IPv4 |
| `scanNetworkChanges` | Boolean | true | Monitor network interface changes |
| `scanInterval` | Integer | 60000 | Network scan interval in ms |

### Endpoint Configuration

Each endpoint in the `data` array defines a listener:

```yaml
data:
  - name: "MQTT Interface"
    url: tcp://:::1883/
    protocol: mqtt
    auth: default
    selectorThreadCount: "{processors}/2"
```

#### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Human-readable endpoint name |
| `url` | String | Endpoint URL (see URL Format below) |
| `protocol` | String | Protocol(s) to enable (comma-separated) |
| `auth` | String | Authentication domain to use |

#### URL Format

```
<transport>://<bind-address>:<port>/
```

**Transport Types:**
- `tcp://` - Plain TCP
- `ssl://` - TLS/SSL over TCP
- `udp://` - Plain UDP
- `dtls://` - DTLS over UDP
- `serial://` - Serial port (format: `serial://<port>,<baud>,<databits>,<parity>,<stopbits>/`)
- `lora://` - LoRa device
- `satellite://` - Satellite modem

**Bind Address:**
- `0.0.0.0` - IPv4 all interfaces
- `:::` - IPv6 all interfaces (also listens on IPv4 if dual-stack)
- Specific IP address

**Examples:**
- `tcp://:::1883/` - MQTT on port 1883, all interfaces
- `ssl://0.0.0.0:8883/` - MQTT over TLS on port 8883
- `udp://0.0.0.0:5683/` - CoAP on port 5683
- `serial://ttyUSB0,9600,8,N,1/` - Serial port at 9600 baud

#### Protocol Values

| Protocol | Description | Typical Ports |
|----------|-------------|---------------|
| `mqtt` | MQTT v3.1.1 and v5.0 | 1883 (TCP), 8883 (SSL) |
| `amqp` | AMQP 1.0 | 5672 (TCP), 5692 (SSL) |
| `stomp` | STOMP 1.1/1.2 | 8674 (TCP), 8695 (SSL) |
| `nats` | NATS v2.0 | 4222 (TCP) |
| `coap` | CoAP (RFC 7252) | 5683 (UDP), 5684 (DTLS) |
| `mqtt-sn` | MQTT-SN v1.2/v2.0 | 1884 (UDP), 1886 (DTLS) |
| `ws` | WebSocket wrapper | Any port with TCP/SSL |
| `wss` | WebSocket Secure | Any port with SSL |
| `semtech` | LoRaWAN Semtech gateway | Custom UDP port |
| `NMEA` | NMEA-0183 GPS | Serial only |
| `satellite` | Satellite modem | HTTP polling |

**Multiple Protocols:**
Comma-separated protocols on same port:
```yaml
protocol: mqtt, ws      # MQTT with WebSocket support
protocol: stomp, wss    # STOMP with WebSocket Secure
```

#### Optional Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `selectorThreadCount` | String/Integer | global | Selector threads for this endpoint |
| `enableStreams` | Boolean | false | Enable NATS streams (NATS only) |
| `maxBlockSize` | Integer | 512 | Max CoAP block size (CoAP only) |
| `idleTimePeriod` | Integer | 120 | CoAP idle timeout in seconds |
| `eventsPerTopicDuringSleep` | Integer | 10 | MQTT-SN buffered events per topic |
| `maxTopicsInSleep` | Integer | 10 | MQTT-SN max topics to buffer |
| `advertiseGateway` | Boolean | global | Override global MQTT-SN advertisement |
| `enablePortChanges` | Boolean | global | Override global port change setting |
| `enableAddressChanges` | Boolean | global | Override global address change setting |

### TLS/SSL Configuration

Global TLS configuration under `global.security.tls`:

```yaml
global:
  security:
    tls:
      clientCertificateRequired: false
      clientCertificateWanted: false

      keyStore:
        type: JKS
        managerFactory: SunX509
        path: my-keystore.jks
        passphrase: password

      trustStore:
        type: JKS
        path: my-truststore.jks
        passphrase: password
        managerFactory: SunX509
```

**TLS Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `clientCertificateRequired` | Boolean | false | Require client certificate authentication |
| `clientCertificateWanted` | Boolean | false | Request (but don't require) client certificate |
| `crlUrl` | String | - | Certificate Revocation List URL |
| `crlInterval` | Integer | - | CRL reload interval in seconds |

**KeyStore Configuration:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `type` | String | JKS | KeyStore type (JKS, PKCS12) |
| `managerFactory` | String | SunX509 | Key manager factory algorithm |
| `path` | String | - | Path to keystore file |
| `passphrase` | String | - | Keystore password |

**TrustStore Configuration:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `type` | String | JKS | TrustStore type |
| `path` | String | - | Path to truststore file |
| `passphrase` | String | - | TrustStore password |
| `managerFactory` | String | SunX509 | Trust manager factory algorithm |

### DTLS Configuration

Similar structure to TLS under `global.security.dtls`:

```yaml
global:
  security:
    dtls:
      clientCertificateRequired: false
      keyStore:
        type: JKS
        managerFactory: SunX509
        path: my-keystore.jks
        passphrase: password
      trustStore:
        type: JKS
        path: my-truststore.jks
        passphrase: password
        managerFactory: SunX509
```

### Token Substitution

Dynamic token replacement in configuration values:

| Token | Replaced With | Example |
|-------|---------------|---------|
| `{{MAPS_DATA}}` | Data directory path | `/var/lib/mapsmessaging` |
| `{{MAPS_HOME}}` | Installation directory | `/opt/mapsmessaging` |
| `{user}` | Username | `john` |
| `{protocol}` | Protocol name | `mqtt` |
| `{processors}` | CPU count | `8` |

**Example:**
```yaml
selectorThreadCount: "{processors}/2"  # Becomes 4 on an 8-core system
```

### Complete Examples

#### Example 1: Basic MQTT Listener

```yaml
- name: "MQTT Interface"
  url: tcp://:::1883/
  protocol: mqtt
  auth: default
  selectorThreadCount: 2
```

#### Example 2: MQTT with TLS and WebSocket

```yaml
- name: "MQTT SSL Interface"
  url: "ssl://0.0.0.0:1892/"
  protocol: mqtt, ws
  auth: default
```

#### Example 3: MQTT-SN with DTLS

```yaml
- name: "DTLS MQTT-SN Interface"
  url: dtls://0.0.0.0:1886/
  protocol: mqtt-sn
  eventsPerTopicDuringSleep: 10
  maxTopicsInSleep: 10
  advertiseGateway: true
  auth: default
```

#### Example 4: CoAP over UDP

```yaml
- name: "CoAP Interface"
  url: udp://0.0.0.0:5683/
  protocol: coap
  maxBlockSize: 512
  idleTimePeriod: 120
  auth: default
```

#### Example 5: NMEA GPS Serial Port

```yaml
- name: NMEA GPS port
  url: serial://ttyUSB0,9600,8,N,1/
  port: ttyUSB0
  baudRate: 9600
  dataBits: 8
  stopBits: 1
  parity: n
  protocol: NMEA
  selectorThreadCount: 1
```

### Default Ports Reference

| Protocol | TCP Port | SSL Port | UDP Port | DTLS Port |
|----------|----------|----------|----------|-----------|
| MQTT | 1883 | 1892/8883 | - | - |
| MQTT-SN | - | - | 1884 | 1886 |
| AMQP | 5672 | 5692 | - | - |
| STOMP | 8674 | 8695 | - | - |
| NATS | 4222 | - | - | - |
| CoAP | - | - | 5683 | 5684 |
| REST API | 8080 | 8443 | - | - |
| Jolokia | 8778 | - | - | - |

### Troubleshooting

**Issue:** Port already in use
```
Error: Address already in use: bind
```
**Solution:** Another process is using the port. Check with:
```bash
netstat -tulpn | grep <port>
```

**Issue:** Permission denied on port < 1024
```
Error: Permission denied
```
**Solution:** On Linux, ports < 1024 require root or capabilities:
```bash
sudo setcap 'cap_net_bind_service=+ep' /path/to/java
```

**Issue:** IPv6 not working
**Solution:** Ensure IPv6 is enabled on the system and use `:::` as bind address.

**Issue:** TLS handshake failures
**Solution:** Verify keystore/truststore paths and passwords. Check certificate validity:
```bash
keytool -list -v -keystore my-keystore.jks
```

### Best Practices

1. **Use TLS in production** - Always encrypt connections for production deployments
2. **Limit selector threads** - Use `{processors}/2` for most deployments
3. **Configure buffer sizes** - Adjust based on message sizes and throughput requirements
4. **Enable monitoring** - Use JMX and health endpoints to monitor connections
5. **Authentication** - Never use anonymous authentication in production
6. **Client certificates** - Use for machine-to-machine authentication
7. **Keep-alive tuning** - Adjust MQTT keep-alive based on client behavior
8. **Port selection** - Use non-standard ports when possible to reduce attack surface
```

---

### 5.3 Getting Started Guide - Hardware Integration

```markdown
# Hardware Integration Guide

## Overview

Maps Messaging Server provides direct integration with hardware devices, automatically publishing sensor data to MQTT topics. This guide walks through setting up I2C, SPI, 1-Wire, and serial devices.

## Prerequisites

- Maps Messaging Server installed and running
- Hardware access (GPIO, I2C, SPI buses enabled)
- Root or appropriate permissions for hardware access

## Supported Hardware Interfaces

1. **I2C** - Two-wire serial protocol (most common sensors)
2. **SPI** - Serial Peripheral Interface (ADCs, high-speed sensors)
3. **1-Wire** - Single-wire protocol (temperature sensors, iButtons)
4. **Serial** - RS232/USB serial devices (GPS, specialized sensors)
5. **LoRa** - RFM95 radio modules

## I2C Device Integration

### Step 1: Enable I2C on Raspberry Pi

```bash
# Enable I2C interface
sudo raspi-config
# Navigate to: Interface Options → I2C → Enable

# Verify I2C is enabled
lsmod | grep i2c
# Should show: i2c_dev, i2c_bcm2835

# Install I2C tools
sudo apt-get install i2c-tools
```

### Step 2: Detect Connected Devices

```bash
# Scan I2C bus 1 (default on Raspberry Pi)
sudo i2cdetect -y 1

# Example output:
#      0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
# 00:          -- -- -- -- -- -- -- -- -- -- -- -- --
# 10: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
# 20: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
# 30: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
# 40: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
# 50: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
# 60: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
# 70: -- -- -- -- -- -- 76 --
# Device found at address 0x76 (likely BME280 sensor)
```

### Step 3: Configure DeviceManager.yaml

Edit `/path/to/config/DeviceManager.yaml`:

```yaml
DeviceManager:
  global:
    enabled: true
    trigger: everyMinute
    scanTime: 30000
    topicNameTemplate: /device/[bus_name]/[device_name]
    filter: ON_CHANGE

  data:
    - name: triggers
      config:
        - type: cron
          name: everyMinute
          cron: 0 0/1 * * * ?

        - type: periodic
          name: everySecond
          interval: 1000

    - name: i2c
      enabled: true
      config:
        - bus: 1
          enabled: true
          autoScan: true
          topicNameTemplate: /device/[device_type]/[bus_name]/[bus_number]/[device_addr]/[device_name]
          trigger: everySecond
```

**Configuration Explained:**
- `enabled: true` - Enables hardware device manager
- `trigger: everyMinute` - Default data collection frequency
- `scanTime: 30000` - Scan for new devices every 30 seconds
- `filter: ON_CHANGE` - Only publish when data changes (vs `ALWAYS_SEND`)
- `autoScan: true` - Automatically detect devices on I2C bus
- `trigger: everySecond` - Override to read sensors every second

### Step 4: Restart Server and Verify

```bash
# Restart server
sudo systemctl restart mapsmessaging

# Subscribe to device topics
mosquitto_sub -h localhost -t '/device/#' -v

# You should see data published:
# /device/i2c/i2c/1/0x76/BME280 {"temperature":22.5,"pressure":1013.25,"humidity":45.2}
```

### Step 5: Understanding Topic Namespace

The `topicNameTemplate` determines where device data is published:

**Template:** `/device/[device_type]/[bus_name]/[bus_number]/[device_addr]/[device_name]`

**Token Replacements:**
- `[device_type]` → `i2c`, `spi`, `onewire`, `serial`
- `[bus_name]` → `i2c`
- `[bus_number]` → `1` (bus number)
- `[device_addr]` → `0x76` (I2C address in hex)
- `[device_name]` → Device identification (e.g., `BME280`, `BMP280`)

**Example Topics:**
```
/device/i2c/i2c/1/0x76/BME280
/device/i2c/i2c/1/0x40/HDC1080
/device/i2c/i2c/1/0x48/ADS1115
```

### Supported I2C Devices (Auto-Detected)

The server automatically identifies and configures these common I2C devices:

**Environmental Sensors:**
- BME280 (temperature, pressure, humidity) - 0x76, 0x77
- BMP280 (temperature, pressure) - 0x76, 0x77
- SHT31 (temperature, humidity) - 0x44, 0x45
- HDC1080 (temperature, humidity) - 0x40

**Air Quality:**
- CCS811 (eCO2, TVOC) - 0x5A, 0x5B
- SGP30 (eCO2, TVOC) - 0x58

**Light/Color:**
- TSL2561 (light) - 0x29, 0x39, 0x49
- TCS34725 (RGB color) - 0x29
- VEML7700 (ambient light) - 0x10

**Motion/Gesture:**
- APDS9960 (gesture, proximity, light, color) - 0x39

**ADC (Analog Input):**
- ADS1115 (16-bit 4-channel ADC) - 0x48, 0x49, 0x4A, 0x4B
- ADS1015 (12-bit 4-channel ADC) - 0x48, 0x49, 0x4A, 0x4B

**Other:**
- INA219 (current/voltage/power) - 0x40, 0x41, 0x44, 0x45
- MCP23017 (16-bit GPIO expander) - 0x20-0x27

### Manual Device Configuration

For devices not auto-detected, add manual configuration:

```yaml
- name: i2c
  enabled: true
  config:
    - bus: 1
      enabled: true
      autoScan: false
      devices:
        - address: 0x76
          type: BME280
          name: "Living Room Sensor"
          trigger: everySecond
```

## SPI Device Integration

### Step 1: Enable SPI

```bash
sudo raspi-config
# Interface Options → SPI → Enable

# Verify SPI devices
ls -l /dev/spidev*
# Should show: /dev/spidev0.0  /dev/spidev0.1
```

### Step 2: Configure MCP3008 ADC

The MCP3008 is an 8-channel 10-bit ADC, commonly used to read analog sensors.

**Wiring (SPI0):**
```
MCP3008 Pin → Raspberry Pi Pin
VDD  (16)   → 3.3V (Pin 1)
VREF (15)   → 3.3V (Pin 1)
AGND (14)   → GND (Pin 9)
CLK  (13)   → SCLK (Pin 23, GPIO 11)
DOUT (12)   → MISO (Pin 21, GPIO 9)
DIN  (11)   → MOSI (Pin 19, GPIO 10)
CS   (10)   → CE0 (Pin 24, GPIO 8)
DGND (9)    → GND (Pin 9)
```

**Configuration:**

```yaml
- name: spi
  enabled: true
  config:
    - name: Mcp3008
      spiBus: 0
      spiMode: 0
      spiChipSelect: 0
      resolution: 10
      channels: 8
      trigger: everySecond
      topicNameTemplate: /device/spi/[device_name]/channel[channel]
```

**Data Published:**
```
/device/spi/Mcp3008/channel0 {"value":512,"voltage":1.65}
/device/spi/Mcp3008/channel1 {"value":1023,"voltage":3.3}
```

## 1-Wire Device Integration

### Step 1: Enable 1-Wire

```bash
# Add to /boot/config.txt
sudo nano /boot/config.txt
# Add line:
dtoverlay=w1-gpio,gpiopin=4

# Reboot
sudo reboot

# Verify 1-Wire is working
ls /sys/bus/w1/devices/
# Example: 28-00000abcdef0 (DS18B20 temperature sensor)
```

### Step 2: Configure 1-Wire Devices

```yaml
- name: oneWire
  enabled: true
  autoScan: true
  trigger: everySecond
  topicNameTemplate: /device/onewire/[device_id]
```

**Auto-Detected Devices:**
- DS18B20 (temperature sensor) - Family code 28
- DS18S20 (temperature sensor) - Family code 10
- iButton devices - Family codes 01, 02, etc.

**Data Published:**
```
/device/onewire/28-00000abcdef0 {"temperature":22.5}
```

## Serial Device Integration

### Step 1: Configure Serial Device

Example: DFRobot SEN0657 Ultrasonic distance sensor

```yaml
- name: serial
  enabled: true
  topicNameTemplate: /serial/[device_name]
  config:
    - name: SEN0657
      serial:
        port: /dev/ttyAMA0
        baudRate: 9600
        dataBits: 8
        stopBits: 1
        parity: n
        flowControl: 0
      trigger: everySecond
```

**Data Published:**
```
/serial/SEN0657 {"distance":145.2,"unit":"cm"}
```

## Trigger Configuration

### Cron-Based Triggers

Use cron expressions for scheduled data collection:

```yaml
triggers:
  - type: cron
    name: hourly
    cron: 0 0 * * * ?

  - type: cron
    name: everyFiveMinutes
    cron: 0 0/5 * * * ?

  - type: cron
    name: dailyAt3AM
    cron: 0 0 3 * * ?
```

**Cron Format:** `<second> <minute> <hour> <day> <month> <day-of-week>`

### Periodic Triggers

Use millisecond intervals for high-frequency collection:

```yaml
triggers:
  - type: periodic
    name: everyHundredMs
    interval: 100

  - type: periodic
    name: every30Seconds
    interval: 30000
```

## Data Filtering

### ON_CHANGE vs ALWAYS_SEND

**ON_CHANGE** (recommended):
- Only publishes when sensor value changes
- Reduces network traffic and storage
- Ideal for most sensors

**ALWAYS_SEND**:
- Publishes on every trigger execution
- Useful for monitoring/heartbeat
- Higher traffic

### JMS Selector Filtering

Apply advanced filtering using JMS selectors:

```yaml
selector: 'temperature > 30 OR humidity < 20'
```

**Selector Examples:**
```sql
temperature > 25
humidity BETWEEN 40 AND 60
CO2 > 1000 AND temperature > 25
pressure < 1000 OR pressure > 1020
```

## Troubleshooting

### Issue: No I2C devices detected

**Check I2C is enabled:**
```bash
ls /dev/i2c-*
# Should show /dev/i2c-1

# If not, enable via raspi-config
```

**Check permissions:**
```bash
# Add user to i2c group
sudo usermod -a -G i2c mapsmessaging
sudo reboot
```

**Check wiring:**
- Verify SDA and SCL connections
- Check device power (3.3V or 5V depending on device)
- Use pullup resistors if needed (typically 4.7kΩ)

### Issue: No data published to topics

**Check server logs:**
```bash
tail -f /var/log/mapsmessaging/server.log
```

**Subscribe to all device topics:**
```bash
mosquitto_sub -h localhost -t '/device/#' -v
```

**Verify trigger is executing:**
```yaml
# Use faster trigger for testing
trigger: everySecond
```

### Issue: Permission denied on /dev/i2c-1

```bash
# Check current permissions
ls -l /dev/i2c-1
# crw-rw---- 1 root i2c 89, 1 Jan 15 10:30 /dev/i2c-1

# Add user to i2c group
sudo usermod -a -G i2c $(whoami)

# Logout and login again
```

### Issue: Device detected but no data

**Check device documentation:**
- Some devices require initialization sequences
- Verify I2C address is correct
- Check device is receiving power

**Enable debug logging:**
```yaml
# In logback.xml
<logger name="io.mapsmessaging.hardware" level="DEBUG"/>
```

## Best Practices

1. **Use ON_CHANGE filtering** - Reduces unnecessary traffic
2. **Choose appropriate triggers** - Balance freshness vs overhead
3. **Monitor device topics** - Subscribe to `/device/#` for debugging
4. **Group similar devices** - Use consistent topic templates
5. **Document device locations** - Use descriptive device names
6. **Test connections** - Use i2cdetect before configuring
7. **Consider power consumption** - Use sleep modes for battery-powered devices
8. **Handle failures gracefully** - Devices may disconnect/reconnect

## Example: Complete BME280 Setup

### Hardware Setup

1. Connect BME280 to Raspberry Pi:
   - VCC → 3.3V (Pin 1)
   - GND → Ground (Pin 9)
   - SCL → GPIO 3 (Pin 5)
   - SDA → GPIO 2 (Pin 3)

2. Enable I2C:
   ```bash
   sudo raspi-config
   # Enable I2C
   ```

3. Detect device:
   ```bash
   sudo i2cdetect -y 1
   # Look for address 0x76 or 0x77
   ```

### Configuration

```yaml
DeviceManager:
  global:
    enabled: true
    trigger: everyMinute
    filter: ON_CHANGE

  data:
    - name: triggers
      config:
        - type: periodic
          name: everySecond
          interval: 1000

    - name: i2c
      enabled: true
      config:
        - bus: 1
          enabled: true
          autoScan: true
          trigger: everySecond
```

### Testing

```bash
# Subscribe to topics
mosquitto_sub -h localhost -t '/device/i2c/#' -v

# Expected output:
# /device/i2c/i2c/1/0x76/BME280 {"temperature":22.5,"pressure":1013.25,"humidity":45.2}
```

### Integration with Node-RED

```json
[
  {
    "id": "mqtt-in",
    "type": "mqtt in",
    "topic": "/device/i2c/+/+/BME280",
    "broker": "localhost",
    "qos": "1"
  },
  {
    "id": "parse-json",
    "type": "json"
  },
  {
    "id": "dashboard",
    "type": "ui_chart",
    "name": "Temperature",
    "group": "sensors",
    "order": 1,
    "width": 6,
    "height": 4
  }
]
```

## Next Steps

- Learn about [Server-to-Server Integration](#) to forward device data to cloud brokers
- Explore [Schema Management](#) to validate device payloads
- Set up [Alerting](#) based on sensor thresholds using JMS selectors
- Configure [Storage and Archival](#) for long-term sensor data retention
```

---

### 5.4 Quick Start - Installation and First Run

```markdown
# Quick Start Guide

## Installation

### Method 1: JAR File (All Platforms)

**Prerequisites:**
- Java 17 or later
- 2GB RAM minimum

**Download:**
```bash
wget https://repository.mapsmessaging.io/releases/io/mapsmessaging/mapsmessaging_server/4.1.1/mapsmessaging_server-4.1.1.jar

# Or from GitHub releases
# https://github.com/Maps-Messaging/mapsmessaging_server/releases
```

**Run:**
```bash
java -jar mapsmessaging_server-4.1.1.jar
```

### Method 2: Docker

```bash
docker pull mapsmessaging/mapsmessaging_server:4.1.1

docker run -d \
  --name mapsmessaging \
  -p 1883:1883 \
  -p 8080:8080 \
  -v /path/to/data:/data \
  mapsmessaging/mapsmessaging_server:4.1.1
```

### Method 3: Debian/Ubuntu Package

```bash
wget https://repository.mapsmessaging.io/releases/mapsmessaging-server_4.1.1_all.deb
sudo dpkg -i mapsmessaging-server_4.1.1_all.deb
sudo systemctl start mapsmessaging
sudo systemctl enable mapsmessaging
```

## First Run

### Default Credentials

On first startup, the server generates random passwords for default users:

**Location:** `$MAPS_DATA/.security/users.properties`

```bash
# View generated credentials
cat $MAPS_DATA/.security/users.properties
# admin:encrypted_password_here
# user:encrypted_password_here
```

**Check server logs for plaintext passwords:**
```bash
tail -n 100 /var/log/mapsmessaging/server.log | grep "Initial password"
```

Output:
```
Initial password for 'admin': Xy7#mK9$pL2@
Initial password for 'user': Qw5!nB8&tR3%
```

### Verify Server is Running

**Check default ports:**
```bash
# MQTT
netstat -tulpn | grep 1883

# REST API
curl http://localhost:8080/health
# Expected: "Ok"
```

### Test MQTT Connection

**Using mosquitto_pub/sub:**
```bash
# Subscribe
mosquitto_sub -h localhost -p 1883 -t test/topic -u admin -P 'Xy7#mK9$pL2@'

# Publish (in another terminal)
mosquitto_pub -h localhost -p 1883 -t test/topic -m "Hello World" -u admin -P 'Xy7#mK9$pL2@'
```

**Using Python (paho-mqtt):**
```python
import paho.mqtt.client as mqtt

def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")
    client.subscribe("test/topic")

def on_message(client, userdata, msg):
    print(f"{msg.topic}: {msg.payload.decode()}")

client = mqtt.Client()
client.username_pw_set("admin", "Xy7#mK9$pL2@")
client.on_connect = on_connect
client.on_message = on_message

client.connect("localhost", 1883, 60)
client.loop_forever()
```

### Test REST API

**Get server info:**
```bash
curl -u admin:'Xy7#mK9$pL2@' http://localhost:8080/api/v1/server/details/info | jq
```

**Response:**
```json
{
  "version": "4.1.1",
  "buildDate": "2024-01-15",
  "vendor": "Maps Messaging B.V.",
  "uptime": 3600000,
  "startTime": "2024-02-04T10:00:00Z"
}
```

## Configuration

### Environment Variables

```bash
export MAPS_DATA=/var/lib/mapsmessaging  # Data directory
export MAPS_HOME=/opt/mapsmessaging      # Installation directory
```

### Configuration Files Location

**JAR Mode:**
- Default: `./config/`
- Override: `-Dconfig.path=/path/to/config`

**Debian Package:**
- `/etc/mapsmessaging/`

**Docker:**
- Mount: `-v /path/to/config:/config`

### Minimal Configuration Changes

**Set server name:**

Edit `MessageDaemon.yaml`:
```yaml
MessageDaemon:
  serverName: "MyMQTTServer"
```

**Change MQTT port:**

Edit `NetworkManager.yaml`:
```yaml
data:
  - name: "MQTT Interface"
    url: tcp://:::8883/  # Changed from 1883
    protocol: mqtt
    auth: default
```

## Access Web UI

**URL:** http://localhost:8080/

**Login:**
- Username: `admin`
- Password: (from first-run logs)

**Features:**
- Dashboard with connection stats
- Destination (topic/queue) browser
- Connection manager
- Configuration editor

## Default Ports Reference

| Service | Port | Protocol | TLS Port |
|---------|------|----------|----------|
| MQTT | 1883 | TCP | 1892 |
| AMQP | 5672 | TCP | 5692 |
| STOMP | 8674 | TCP | 8695 |
| NATS | 4222 | TCP | - |
| CoAP | 5683 | UDP | 5684 (DTLS) |
| MQTT-SN | 1884 | UDP | 1886 (DTLS) |
| REST API | 8080 | HTTP | 8443 (HTTPS) |
| Jolokia | 8778 | HTTP | - |

## Next Steps

1. **Change Default Passwords:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/users/admin/password \
     -u admin:'Xy7#mK9$pL2@' \
     -H "Content-Type: application/json" \
     -d '{"password":"NewSecurePassword123!"}'
   ```

2. **Enable TLS:**
   - See [Security Configuration Guide](#security-configuration-guide)

3. **Configure Protocols:**
   - Enable/disable protocols in `NetworkManager.yaml`

4. **Set Up Monitoring:**
   - Configure JMX or Prometheus
   - See [Monitoring Guide](#monitoring-guide)

5. **Explore Features:**
   - [Hardware Integration](#hardware-integration-guide)
   - [Server-to-Server Bridging](#server-integration-guide)
   - [Schema Management](#schema-guide)

## Troubleshooting

### Server won't start

**Check Java version:**
```bash
java -version
# Requires Java 17+
```

**Check port conflicts:**
```bash
sudo netstat -tulpn | grep 1883
# Kill conflicting process or change port
```

**View logs:**
```bash
tail -f /var/log/mapsmessaging/server.log
```

### Can't connect via MQTT

**Check authentication:**
```bash
mosquitto_pub -h localhost -p 1883 -t test -m "test" -u admin -P 'wrong_password' -d
# Look for: Connection error: Connection Refused: bad user name or password
```

**Check firewall:**
```bash
sudo ufw allow 1883/tcp
```

### REST API returns 403

**Enable authentication:**

Edit `RestApi.yaml`:
```yaml
RestApi:
  enabled: true
  enableAuthentication: false  # Set to true for production
```

### High memory usage

**Adjust JVM heap:**
```bash
java -Xmx2G -Xms1G -jar mapsmessaging_server-4.1.1.jar
```

**Configure storage:**

Edit `DestinationManager.yaml`:
```yaml
type: Memory  # Change to Partition for file-based storage
```

## Production Checklist

- [ ] Change default passwords
- [ ] Enable TLS/SSL on all endpoints
- [ ] Configure authentication provider (LDAP/Auth0)
- [ ] Set up log rotation
- [ ] Configure backups for `$MAPS_DATA`
- [ ] Enable JMX monitoring
- [ ] Configure firewall rules
- [ ] Set up systemd service
- [ ] Test failover/restart
- [ ] Document configuration
- [ ] Set up alerting

## Support

- Documentation: https://docs.mapsmessaging.io
- Issues: https://github.com/Maps-Messaging/mapsmessaging_server/issues
- Community: https://discord.gg/mapsmessaging (example)
```

---

## Conclusion

This gap analysis reveals significant documentation needs despite the server's extensive capabilities. The immediate priority should be creating REST API documentation and a comprehensive getting started guide, followed by configuration references and feature-specific tutorials.

The suggested content above provides templates that can be expanded based on the detailed source code analysis. Each section should be validated against the actual implementation and updated as the codebase evolves.

**Estimated Documentation Effort:**
- Immediate priorities: 2-3 weeks
- Short-term actions: 4-6 weeks
- Medium-term actions: 8-12 weeks
- Total comprehensive documentation: 3-4 months

**Recommended Approach:**
1. Start with auto-generated REST API docs (OpenAPI/Swagger)
2. Create configuration reference from YAML comments
3. Develop end-to-end tutorials for key features
4. Build architecture documentation for developers
5. Continuous updates as features are added
