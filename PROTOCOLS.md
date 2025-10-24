# Supported Protocols and Management Interfaces

This document summarizes the client-facing and management/admin protocols supported by the Maps Messaging Server, with code and configuration pointers. All ports are configurable; values below reflect the defaults in the provided config files.


## Discovery and bootstrapping
- Protocols are discovered via ServiceLoader:
  - Protocol implementations: src/main/java/io/mapsmessaging/network/protocol/impl/**/* (registered via ProtocolImplFactory)
  - Endpoint/transport servers: src/main/java/io/mapsmessaging/network/io/impl/**/* (registered via EndPointServerFactory)
- Feature flags gate loading (see SubSystemManager and NetworkManager):
  - Protocols: feature "protocols.<name>" (e.g., protocols.mqtt, protocols.nats)
  - Network transports: feature "network.<name>" (e.g., network.tcp, network.udp)
- YAML configuration drives listeners and defaults:
  - Network endpoints: src/main/resources/NetworkManager.yaml (and NetworkManagerDocker.yaml)
  - REST API: src/main/resources/RestApi.yaml
  - JMX, system topics, etc.: src/main/resources/MessageDaemon.yaml
  - Auth: src/main/resources/AuthManager.yaml


## Client-facing protocols

### MQTT v3.1.1
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/mqtt/*
  - Factory: MQTTProtocolFactory.java (declares "MQTT version 3.1.1")
- Default endpoints (NetworkManager.yaml):
  - tcp://:::1883/ (protocol: mqtt)
  - ssl://0.0.0.0:1892/ (protocols: mqtt, ws) — MQTT over WebSocket supported here
- Authentication: username/password (Connect), enforced via AuthManager (JAAS). TLS optional.

### MQTT v5.0
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/mqtt5/*
  - Factory: MQTT5ProtocolFactory.java (declares "MQTT version 5.0")
- Endpoints: same listener types as MQTT 3.1.1 when configured for mqtt in YAML.
- Authentication: username/password and optional enhanced auth; TLS optional.

### AMQP 1.0
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/amqp/*
  - Uses Apache Qpid Proton-J (pom.xml: org.apache.qpid:proton-j)
  - Factory: AMQPProtocolFactory.java
- Default endpoints (NetworkManager.yaml):
  - tcp://0.0.0.0:5672/ (protocol: amqp)
  - ssl://0.0.0.0:5692/ (protocol: amqp)
- Authentication: SASL (PLAIN/ANONYMOUS etc.) via Proton; per-endpoint SASL config supported (EndPointServerConfigDTO.saslConfig). TLS optional.
- WebSocket subprotocol: "amqp" when ws/wss is enabled (see WebSocket section below).

### STOMP 1.1/1.2
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/stomp/*
  - Supported versions: 1.1 and 1.2 (BaseConnectListener.java)
  - Factory: StompProtocolFactory.java
- Default endpoints (NetworkManager.yaml):
  - tcp://0.0.0.0:8674/ (protocols: stomp, ws)
  - ssl://0.0.0.0:8695/ (protocols: stomp, wss)
- Authentication: login/passcode headers (anonymous if not supplied), enforced via AuthManager. TLS optional.
- WebSocket subprotocols: v10.stomp, v11.stomp, v12.stomp.

### NATS (v2.0)
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/nats/*
  - Factory: NatsProtocolFactory.java
  - getVersion() returns "2.0"
- Default endpoint (NetworkManager.yaml):
  - tcp://0.0.0.0:4222/ (protocol: nats)
- Authentication: optional user/pass in CONNECT; enforced via AuthManager. TLS optional.

### CoAP (RFC 7252/7641/7959)
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/coap/*
  - Factory: CoapProtocolFactory.java (notes RFCs)
- Default endpoints (NetworkManager.yaml):
  - udp://0.0.0.0:5683/ (protocol: coap)
  - dtls://0.0.0.0:5684/ (protocol: coap)
- Transport/auth: UDP and DTLS supported; DTLS configurable in YAML.

### MQTT-SN v1.2 and v2.0
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/mqtt_sn/* (v1_2, v2_0)
  - Factory: MQTT_SNProtocolFactory.java
  - Interface manager: MQTTSNInterfaceManager.java (selects v1.2 or v2.0 based on CONNECT)
- Default endpoints (NetworkManager.yaml):
  - udp://0.0.0.0:1884/ (protocol: mqtt-sn)
  - dtls://0.0.0.0:1886/ (protocol: mqtt-sn)
- Transport/auth: UDP/DTLS; username/password optional; DTLS configurable.

### WebSocket (RFC 6455, encapsulation)
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/websockets/*
  - Factory: WebSocketProtocolFactory.java; handshake in Connecting.java
  - Subprotocols supported (when allowed by endpoint protocols): mqtt, amqp, v10.stomp, v11.stomp, v12.stomp
- Endpoints: enabled when endpoint "protocols" includes ws (or wss). Examples in NetworkManager.yaml:
  - ssl://0.0.0.0:1892/ (protocols: mqtt, ws)
  - tcp://0.0.0.0:8674/ (protocols: stomp, ws)

### LoRaWAN Semtech UDP gateway
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/semtech/*
  - Factory: SemTechProtocolFactory.java
  - Config DTO: dto/rest/config/protocol/impl/SemtechConfigDTO.java (maps inbound/outbound/status topics)
- Endpoint: UDP port is user-configurable (commonly 1700). Not enabled by default in NetworkManager.yaml; add a UDP endpoint with protocol: semtech.

### NMEA-0183 (serial)
- Implementation: src/main/java/io/mapsmessaging/network/protocol/impl/nmea/*
- Example (commented) in NetworkManager.yaml shows serial usage:
  - serial://ttyUSB0,9600,8,N,1/ with protocol: NMEA-0183


## Transports / listeners
- TCP: src/main/java/io/mapsmessaging/network/io/impl/tcp/* (TCPEndPointServerFactory)
- SSL/TLS: src/main/java/io/mapsmessaging/network/io/impl/ssl/*
- UDP: src/main/java/io/mapsmessaging/network/io/impl/udp/*
- DTLS: src/main/java/io/mapsmessaging/network/io/impl/dtls/*
- Serial: src/main/java/io/mapsmessaging/network/io/impl/serial/*
- LoRa: src/main/java/io/mapsmessaging/network/io/impl/lora/*
- Proxy Protocol: EndPointConfigDTO supports proxyProtocolMode and allowedProxyHosts


## Default endpoints/ports (from NetworkManager.yaml)
- MQTT: tcp://:::1883/ (mqtt)
- MQTT + WebSocket (TLS): ssl://0.0.0.0:1892/ (mqtt, ws)
- NATS: tcp://0.0.0.0:4222/ (nats)
- STOMP: tcp://0.0.0.0:8674/ (stomp, ws)
- STOMP TLS: ssl://0.0.0.0:8695/ (stomp, wss)
- AMQP: tcp://0.0.0.0:5672/ (amqp)
- AMQP TLS: ssl://0.0.0.0:5692/ (amqp)
- MQTT-SN: udp://0.0.0.0:1884/ (mqtt-sn)
- MQTT-SN DTLS: dtls://0.0.0.0:1886/ (mqtt-sn)
- CoAP: udp://0.0.0.0:5683/ (coap)
- CoAP DTLS: dtls://0.0.0.0:5684/ (coap)
- Docker example (NetworkManagerDocker.yaml): tcp://0.0.0.0:9000/ (protocol: all → amqp, mqtt, stomp, nats, ws)


## Authentication
- Central manager: src/main/java/io/mapsmessaging/auth/AuthManager.java; config in src/main/resources/AuthManager.yaml
  - JAAS-based; requires -Djava.security.auth.login.config=<path> at runtime when authenticationEnabled=true
  - On first boot, creates initial users (admin/user) with random passwords under MAPS_DATA/.security
- Protocol-level:
  - MQTT 3/5: username/password in CONNECT; v5 supports enhanced auth (AuthenticationContext)
  - STOMP: login/passcode headers; if absent, attempts anonymous
  - NATS: user/pass in CONNECT
  - AMQP: SASL via Proton; per-endpoint SASL (EndPointServerConfigDTO.saslConfig)
- TLS/DTLS available for endpoints (NetworkManager.yaml → global.security.tls/dtls), with optional client-certificate requirements
- Proxy Protocol support at endpoint level (EndPointConfigDTO: proxyProtocolMode, allowedProxyHosts)


## Management and administration
- REST API (Jersey/Grizzly)
  - Base path: /api/v1 (src/main/java/io/mapsmessaging/rest/api/Constants.java)
  - Config: src/main/resources/RestApi.yaml; code: src/main/java/io/mapsmessaging/rest/RestApiServerManager.java
  - Default: http://0.0.0.0:8080 (TLS optional via RestApi.yaml)
  - Swagger/OpenAPI enabled by default; endpoints registered in MapsRestServerApi.java and other resources under io.mapsmessaging.rest.api.impl.*
  - Health endpoints:
    - /health (plain text): src/main/java/io/mapsmessaging/rest/api/impl/ConsulHealth.java
    - /api/v1/server/health and /api/v1/server/status (JSON): ServerDetailsApi.java
  - Other: /api/v1/ping, plus auth/destination/interface/schema/device/logging/ML endpoints when enabled
- JMX
  - Enabled by default (MessageDaemon.yaml: EnableJMX: true). Beans under io.mapsmessaging.admin, network.*.jmx, etc.
- Jolokia (JMX over HTTP)
  - Optional bridge; default disabled (src/main/resources/jolokia.yaml, default port 8778)
- Metrics
  - Exposed via JMX; a Prometheus JMX exporter mapping is provided at src/main/resources/prometheus_config.yml
- Discovery/Registration
  - Consul integration registers endpoint metadata (MessageDaemon.buildMetaData; ConsulManagerFactory)


## Key code entry points
- Bootstrap: src/main/java/io/mapsmessaging/MessageDaemon.java
- Subsystems: src/main/java/io/mapsmessaging/SubSystemManager.java
- Network manager: src/main/java/io/mapsmessaging/network/NetworkManager.java
- Endpoint URL parsing: src/main/java/io/mapsmessaging/network/EndPointURL.java
- EndPoint config packing/unpacking: src/main/java/io/mapsmessaging/config/network/EndPointConfigFactory.java


## Configuration files
- Network endpoints and protocol defaults: src/main/resources/NetworkManager.yaml
- Docker example: src/main/resources/NetworkManagerDocker.yaml
- REST API: src/main/resources/RestApi.yaml
- Message daemon (JMX, system topics, compression, etc.): src/main/resources/MessageDaemon.yaml
- Authentication: src/main/resources/AuthManager.yaml
- Jolokia: src/main/resources/jolokia.yaml
- Prometheus JMX exporter mapping: src/main/resources/prometheus_config.yml
