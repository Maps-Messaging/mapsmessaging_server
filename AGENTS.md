# AGENTS.md — Coding Agent Guidelines for mapsmessaging_server

## Project Overview

MAPS (Multi Adapter and Protocol Standards) Messaging Server — a Java 21 IoT messaging
server supporting 20+ wire protocols (MQTT 3/5, MQTT-SN, AMQP, STOMP, CoAP, NATS, LoRa,
WebSocket, etc.). Plugin architecture via Java ServiceLoader (SPI). Main entry point:
`io.mapsmessaging.MessageDaemon`.

## Build System

Maven project. Java 21 source/target. UTF-8 encoding.

```bash
# Full build (skip tests)
mvn clean package -DskipTests

# Full build with tests
mvn clean verify

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=RetainManagerTest

# Run a single test method
mvn test -Dtest=RetainManagerTest#current_onNewInstance_returnsMinusOne

# Run tests matching a pattern
mvn test -Dtest="io.mapsmessaging.engine.**"

# Build and deploy (used by CI)
mvn -DskipTests=true clean deploy -U

# OWASP dependency vulnerability check
mvn dependency-check:check

# Config lint (runs during verify phase)
mvn verify
```

Test patterns recognized by Surefire: `**/*Test.java`, `**/*Tests.java`, `**/*IT.java`.

Surefire argLine sets SSL keystores and `MAPS_HOME=${project.build.directory}` automatically.

## Testing

- **Framework:** JUnit Jupiter (JUnit 5), Mockito 5, REST Assured 5
- **Base classes:**
  - `BaseTest` — logging/timing hooks for all tests
  - `BaseTestConfig extends BaseTest` — starts full `MessageDaemon`; has `@Timeout(240000)`
  - `ApiTestBase extends BaseTestConfig` — REST API tests with OpenAPI validation
- **Test naming:** snake_case descriptive names preferred (e.g.,
  `current_onNewInstance_returnsMinusOne`), camelCase also used
- **Assertions:** Use `org.junit.jupiter.api.Assertions` (assertEquals, assertThrows, etc.)
- **Test class visibility:** Package-private (no `public` modifier) is the norm
- **Cleanup:** `try-finally` for resource cleanup in tests; `BaseTestConfig` handles
  session/destination cleanup in `@AfterEach`

## Code Style

### Formatting
- **2-space indentation** (spaces, not tabs)
- **K&R brace style** — opening brace on same line
- **No formal formatter config** (no Checkstyle/Prettier/EditorConfig)
- SonarCloud used for code quality analysis

### Imports
- Order: project (`io.mapsmessaging.*`) → third-party → Lombok → Java stdlib
- **Wildcard imports** are used for `java.util.*`, `java.io.*`, `java.nio.*`
- Explicit imports for third-party and project classes

### Naming Conventions
- **Packages:** lowercase dot-separated (`io.mapsmessaging.engine.destination`)
- **Classes:** PascalCase (`RetainManager`, `MessageDaemon`)
- **Methods/fields:** camelCase (`getInstance()`, `currentId`)
- **Constants:** UPPER_SNAKE_CASE (`HEADER_SIZE`)
- **Test methods:** snake_case descriptive (`replace_withValidIds_setsAndOverwritesRetainId`)

### Type Annotations and Null Safety
- **Lombok:** Used extensively — `@Getter`, `@Setter`, `@NoArgsConstructor`, `@NonNull`
- **JetBrains annotations:** `@NotNull`, `@Nullable` alongside Lombok `@NonNull`
- **SonarQube suppressions:** `@SuppressWarnings("java:S106")` with Sonar rule IDs

### Error Handling
- `try-finally` for resource cleanup (not try-with-resources in all cases)
- `Thread.currentThread().interrupt()` when catching `InterruptedException`
- Custom enum-based logging: `logger.log(ServerLogMessages.MESSAGE_DAEMON_STARTUP)`
- No string concatenation in log calls

### Logging
- Custom framework: `io.mapsmessaging.logging.Logger` / `LoggerFactory`
- Log messages are enum constants (`ServerLogMessages`, `LogMessages`)
- Logback as the underlying implementation

### Patterns
- **Singletons:** Static `getInstance()` (e.g., `MessageDaemonConfig.getInstance()`)
- **ServiceLoader (SPI):** Plugin discovery for protocols, transformations, analyzers
  (see `META-INF/services/`)
- **CompletableFuture:** Heavily used for async operations in the API layer
- **Editor fold markers:** `//<editor-fold desc="...">` to organize code sections
- **Configuration:** YAML-based, loaded via `ConfigurationManager`, mapped to DTO/Config classes

### License Header
Every Java file must start with:
```java
/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  ...
 */
```

## Project Structure

```
src/main/java/io/mapsmessaging/
  MessageDaemon.java        # Main entry point
  api/                      # Public API (Session, Destination, Message, Schema)
  config/                   # Configuration DTOs and managers
  engine/                   # Core messaging engine (destinations, sessions, subscriptions)
  network/protocol/impl/    # Protocol implementations (mqtt/, amqp/, stomp/, coap/, etc.)
  rest/                     # REST API (Jersey/Grizzly)
  auth/                     # Authentication/authorization
  dto/                      # Data Transfer Objects
  logging/                  # Custom enum-based logging

src/test/java/io/mapsmessaging/
  BaseTest.java             # Base test class
  test/BaseTestConfig.java  # Server-bootstrapping base (starts MessageDaemon)

src/main/resources/         # YAML configs (MessageDaemon.yaml, NetworkManager.yaml, etc.)
```

## Commit Conventions

Conventional Commits enforced. Format: `type(scope): subject`

- **Types:** feat, fix, refactor, perf, test, build, ci, docs, style, chore, revert
- **Scopes:** server, ml, engine, config, schema, protocol, network, rest, buildkite, docs
- **Header:** ≤100 chars, imperative mood, lowercase type/scope, no trailing period
- **Footer:** `JIRA: MAPS-###` or `NO-ISSUE`; optional `BREAKING CHANGE:`, `DEPRECATED:`,
  `SECURITY:`
- **Branch naming:** `MAPS-###-short-desc`

Example:
```
feat(protocol): add MQTT 5 bridge support

Implements protocol translation between MQTT 3.1.1 and 5.0.

JIRA: MAPS-123
```

## CI/CD

- **Buildkite** is the primary CI (`.buildkite/server_pipeline.yml`)
- **GitHub Actions** for Dependabot auto-merge only
- SonarCloud integration for code quality (org: `maps-messaging`)
- JaCoCo for code coverage
- OWASP dependency-check for vulnerability scanning (fails on CVSS ≥ 10)

## Key Dependencies

Lombok, Logback, SnakeYAML, Jackson, Jersey 4 + Grizzly, Swagger/OpenAPI,
Apache Qpid Proton-J (AMQP), Eclipse Paho (MQTT), Californium (CoAP), Auth0 JWT,
AWS SDK (Cognito), Pi4J (hardware), MapDB, Quartz Scheduler, JMH (benchmarks).

## Important Notes

- The `ml` Maven profile adds `src/main/java-ml` and `src/test/java-ml` source dirs
- The `native` Maven profile builds a GraalVM native image
- SSL keystores (`my-keystore.jks`, `my-truststore.jks`) are in the project root and
  required for tests — do not delete them
- Test timeout is 4 minutes (`@Timeout(240000)`) on integration tests via `BaseTestConfig`
- Many tests start the full `MessageDaemon` — they are slow; prefer unit tests when possible
