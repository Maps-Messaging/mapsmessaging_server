# AGENTS.md — Coding Agent Guidelines for mapsmessaging_server

## Project Overview

MAPS Messaging Server is a Java 21 IoT messaging server supporting many wire protocols, including MQTT 3.x/5, MQTT-SN, AMQP, STOMP, CoAP, NATS, LoRa, WebSocket, and related transports.

The server uses a plugin architecture based on Java `ServiceLoader` / SPI discovery. The main entry point is:

```java
io.mapsmessaging.MessageDaemon
```

This is a protocol-heavy, non-blocking messaging server. Correctness, compatibility, resource cleanup, and regression safety matter more than cosmetic refactoring.

## Agent Operating Rules

### Default behaviour

* Inspect the current git diff before making changes.
* Make the smallest safe change that satisfies the request.
* Do not reformat unrelated files.
* Do not rename public APIs unless explicitly requested.
* Do not change license headers.
* Do not rewrite working code for style reasons.
* Do not introduce new dependencies unless explicitly requested.
* Do not change production code when the task is to add tests.
* If production code appears wrong while writing tests, report it separately.

### When creating tests

* Add or update files only under `src/test/java` unless explicitly told otherwise.
* Prefer focused unit tests over full daemon integration tests.
* Use full daemon tests only when the behaviour cannot be tested locally.
* Prefer real objects over mocks.
* Use Mockito only for IO, timing, scheduler, network, storage, or external boundaries.
* Test behaviour, not private implementation details.
* Add regression tests for boundary values, malformed input, null handling, and protocol edge cases.
* Use realistic protocol payloads where possible.
* Do not invent protocol semantics. If behaviour is unclear, inspect nearby code and tests.

### When fixing failures

* First determine whether the failure is in the test or production code.
* Fix test code if the test is wrong.
* Do not change production code unless the user requested a fix or the production bug is clearly proven.
* Keep any production-code fix minimal and explain it.

## Build System

This is a Maven project using Java 21 source/target and UTF-8 encoding.

Common commands:

```bash
# Full build without tests
mvn clean package -DskipTests

# Full build with tests and verification
mvn clean verify

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=RetainManagerTest

# Run a single test method
mvn test -Dtest=RetainManagerTest#current_onNewInstance_returnsMinusOne

# Run tests matching a pattern
mvn test -Dtest="io.mapsmessaging.engine.**"

# Build and deploy, used by CI
mvn -DskipTests=true clean deploy -U

# OWASP dependency vulnerability check
mvn dependency-check:check
```

Surefire recognises these test patterns:

```text
**/*Test.java
**/*Tests.java
**/*IT.java
```

Surefire `argLine` sets SSL keystores and `MAPS_HOME=${project.build.directory}` automatically.

SSL keystores in the project root are required for tests:

```text
my-keystore.jks
my-truststore.jks
```

Do not delete or move them.

## Maven Scope Rules

Use the smallest useful Maven command first.

When working in a module, prefer:

```bash
mvn -pl <module> test
```

Only run the full build when:

* The change crosses module boundaries.
* The module-specific test command passes.
* The user explicitly requests a full build.
* The failure may involve integration, packaging, profiles, or generated resources.

For test generation, run the affected test class first, then the affected module tests.

## Testing Stack

* JUnit Jupiter / JUnit 5
* Mockito 5
* REST Assured 5
* JaCoCo coverage
* Surefire

Base classes:

```text
BaseTest
```

Logging and timing hooks for tests.

```text
BaseTestConfig extends BaseTest
```

Starts the full `MessageDaemon`. This is slow and has `@Timeout(240000)`.

```text
ApiTestBase extends BaseTestConfig
```

REST API tests with OpenAPI validation.

## Test Style

* Test classes are usually package-private.
* Use JUnit assertions from `org.junit.jupiter.api.Assertions`.
* Prefer descriptive snake_case test names.

Example:

```java
current_onNewInstance_returnsMinusOne
```

Camel case exists in older tests, but snake_case is preferred for new tests.

Use `try-finally` for cleanup where resources must be closed or state must be restored.

Do not use integration-style daemon startup tests when a focused unit test can prove the behaviour.

## High-Value Test Areas

Prioritise regression tests around:

* Protocol parsing and encoding.
* MQTT session persistence and clean-session behaviour.
* MQTT 5 session expiry and subscription handling.
* MQTT-SN topic id and alias behaviour.
* NMEA/AIS/CAN/MAVLink packet parsing.
* State transitions and reconnect scheduling.
* Duplicate scheduling prevention.
* DTO defaults and update/copy behaviour.
* YAML configuration mapping.
* Authentication and authorization boundary cases.
* Retain manager and subscription depth behaviour.
* Timeout, shutdown, and resource cleanup paths.

## Code Style

### Formatting

* 2-space indentation.
* Spaces, not tabs.
* K&R brace style.
* No formal formatter config is currently enforced.
* SonarCloud is used for code quality.

### Imports

Preferred order:

```text
io.mapsmessaging.*
third-party libraries
lombok
java.*
javax.* / jakarta.*
```

Wildcard imports are accepted for common Java packages such as:

```java
java.util.*
java.io.*
java.nio.*
```

Use explicit imports for project and third-party classes unless nearby code clearly uses another style.

### Naming

Packages:

```text
lowercase dot-separated
```

Classes:

```text
PascalCase
```

Methods and fields:

```text
camelCase
```

Constants:

```text
UPPER_SNAKE_CASE
```

Test methods:

```text
descriptive_snake_case
```

Use descriptive Java variable names. Do not use C/C++ style short names such as `i`, `j`, `x`, `ptr`, or `buf` unless the variable is a conventional loop index in a tiny local scope.

Declare variables separately.

Prefer:

```java
int currentOffset;
int payloadLength;
```

Do not use:

```java
int currentOffset, payloadLength;
```

## Lombok and Null Safety

Lombok is used extensively. Use existing project style when adding or modifying classes.

Common annotations:

```java
@Getter
@Setter
@NoArgsConstructor
@NonNull
```

JetBrains annotations are also used:

```java
@NotNull
@Nullable
```

When existing code uses Lombok or JetBrains annotations, preserve that style.

Do not add broad null-handling changes unless requested or required by a failing test.

## Error Handling

* Use `try-finally` for explicit cleanup where this matches existing code.
* Use try-with-resources only where it fits the surrounding style.
* Always call `Thread.currentThread().interrupt()` when catching `InterruptedException`.
* Do not swallow exceptions silently.
* Keep exception handling narrow and purposeful.

## Logging

The project uses a custom logging framework:

```java
io.mapsmessaging.logging.Logger
io.mapsmessaging.logging.LoggerFactory
```

Log messages should use enum constants such as:

```java
ServerLogMessages.MESSAGE_DAEMON_STARTUP
LogMessages.SOME_EVENT
```

Do not use string concatenation in log calls.

Prefer structured arguments where the logging API supports them.

## Project Patterns

Common patterns in this codebase:

* Static singleton access via `getInstance()`.
* Plugin discovery through `ServiceLoader` and `META-INF/services/`.
* YAML configuration loaded by `ConfigurationManager`.
* DTO/config mapping classes.
* `CompletableFuture` for async API operations.
* Editor fold markers:

```java
//<editor-fold desc="...">
```

Preserve these patterns when modifying nearby code.

## License Header

Every Java file must start with the project license header.

Do not remove, rewrite, or reformat license headers.

For new Java files, use the existing header from nearby source files.

## Project Structure

```text
src/main/java/io/mapsmessaging/
  MessageDaemon.java
  api/
  auth/
  config/
  dto/
  engine/
  logging/
  network/protocol/impl/
  rest/

src/test/java/io/mapsmessaging/
  BaseTest.java
  test/BaseTestConfig.java

src/main/resources/
  MessageDaemon.yaml
  NetworkManager.yaml
```

Important areas:

```text
api/
```

Public API: sessions, destinations, messages, schemas.

```text
engine/
```

Core messaging engine: destinations, sessions, subscriptions, retain handling.

```text
network/protocol/impl/
```

Protocol implementations such as MQTT, AMQP, STOMP, CoAP, NMEA, and related handlers.

```text
config/
dto/
```

Configuration and data transfer objects.

```text
rest/
```

REST API using Jersey and Grizzly.

## Profiles and Special Builds

The `ml` Maven profile adds:

```text
src/main/java-ml
src/test/java-ml
```

The `native` Maven profile builds a GraalVM native image.

Do not modify profile behaviour unless explicitly requested.

## CI/CD

* Buildkite is the primary CI system.
* Main pipeline file:

```text
.buildkite/server_pipeline.yml
```

* GitHub Actions are used mainly for Dependabot auto-merge.
* SonarCloud is used for code quality.
* JaCoCo is used for coverage.
* OWASP dependency-check fails on CVSS >= 10.

## Commit Conventions

Conventional Commits are enforced.

Format:

```text
type(scope): subject
```

Allowed types include:

```text
feat
fix
refactor
perf
test
build
ci
docs
style
chore
revert
```

Common scopes include:

```text
server
ml
engine
config
schema
protocol
network
rest
buildkite
docs
```

Rules:

* Header must be no more than 100 characters.
* Use imperative mood.
* Use lowercase type and scope.
* Do not end the subject with a period.
* Include a footer.

Footer examples:

```text
JIRA: MAPS-123
NO-ISSUE
BREAKING CHANGE: description
DEPRECATED: description
SECURITY: description
```

Example:

```text
feat(protocol): add MQTT 5 bridge support

Implements protocol translation between MQTT 3.1.1 and 5.0.

JIRA: MAPS-123
```

Branch naming:

```text
MAPS-###-short-desc
```

## Dependency Notes

Key dependencies include:

* Lombok
* Logback
* SnakeYAML
* Jackson
* Jersey 4
* Grizzly
* Swagger/OpenAPI
* Apache Qpid Proton-J
* Eclipse Paho
* Californium
* Auth0 JWT
* AWS SDK / Cognito
* Pi4J
* MapDB
* Quartz Scheduler
* JMH

Do not add or upgrade dependencies unless the user specifically asks.

## Agent Response Format

When finishing a task, report:

* Files changed.
* Tests added or modified.
* Maven command run.
* Result of the Maven command.
* Any production issue noticed but not changed.
* Any remaining risk or untested area.

Keep the summary concise.

## Hard Restrictions

Do not:

* Delete keystores.
* Modify generated files unless asked.
* Reformat the whole project.
* Convert tests to a different framework.
* Replace the logging framework.
* Change public protocol semantics without explicit instruction.
* Make broad concurrency changes without tests.
* Replace existing SPI patterns.
* Add sleeps to tests unless there is no practical alternative.
* Hide failing tests by disabling them.
* Lower assertion strength just to make tests pass.
