# MSG-356 server SonarCloud remediation

Branch: `fix/MSG-356-server-sonar-remediation`. Baseline: 106 unresolved bugs/vulnerabilities exported on 30 September 2026.

## Implementation sequence

- Verify each finding against current development; record false-positive evidence without broad suppression.
- Add failing regression tests before fixing satellite field bounds, LoRa EOF/unsigned lengths and schema context fallback.
- Review null paths and resource ownership next, then cancellation, concurrency, REST and build download security.
- Run focused suites per change; run broader checks after focused tests pass. Keep commits focused with `Refs: MSG-356`.
- Open one PR; record scope, validation and risks in Jira before completion.

## Finding ledger

| Key | Rule | Component | Line | Assessment |
|---|---|---|---:|---|
| AaDC8CNiin_CbpmeOlIW | java:S4507 | src/main/java/io/mapsmessaging/state/drone/tak/TakTwinObserver.java | 337 | Pending source review |
| AaDC8COAin_CbpmeOlIc | java:S3077 | src/main/java/io/mapsmessaging/state/drone/tak/MtiStatusRegistry.java | 48 | Pending source review |
| AaDC8CSLin_CbpmeOlIt | java:S3077 | src/main/java/io/mapsmessaging/state/drone/drone/DroneTwin.java | 125 | Pending source review |
| AaDC8CSLin_CbpmeOlIu | java:S3077 | src/main/java/io/mapsmessaging/state/drone/drone/DroneTwin.java | 128 | Pending source review |
| AaDC8Cibin_CbpmeOlJ- | java:S3077 | src/main/java/io/mapsmessaging/state/mavlink/sender/MavlinkTransmissionState.java | 27 | False positive: volatile publishes an immutable Snapshot; narrow S3077 suppression with ownership rationale |
| AaDC8DrJin_CbpmeOlQZ | java:S3077 | src/main/java/io/mapsmessaging/network/io/connection/state/Disconnected.java | 50 | Pending source review |
| AaDC8CP8in_CbpmeOlIh | java:S2095 | src/main/java/io/mapsmessaging/state/drone/tak/TakSocketConnection.java | 181 | Fixed: failed reconnect closes pending socket; TLS handshake regression test passes |
| AaDC8CP8in_CbpmeOlIi | java:S2095 | src/main/java/io/mapsmessaging/state/drone/tak/TakSocketConnection.java | 186 | Fixed: failed reconnect closes pending socket; TLS handshake regression test passes |
| AaDC8Dkbin_CbpmeOlPM | java:S3077 | src/main/java/io/mapsmessaging/network/protocol/impl/mavlink/MavlinkProtocol.java | 84 | Pending source review |
| AaDC8CuSin_CbpmeOlKr | java:S2583 | src/main/java/io/mapsmessaging/state/config/DataProductConfigLoader.java | 23 | Pending source review |
| AaDC8CSfin_CbpmeOlI0 | java:S2445 | src/main/java/io/mapsmessaging/state/drone/publisher/TwinJsonPublisher.java | 117 | Pending source review |
| AaDC8CNiin_CbpmeOlIS | java:S2142 | src/main/java/io/mapsmessaging/state/drone/tak/TakTwinObserver.java | 110 | Pending source review |
| AaDC8CNiin_CbpmeOlIT | java:S4507 | src/main/java/io/mapsmessaging/state/drone/tak/TakTwinObserver.java | 112 | Pending source review |
| AaDC8CNiin_CbpmeOlIV | java:S4507 | src/main/java/io/mapsmessaging/state/drone/tak/TakTwinObserver.java | 317 | Pending source review |
| AaDC8C6hin_CbpmeOlLG | java:S2142 | src/main/java/io/mapsmessaging/state/TwinPublisherManager.java | 53 | Pending source review |
| AaDC8CSfin_CbpmeOlIx | java:S2142 | src/main/java/io/mapsmessaging/state/drone/publisher/TwinJsonPublisher.java | 104 | Pending source review |
| AaDC8CSfin_CbpmeOlIz | java:S2142 | src/main/java/io/mapsmessaging/state/drone/publisher/TwinJsonPublisher.java | 152 | Pending source review |
| AaDC8C0sin_CbpmeOlK2 | java:S2201 | src/main/java/io/mapsmessaging/state/n2k/N2kTwinUpdater.java | 53 | Pending source review |
| AaDC8D_cin_CbpmeOlU3 | java:S4507 | src/main/java/io/mapsmessaging/api/transformers/JSONToXML.java | 74 | Pending source review |
| AaDC8Drtin_CbpmeOlQd | java:S3077 | src/main/java/io/mapsmessaging/network/io/connection/EndPointConnection.java | 86 | Pending source review |
| AaDC8Drtin_CbpmeOlQe | java:S3077 | src/main/java/io/mapsmessaging/network/io/connection/EndPointConnection.java | 90 | Pending source review |
| AaDC8Drtin_CbpmeOlQf | java:S3077 | src/main/java/io/mapsmessaging/network/io/connection/EndPointConnection.java | 94 | Pending source review |
| AaDC8EPrin_CbpmeOlYh | java:S4507 | src/main/java/io/mapsmessaging/SubSystemManager.java | 98 | Pending source review |
| AaDC8Dj1in_CbpmeOlO8 | java:S4507 | src/main/java/io/mapsmessaging/network/protocol/impl/nmea/NMEAProtocolFactory.java | 82 | Pending source review |
| AaDC8Dj1in_CbpmeOlO9 | java:S4507 | src/main/java/io/mapsmessaging/network/protocol/impl/nmea/NMEAProtocolFactory.java | 91 | Pending source review |
| AaDC8CR2in_CbpmeOlIr | java:S2142 | src/main/java/io/mapsmessaging/state/drone/tak/EventPublisher.java | 150 | Pending source review |
| AaDC8DqIin_CbpmeOlQR | java:S3077 | src/main/java/io/mapsmessaging/network/io/impl/canbus/CanbusEndPoint.java | 42 | Pending source review |
| AaDC8EARin_CbpmeOlVB | java:S4507 | src/main/java/io/mapsmessaging/api/transformers/JsonMapperTransformation.java | 67 | Pending source review |
| AaDC8CSvin_CbpmeOlI5 | java:S2445 | src/main/java/io/mapsmessaging/state/drone/core/TwinManager.java | 118 | Pending source review |
| AaDC8DjJin_CbpmeOlOz | javabugs:S2259 | src/main/java/io/mapsmessaging/network/protocol/impl/extension/ExtensionProtocol.java | 125 | Pending source review |
| AaDC8EAAin_CbpmeOlU- | java:S2755 | src/main/java/io/mapsmessaging/api/transformers/xml/AttributeXmlBuilder.java | 145 | False positive: locally built DOM and identity transformer; method-level java:S2755 suppression with rationale; Sonar status not changed |
| AaDC8D_Kin_CbpmeOlU0 | javabugs:S2259 | src/main/java/io/mapsmessaging/api/transformers/JsonToSchema.java | 95 | Pending source review |
| AaDC8D69in_CbpmeOlTZ | javabugs:S2259 | src/main/java/io/mapsmessaging/engine/schema/SchemaManager.java | 121 | Pending source review |
| AaDC8ELAin_CbpmeOlXs | java:S2142 | src/main/java/io/mapsmessaging/license/LicenseServerClient.java | 82 | Pending source review |
| AaDC8Dhbin_CbpmeOlOl | java:S2142 | src/main/java/io/mapsmessaging/network/protocol/impl/satellite/gateway/protocol/SatelliteGatewayProtocol.java | 515 | Pending source review |
| AaDC8ER_in_CbpmeOlZp | java:S8745 | src/test/java/io/mapsmessaging/test/BaseTestConfig.java | 82 | Pending source review |
| AaDC8EJ-in_CbpmeOlXT | java:S4507 | src/main/java/io/mapsmessaging/aggregator/worker/AggregatorStripeWorker.java | 62 | Pending source review |
| AaDC8EJ-in_CbpmeOlXV | java:S4507 | src/main/java/io/mapsmessaging/aggregator/worker/AggregatorStripeWorker.java | 83 | Pending source review |
| AaDC8EQbin_CbpmeOlY5 | shell:S6506 | src/main/scripts/download-extensions.sh | 41 | Pending source review |
| AaDC8EQbin_CbpmeOlY6 | shell:S6506 | src/main/scripts/download-extensions.sh | 73 | Pending source review |
| AaDC8ECYin_CbpmeOlVm | java:S1751 | src/main/java/io/mapsmessaging/rest/api/impl/messaging/impl/RestMessageListener.java | 159 | Pending source review |
| AaDC8ECYin_CbpmeOlVn | java:S1751 | src/main/java/io/mapsmessaging/rest/api/impl/messaging/impl/RestMessageListener.java | 170 | Pending source review |
| AaDC8CMDin_CbpmeOlIK | javabugs:S2259 | src/main/java/io/mapsmessaging/utilities/filtering/NamespaceFilters.java | 38 | Fixed: absent namespace configuration produces an empty filter set; regression test passes |
| AaDC8EDFin_CbpmeOlVx | java:S2142 | src/main/java/io/mapsmessaging/rest/api/impl/server/ServerHealthApi.java | 201 | Pending source review |
| AaDC8ECyin_CbpmeOlVt | java:S2441 | src/main/java/io/mapsmessaging/rest/api/impl/destination/DestinationListManagementAPI.java | 189 | Pending source review |
| AaDC8EOXin_CbpmeOlYX | java:S3077 | src/main/java/io/mapsmessaging/hardware/trigger/PeriodicRunner.java | 49 | False positive: executor-produced Future is thread-safe; narrow S3077 suppression |
| AaDC8D-min_CbpmeOlUu | java:S4507 | src/main/java/io/mapsmessaging/tools/config/UpgradeTest.java | 60 | Pending source review |
| AaDC8D87in_CbpmeOlUU | javabugs:S2259 | src/main/java/io/mapsmessaging/tools/config/yaml/SchemaResolver.java | 76 | Fixed: null schema coerces to an empty object; regression test passes |
| AaDC8D69in_CbpmeOlTa | javabugs:S6322 | src/main/java/io/mapsmessaging/engine/schema/SchemaManager.java | 399 | Pending source review |
| AaDC8EMcin_CbpmeOlX9 | java:S2095 | src/main/java/io/mapsmessaging/auth/registry/AuthDbStoreManager.java | 130 | Pending source review |
| AaDC8EIZin_CbpmeOlWn | javabugs:S2259 | src/main/java/io/mapsmessaging/rest/responses/LoginResponse.java | 58 | Pending source review |
| AaDC8EJAin_CbpmeOlW2 | java:S3923 | src/main/java/io/mapsmessaging/rest/EndpointIntrospector.java | 71 | Pending source review |
| AaDC8ENein_CbpmeOlYO | java:S4507 | src/main/java/io/mapsmessaging/hardware/device/handler/serial/SerialDeviceBusHandler.java | 63 | Pending source review |
| AaDC8Da1in_CbpmeOlNk | java:S4507 | src/main/java/io/mapsmessaging/network/protocol/impl/nats/state/SessionState.java | 223 | Pending source review |
| AaDC8ER_in_CbpmeOlZo | java:S8745 | src/test/java/io/mapsmessaging/test/BaseTestConfig.java | 68 | Pending source review |
| AaDC8EBUin_CbpmeOlVV | java:S4507 | src/main/java/io/mapsmessaging/api/auth/SubscriptionAuthorisationCheck.java | 74 | Pending source review |
| AaDC8DIUin_CbpmeOlLt | java:S4507 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt/listeners/BaseConnectionListener.java | 60 | Pending source review |
| AaDC8DIwin_CbpmeOlLy | java:S4507 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt/listeners/SubscribeListener.java | 61 | Pending source review |
| AaDC8EMmin_CbpmeOlYC | java:S4507 | src/main/java/io/mapsmessaging/auth/registry/AuthenticationStorage.java | 88 | Pending source review |
| AaDC8D5zin_CbpmeOlTN | javabugs:S2259 | src/main/java/io/mapsmessaging/engine/destination/DestinationImpl.java | 343 | Pending source review |
| AaDC8DqTin_CbpmeOlQU | java:S4507 | src/main/java/io/mapsmessaging/network/io/impl/Selector.java | 81 | Pending source review |
| AaDC8Drhin_CbpmeOlQb | javabugs:S2259 | src/main/java/io/mapsmessaging/network/io/connection/route/Metrics.java | 101 | Fixed: protocol and endpoint are read once per metric; regression test passes |
| AaDC8DrTin_CbpmeOlQa | java:S4507 | src/main/java/io/mapsmessaging/network/io/connection/route/RouteManager.java | 153 | Pending source review |
| AaDC8EQJin_CbpmeOlY2 | javabugs:S2259 | src/main/java/io/mapsmessaging/MessageDaemon.java | 432 | Pending source review |
| AaDC8EPZin_CbpmeOlYe | java:S4507 | src/main/java/io/mapsmessaging/stats/StatsReporter.java | 127 | Pending source review |
| AaDC8DiYin_CbpmeOlOr | java:S6218 | src/main/java/io/mapsmessaging/network/protocol/impl/satellite/protocol/MessageQueuePacker.java | 35 | Pending source review |
| AaDC8ER0in_CbpmeOlZj | docker:S6506 | src/main/docker/arm/Dockerfile | 30 | Pending source review |
| AaDC8ER0in_CbpmeOlZk | docker:S6506 | src/main/docker/arm/Dockerfile | 40 | Pending source review |
| AaDC8Ds3in_CbpmeOlQr | java:S2095 | src/main/java/io/mapsmessaging/network/auth/Auth0TokenGenerator.java | 61 | Pending source review |
| AaDC8DgAin_CbpmeOlOB | javabugs:S6466 | src/main/java/io/mapsmessaging/network/protocol/impl/satellite/modem/device/messages/SendMessageState.java | 48 | Fixed field-count validation; legacy and OGx regressions pass |
| AaDC8DgAin_CbpmeOlOE | javabugs:S6466 | src/main/java/io/mapsmessaging/network/protocol/impl/satellite/modem/device/messages/SendMessageState.java | 50 | Fixed field-count validation; legacy and OGx regressions pass |
| AaDC8DgAin_CbpmeOlOF | javabugs:S6466 | src/main/java/io/mapsmessaging/network/protocol/impl/satellite/modem/device/messages/SendMessageState.java | 53 | Fixed field-count validation; legacy and OGx regressions pass |
| AaDC8DgAin_CbpmeOlOD | javabugs:S6466 | src/main/java/io/mapsmessaging/network/protocol/impl/satellite/modem/device/messages/SendMessageState.java | 54 | Fixed field-count validation; legacy and OGx regressions pass |
| AaDC8DgAin_CbpmeOlOC | javabugs:S6466 | src/main/java/io/mapsmessaging/network/protocol/impl/satellite/modem/device/messages/SendMessageState.java | 55 | Fixed field-count validation; legacy and OGx regressions pass |
| AaDC8ERqin_CbpmeOlZg | docker:S6506 | src/main/docker/Dockerfile | 32 | Pending source review |
| AaDC8ERSin_CbpmeOlZL | java:S2095 | src/main/java-ml/io/mapsmessaging/ml/llm/ChatGPTSelectorClient.java | 35 | Pending source review |
| AaDC8EQJin_CbpmeOlY1 | java:S4507 | src/main/java/io/mapsmessaging/MessageDaemon.java | 424 | Pending source review |
| AaDC8DIgin_CbpmeOlLw | java:S2142 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt/listeners/PublishListener.java | 97 | Pending source review |
| AaDC8Doain_CbpmeOlQB | javabugs:S2259 | src/main/java/io/mapsmessaging/network/io/impl/lora/LoRaEndPointServer.java | 130 | Fixed: unbind closes a present endpoint and remains idempotent; regression tests pass |
| AaDC8DIDin_CbpmeOlLs | javabugs:S2259 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt/packet/Publish.java | 166 | Pending source review |
| AaDC8EDmin_CbpmeOlV_ | java:S2441 | src/main/java/io/mapsmessaging/rest/api/impl/BaseRestApi.java | 160 | Pending source review |
| AaDC8EQTin_CbpmeOlY3 | shell:S6506 | src/main/scripts/download-smile.sh | 60 | Pending source review |
| AaDC8DSwin_CbpmeOlMs | java:S2142 | src/main/java/io/mapsmessaging/network/protocol/impl/nats/jetstream/stream/consumer/NamedConsumer.java | 86 | Pending source review |
| AaDC8DT_in_CbpmeOlNK | java:S2142 | src/main/java/io/mapsmessaging/network/protocol/impl/nats/jetstream/stream/api/handlers/StreamDeleteHandler.java | 83 | Pending source review |
| AaDC8DUQin_CbpmeOlNP | java:S2142 | src/main/java/io/mapsmessaging/network/protocol/impl/nats/jetstream/stream/api/handlers/StreamCreateHandler.java | 81 | Pending source review |
| AaDC8DTlin_CbpmeOlND | java:S2583 | src/main/java/io/mapsmessaging/network/protocol/impl/nats/jetstream/stream/api/handlers/StreamInfoHandler.java | 115 | Pending source review |
| AaDC8Disin_CbpmeOlOt | java:S2142 | src/main/java/io/mapsmessaging/network/protocol/impl/extension/Extension.java | 84 | Pending source review |
| AaDC8D0Zin_CbpmeOlSL | java:S4973 | src/main/java/io/mapsmessaging/config/RestApiManagerConfig.java | 111 | Pending source review |
| AaDC8EDmin_CbpmeOlV2 | java:S2583 | src/main/java/io/mapsmessaging/rest/api/impl/BaseRestApi.java | 170 | Pending source review |
| AaDC8Dsain_CbpmeOlQl | javabugs:S2259 | src/main/java/io/mapsmessaging/network/admin/EndPointStatisticsJMX.java | 78 | Pending source review |
| AaDC8Dsain_CbpmeOlQm | javabugs:S2259 | src/main/java/io/mapsmessaging/network/admin/EndPointStatisticsJMX.java | 83 | Pending source review |
| AaDC8EA1in_CbpmeOlVO | javabugs:S2259 | src/main/java/io/mapsmessaging/api/message/interceptors/impl/JMSTypeInterceptor.java | 34 | Pending source review |
| AaDC8D24in_CbpmeOlSu | javabugs:S2259 | src/main/java/io/mapsmessaging/engine/session/security/JaasSecurityContext.java | 62 | Pending source review |
| AaDC8ENNin_CbpmeOlYM | javabugs:S2259 | src/main/java/io/mapsmessaging/auth/AuthManager.java | 463 | Pending source review |
| AaDC8ENNin_CbpmeOlYK | javabugs:S2259 | src/main/java/io/mapsmessaging/auth/AuthManager.java | 473 | Pending source review |
| AaDC8ENNin_CbpmeOlYL | javabugs:S2259 | src/main/java/io/mapsmessaging/auth/AuthManager.java | 478 | Pending source review |
| AaDC8EMuin_CbpmeOlYE | java:S2119 | src/main/java/io/mapsmessaging/auth/registry/PasswordGenerator.java | 40 | Pending source review |
| AaDC8EQkin_CbpmeOlY9 | shell:S6506 | src/main/scripts/pack.sh | 22 | Pending source review |
| AaDC8DPoin_CbpmeOlMK | java:S4507 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt_sn/v2_0/state/AuthenticationState.java | 67 | Pending source review |
| AaDC8DPein_CbpmeOlMJ | java:S4507 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt_sn/v2_0/state/InitialConnectionState.java | 163 | Pending source review |
| AaDC8DCdin_CbpmeOlLj | javabugs:S2259 | src/main/java/io/mapsmessaging/network/protocol/impl/semtech/SemTechProtocol.java | 126 | Pending source review |
| AaDC8D4zin_CbpmeOlTA | java:S9354 | src/main/java/io/mapsmessaging/engine/destination/subscription/SubscriptionContext.java | 249 | Pending source review |
| AaDC8D34in_CbpmeOlS4 | javabugs:S2259 | src/main/java/io/mapsmessaging/engine/destination/subscription/builders/BrowserSubscriptionBuilder.java | 64 | Pending source review |
| AaDC8DoGin_CbpmeOlP9 | java:S9130 | src/main/java/io/mapsmessaging/network/io/impl/lora/serial/LoRaStreamHandler.java | 70 | Fixed EOF checks before casts and unsigned payload length; focused regressions pass |
| AaDC8DoGin_CbpmeOlP- | java:S9130 | src/main/java/io/mapsmessaging/network/io/impl/lora/serial/LoRaStreamHandler.java | 85 | Fixed EOF checks before casts and unsigned payload length; focused regressions pass |
| AaDC8ERqin_CbpmeOlZW | docker:S6506 | src/main/docker/Dockerfile | 24 | Pending source review |

## Validation and progress

Draft PR: https://github.com/Maps-Messaging/mapsmessaging_server/pull/2260. Jira: https://mapsmessaging.atlassian.net/browse/MSG-356.

Java 21 Maven compilation succeeded. The focused satellite, LoRa, schema, namespace and metrics suites passed 31 tests; TAK passed 4 tests. New regression tests reproduced defects before fixes. Full-suite execution and refreshed SonarCloud analysis remain pending. The remaining rows explicitly marked pending have not been adjudicated.

## Code-smell cleanup blocks

The 30 September development export contains 1,213 unresolved code smells. Use the existing branch and PR for every block, with focused commits. A fresh analysis is required to confirm that findings disappear.

### Block 1: imports and overrides

Added 37 missing `@Override` annotations and removed 24 unused or duplicate imports across 34 Java source/test files. No method bodies, signatures, runtime annotations or dependencies changed. No tests were added or modified beyond import/annotation cleanup.

Validation: Java 21 `mvn -B -DskipTests -Dexec.skip=true -Ddependency-check.skip=true test-compile` succeeded, compiling production and test sources. Full tests/build are deferred until the cleanup blocks are ready.

| Issue key | Rule | File |
|---|---|---|
| AaDC8DxDin_CbpmeOlRX | java:S1128 | src/main/java/io/mapsmessaging/config/device/SerialDeviceConfig.java |
| AaDC8CbWin_CbpmeOlJM | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/px4/GenericPx4UxvModel.java |
| AaDC8Cbrin_CbpmeOlJS | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8Cbrin_CbpmeOlJT | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8Cbrin_CbpmeOlJU | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8Cbrin_CbpmeOlJV | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8Cbrin_CbpmeOlJW | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8Cbrin_CbpmeOlJX | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8Cbrin_CbpmeOlJY | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8Cbrin_CbpmeOlJZ | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8Cbrin_CbpmeOlJa | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8Cbrin_CbpmeOlJb | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java |
| AaDC8CdLin_CbpmeOlJt | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/UavModel.java |
| AaDC8CdLin_CbpmeOlJu | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/UavModel.java |
| AaDC8CTFin_CbpmeOlJB | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/UgvModel.java |
| AaDC8CTFin_CbpmeOlJC | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/UgvModel.java |
| AaDC8CcJin_CbpmeOlJj | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/UsvModel.java |
| AaDC8CcJin_CbpmeOlJk | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/UsvModel.java |
| AaDC8Catin_CbpmeOlJF | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/UuvModel.java |
| AaDC8Catin_CbpmeOlJG | java:S1161 | src/main/java/io/mapsmessaging/state/mavlink/model/UuvModel.java |
| AaDC8EVfin_CbpmeOlaw | java:S1128 | src/test/java/io/mapsmessaging/aggregator/Envelope.java |
| AaDC8DMzin_CbpmeOlL2 | java:S1161 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt/MQTTProtocol.java |
| AaDC8DA9in_CbpmeOlLS | java:S1161 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt5/MQTT5Protocol.java |
| AaDC8CI0in_CbpmeOlHz | java:S1128 | src/main/java/io/mapsmessaging/dto/rest/config/transformer/impl/JsonMutateTransformationDTO.java |
| AaDC8CJBin_CbpmeOlH1 | java:S1128 | src/main/java/io/mapsmessaging/dto/rest/config/transformer/TransformationConfigDTO.java |
| AaDC8C1nin_CbpmeOlLC | java:S1128 | src/main/java/io/mapsmessaging/state/rest/twins/TwinManagementApi.java |
| AaDC8C1nin_CbpmeOlLD | java:S1128 | src/main/java/io/mapsmessaging/state/rest/twins/TwinManagementApi.java |
| AaDC8ELIin_CbpmeOlXt | java:S1128 | src/main/java/io/mapsmessaging/license/FeatureDetails.java |
| AaDC8ELIin_CbpmeOlXu | java:S1128 | src/main/java/io/mapsmessaging/license/FeatureDetails.java |
| AaDC8EKlin_CbpmeOlXo | java:S1128 | src/main/java/io/mapsmessaging/license/features/Hardware.java |
| AaDC8EKuin_CbpmeOlXp | java:S1128 | src/main/java/io/mapsmessaging/license/features/InterConnections.java |
| AaDC8EPrin_CbpmeOlYi | java:S1128 | src/main/java/io/mapsmessaging/SubSystemManager.java |
| AaDj9zzeHmxAf3IaJAdd | java:S1128 | src/test/java/io/mapsmessaging/network/protocol/impl/satellite/SatelliteReplicationTests.java |
| AaDC8CJBin_CbpmeOlH0 | java:S1128 | src/main/java/io/mapsmessaging/dto/rest/config/transformer/TransformationConfigDTO.java |
| AaDC8Dk_in_CbpmeOlPR | java:S1128 | src/main/java/io/mapsmessaging/network/protocol/impl/mavlink/MavlinkSerialProtocol.java |
| AaDC8CMQin_CbpmeOlIO | java:S1128 | src/main/java/io/mapsmessaging/utilities/configuration/ConfigurationManager.java |
| AaDC8CMQin_CbpmeOlIP | java:S1128 | src/main/java/io/mapsmessaging/utilities/configuration/ConfigurationManager.java |
| AaDC8CMQin_CbpmeOlIQ | java:S1128 | src/main/java/io/mapsmessaging/utilities/configuration/ConfigurationManager.java |
| AaDC8EUzin_CbpmeOlak | java:S1128 | src/test/java/io/mapsmessaging/api/transformers/TransformationAssertions.java |
| AaDC8EUrin_CbpmeOlaj | java:S1128 | src/test/java/io/mapsmessaging/api/transformers/TransformationTestSupport.java |
| AaDC8ENxin_CbpmeOlYQ | java:S1161 | src/main/java/io/mapsmessaging/hardware/device/handler/spi/SpiDeviceHandler.java |
| AaDC8ENxin_CbpmeOlYR | java:S1161 | src/main/java/io/mapsmessaging/hardware/device/handler/spi/SpiDeviceHandler.java |
| AaDj9zoeHmxAf3IaJAdb | java:S1128 | src/test/java/io/mapsmessaging/test/BaseTestConfig.java |
| AaDC8D4Win_CbpmeOlS7 | java:S1128 | src/main/java/io/mapsmessaging/engine/destination/subscription/impl/DestinationSubscription.java |
| AaDC8Dpjin_CbpmeOlQJ | java:S1161 | src/main/java/io/mapsmessaging/network/io/impl/tcp/TCPEndPoint.java |
| AaDC8Dpjin_CbpmeOlQL | java:S1161 | src/main/java/io/mapsmessaging/network/io/impl/tcp/TCPEndPoint.java |
| AaDC8DU0in_CbpmeOlNU | java:S1161 | src/main/java/io/mapsmessaging/network/protocol/impl/nats/jetstream/stream/transactions/TransactionManager.java |
| AaDC8DV_in_CbpmeOlNb | java:S1161 | src/main/java/io/mapsmessaging/network/protocol/impl/nats/frames/HPayloadFrame.java |
| AaDC8DWmin_CbpmeOlNf | java:S1161 | src/main/java/io/mapsmessaging/network/protocol/impl/nats/frames/ConnectFrame.java |
| AaDC8DW9in_CbpmeOlNh | java:S1161 | src/main/java/io/mapsmessaging/network/protocol/impl/nats/frames/PayloadFrame.java |
| AaDC8ESHin_CbpmeOlZt | java:S1128 | src/test/java/io/mapsmessaging/test/TestFeatureManager.java |
| AaDC8ESHin_CbpmeOlZs | java:S1161 | src/test/java/io/mapsmessaging/test/TestFeatureManager.java |
| AaDC8DjJin_CbpmeOlOy | java:S1161 | src/main/java/io/mapsmessaging/network/protocol/impl/extension/ExtensionProtocol.java |
| AaDC8CLQin_CbpmeOlIE | java:S1161 | src/main/java/io/mapsmessaging/utilities/stats/LinkedMovingAverages.java |
| AaDj9zoeHmxAf3IaJAda | java:S1128 | src/test/java/io/mapsmessaging/test/BaseTestConfig.java |
| AaDj9zoeHmxAf3IaJAdc | java:S1128 | src/test/java/io/mapsmessaging/test/BaseTestConfig.java |
| AaDC8DMzin_CbpmeOlL3 | java:S1161 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt/MQTTProtocol.java |
| AaDC8DA9in_CbpmeOlLT | java:S1161 | src/main/java/io/mapsmessaging/network/protocol/impl/mqtt5/MQTT5Protocol.java |
| AaDC8CLQin_CbpmeOlIC | java:S1161 | src/main/java/io/mapsmessaging/utilities/stats/LinkedMovingAverages.java |
| AaDC8CLQin_CbpmeOlID | java:S1161 | src/main/java/io/mapsmessaging/utilities/stats/LinkedMovingAverages.java |
| AaDC8EU9in_CbpmeOlal | java:S1161 | src/test/java/io/mapsmessaging/engine/session/FakeSecurityManager.java |

### Next scan

The export has 112 instanceof/cast findings, 12 switch-label consolidation findings, 2 modifier-order findings, 4 immediate-return findings and 1 empty statement. Review source semantics before applying each block. Do not bulk-convert loops, mutable list collectors, exception handling, serialized field names or equality methods solely to satisfy style rules.

### Block 2a: direct instanceof bindings

Converted 44 immediate checked casts into pattern bindings across 33 production files, retaining the existing local names and all subsequent statements. Repeated getters, generic map casts, and more complex flow scopes remain pending. Added six AMQP decoder tests for binary/scalar payloads, sequence order, text, absent bodies and unmatched bodies. Isolated network discovery test singleton initialization from daemon configuration.

Validation: 43 focused tests passed, zero failures/errors/skips, using Java 21 Maven test with Mockito JVM attachment enabled. The initial sandbox run could not attach Mockito; the unrestricted run exposed two network test configuration initialization errors, which were corrected in the test fixture. No production configuration behavior was changed.

### Block 3a: tested switch-label consolidation

Consolidated five empty fall-through label groups in NMEA TypeFactory and SemtechStatusEventFactory, preserving branch bodies/defaults. Existing NMEA tests exercise each scalar alias. Added parameterized Semtech tests for all 12 states through both factory methods and a null-state test. Validation: 28 tests passed, zero failures/errors/skips.

The Semtech gateway factory continues to reject GATEWAY_PUSH, matching its existing implementation. No semantics were changed to accommodate that state. Remaining pattern/cast and switch-label findings are still pending; do not count all 112 pattern or 12 switch findings as fixed.

### Block 4: small mechanical cleanup

Removed four immediate-return temporary variables, reordered two modifier declarations, and removed one empty statement. Added tests verifying fresh mutable configuration/maps, map insertion order, numeric generator bounds and equal-bound handling.

### Block 3b: remaining switch-label groups

Consolidated the remaining seven reported switch-label groups: JMS delivery mode, hardware sensor/clock dispatch, CoAP GET/PATCH, MQTT-SN v1/v2 duplicate CONNECT/WILLMSG responses, and NATS +ack/+term. Added direct dispatch contract tests for all grouped labels. Combined focused suite (SwitchDispatchContractTest, JsonInstanceGeneratorNumericTest, EmptyConfigurationResultTest, StaticAggregatorWorkSchedulerTest, StatisticsTests, DeviceSessionManagementTest, FieldInterceptorTest): 29 tests passed, no failures/errors/skips, Java 21 Maven. Full integration build remains deferred.

All 12 exported S6208 switch-label findings now have code changes; fresh SonarCloud analysis must confirm removal. The remaining S6201 pattern/cast findings are pending. No unrelated production bug was changed.

### Block 2b: remaining direct checked casts

Converted 66 further type checks/casts across 38 production files, retaining control-flow and statement order. This includes configuration single-item branches, privilege encoding, subscription handling, resource-statistics wrappers, AMQP/MQTT/NATS packet paths, reflection/schema helpers and UDP address checks. Existing unchecked generic behavior was preserved where a raw binding replaces a raw cast; no collections were converted to immutable forms. Repeated-getter cases, erased generic casts and the empty SerialConfig update remain pending for separate review. Conversion counts are source conditions, not a confirmed count of closed SonarCloud issues.

Validation: Java 21 compilation and 81 focused tests passed with no failures/errors/skips. Added four UDP address migration tests; all four passed, covering same-host port changes, controlled host changes, both non-InetSocketAddress boundaries and unknown clients. Full integration build and a fresh analysis remain deferred. Existing accumulated JaCoCo execution data produced class-mismatch warnings after recompilation; this block makes no coverage claim.

### Block 5a: duplicated configuration keys

Extracted 15 private String constants across SaslConfig, SslConfig, destination ConfigHelper, DestinationConfig, MessageOverrideConfig and ProtocolConfigFactory. Literal values, key casing, defaults, aliases and control flow are unchanged. This addresses 15 exported S1192 findings in source; closure awaits fresh analysis.

Validation: Java 21 Maven focused suite (SaslConfigTest, SslConfigTest, ConfigHelperTest, DestinationConfigTest, DestinationConfigBranchCoverageTest, MessageOverrideConfigTest, MessageOverrideConfigBranchCoverageTest, ProtocolConfigFactoryTest) passed: 29 tests, zero failures/errors/skips. Added a protocol messageDefaults regression covering exact retain/storeOffline keys and values through serialization and restoration. Existing tests cover SASL, TLS, destination settings and S3 archive mapping. Command: mvn -s /tmp/msg356-tools/settings.xml -B -Dtest=SaslConfigTest,SslConfigTest,ConfigHelperTest,DestinationConfigTest,DestinationConfigBranchCoverageTest,MessageOverrideConfigTest,MessageOverrideConfigBranchCoverageTest,ProtocolConfigFactoryTest -Dexec.skip=true -Ddependency-check.skip=true test.

JaCoCo report was generated, but accumulated execution data contains mismatches for other previously changed classes; no fresh overall coverage percentage is claimed. Full build and SonarCloud rescan remain pending.

### Block 5b: duplicated state configuration keys

Extracted 17 private String constants across TwinManagerConfig (11), CotConfigSupport (2) and n2k/N2KAisConfig (4). Exact inverse-substitution verification confirms production changes only replace literals and declare constants; key spelling, control flow and existing line endings are preserved. Fresh SonarCloud analysis must confirm issue closure.

Validation: 31 focused tests passed with zero failures/errors/skips using Java 21 Maven. Strengthened the CoT round-trip test for both error-distance keys/values. Added two twin regression tests covering MAVLink known sources in single/list forms and drone altitude serialization/restoration. Existing tests cover N2K PGN blocks, specialization, descriptions, publishing and geospatial configuration.

Fresh isolated JaCoCo execution data produced no class-mismatch warnings. Class coverage (line / branch): TwinManagerConfig 84.1% / 64.4%, CotConfigSupport 94.9% / 73.8%, N2KAisConfig 74.5% / 52.1%. These are focused-suite class measurements, not overall server coverage or SonarCloud new-code coverage. Some existing class branches remain uncovered; production edits are only literal substitutions. Full build and refreshed analysis remain pending.

Command: mvn -s /tmp/msg356-tools/settings.xml -B -Dtest=CotConfigSupportTest,N2KAisConfigBranchCoverageTest,TwinManagerConfigTest,TwinManagerConfigFinalCoverageTest,TwinManagerConfigPlanTaskTypeTest,GeoSpatialTwinManagerConfigTest -Djacoco.destFile=/tmp/msg356-state-constants-final.exec -Djacoco.dataFile=/tmp/msg356-state-constants-final.exec -Dexec.skip=true -Ddependency-check.skip=true test.

### Block 5c: duplicated MAVLink model strings

Extracted 12 private constants across six MAVLink model classes: common mission model, ArduPilot base, PX4 UAV/fixed-wing/UGV, and Stickleback USV. Exact inverse-substitution checks verified that only literals and constant declarations changed. Exception text, validation messages (including trailing spaces in the mission-item prefix), parameter labels and control flow are preserved. Fresh analysis must confirm issue closure.

Added MavlinkModelValidationMessageTest: 13 parameterized/test cases for null-context/request/plan diagnostics, exact indexed depth-validation messages across four concrete vehicle models, and the negative-radius parameter diagnostic. Final Java 21 focused model suite plus the PX4 drone command tests passed: 151 tests, no failures/errors/skips.

Initial existing-test failures were caused by Mockito being unable to self-attach its agent. Loading the cached Mockito agent at JVM startup resolved these without repository changes. Final command: JAVA_TOOL_OPTIONS=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar mvn -s /tmp/msg356-tools/settings.xml -B '-Dtest=io.mapsmessaging.state.mavlink.model.**,io.mapsmessaging.state.drone.model.GenericPx4UavModelTest' -Djacoco.destFile=/tmp/msg356-model-constants-final.exec -Djacoco.dataFile=/tmp/msg356-model-constants-final.exec -Dexec.skip=true -Ddependency-check.skip=true test.

Fresh focused-suite JaCoCo coverage (no class-mismatch warnings):

| Class | Line | Branch |
| --- | --- | --- |
| GenericPx4UgvModel | 68.0% | 52.6% |
| SticklebackArdupilotUsvModel | 69.1% | 56.4% |
| AbstractMissionUxvModel | 92.6% | 85.8% |
| GenericArduPilotUxvModel | 87.5% | 87.5% |
| GenericPx4UavModel | 95.4% | 86.5% |
| GenericPx4FixedWingUavModel | 100.0% | 100.0% |

These are focused class measurements, not whole-server or SonarCloud new-code coverage. Existing UGV/USV paths remain uncovered by this suite. Full integration build and refreshed analysis remain pending.

### Block 5d: duplicated REST configuration messages

Extracted four private constants across ConfigManagementApi and TwinConfigurationApi: configuration error messages, twin save error message, and the drone-info cache path. Runtime responses and OpenAPI descriptions use the same exact text; cache invalidation uses the original path. Exact inverse-substitution checks confirmed production changes are only constant declarations and literal replacement. Fresh analysis must confirm issue closure.

Added seven direct API response tests (ConfigManagementErrorResponseTest and TwinConfigurationErrorResponseTest): configuration read/update failure responses, twin read failures, persistence IOException mapping, unexpected update failures, and successful drone deletion invalidating both original cache paths. Authentication is isolated; the storage boundary is mocked to inject failures. No full daemon startup.

Validation: Java 21 Maven passed, 7 tests, zero failures/errors/skips. Fresh isolated JaCoCo report generated without class-mismatch warnings; no whole-API/server coverage claim. Command: JAVA_TOOL_OPTIONS=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar mvn -s /tmp/msg356-tools/settings.xml -B -Dtest=TwinConfigurationErrorResponseTest,ConfigManagementErrorResponseTest -Djacoco.destFile=/tmp/msg356-rest-constants-final.exec -Djacoco.dataFile=/tmp/msg356-rest-constants-final.exec -Dexec.skip=true -Ddependency-check.skip=true test. Full REST integration build remains deferred.

### Block 5e: duplicated REST resource response messages

Extracted six private constants across InterfaceInstanceApi, IntegrationInstanceManagementApi and ModelStoreApi. Endpoint/name validation, resource-not-found and ML-not-supported text are unchanged, including OpenAPI descriptions where applicable. Inverse-substitution verification confirms only literal replacement and declarations in production. Fresh analysis must confirm issue closure.

Added RestResourceValidationResponseTest with 13 executed cases covering null/empty/blank/malformed inputs across endpoint, integration and model operations; missing endpoint/integration responses; and all four model-store-unavailable responses, including servlet HTTP status updates. Authentication/cache lookup and network/storage services are isolated through scoped test doubles; no daemon starts.

Validation: Java 21 focused Maven suite passed (23 tests, zero failures/errors/skips), including prior configuration response tests and existing interface-state transition tests. Fresh isolated JaCoCo report generated without class-mismatch warnings; no overall coverage claim. Command: JAVA_TOOL_OPTIONS=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar mvn -s /tmp/msg356-tools/settings.xml -B -Dtest=RestResourceValidationResponseTest,InterfaceInstanceApiBranchCoverageTest,TwinConfigurationErrorResponseTest,ConfigManagementErrorResponseTest -Djacoco.destFile=/tmp/msg356-rest-resource-constants.exec -Djacoco.dataFile=/tmp/msg356-rest-resource-constants.exec -Dexec.skip=true -Ddependency-check.skip=true test. Full REST integration and a fresh SonarCloud analysis remain pending.

### Block 5f: complete remaining duplicated literals

Extracted the remaining 88 Java S1192 literals across 50 files, plus three shell S1192 findings in download-extensions.sh/generate.sh. Protocol names, JSON/schema keys, HTTP headers, route/cache paths, auth error messages, log text, enum values and shell arguments retain their exact values. Substitution reversal checks verified unchanged production statements/control flow. Class-level group/user route annotations retain literal spelling because private class constants cannot be referenced there; each route string now occurs only twice in source (annotation and private constant). Enum constructor literals use qualified private compile-time constants to avoid forward-reference errors. No public constants/APIs were added.

Validation: 271 focused Java 21 tests across 48 classes passed, zero failures/errors/skips. Existing coverage includes configuration, transformations, aggregation, MAVLink state/packets, schema/YAML/lint, REST responses, permissions, logs and CoT mapping. Added five auth response/route test cases for malformed UUIDs, missing users/groups and unchanged routes. A scheduler fixture race surfaced under the larger suite: enqueue 1,000 signals before starting its worker to deterministically verify coalescing. Its call-count assertion is unchanged; no scheduler production logic changed. This test-only fix has its own commit under MSG-356.

Command: JAVA_TOOL_OPTIONS=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar mvn -s /tmp/msg356-tools/settings.xml -B -Dtest=AdapterManagerTest,BaseRestApiBranchCoverageTest,BaseRestApiFinalCoverageTest,ConfigHelperTest,ConfigManagementErrorResponseTest,DeterministicJsonWriterTest,EndPointConfigFactoryTest,EnvelopeAggregationStrategyBranchCoverageTest,EnvelopeAggregationStrategyFinalCoverageTest,EnvelopeAggregationStrategyTest,JsonMapperTest,JsonQueryTransformationConfigCoverageSweepTest,JsonQueryTransformationTest,LinkConfigBranchCoverageTest,LinkConfigTest,LintEngineTest,MLModelManagerConfigBranchCoverageTest,MavlinkConfigBranchCoverageTest,MavlinkConfigDTOTest,MavlinkConfigFinalCoverageTest,MavlinkStateSubscriberTest,MavlinkTwinUpdaterTest,NMEAPacketTest,RestApiManagerConfigBranchCoverageTest,RestResourceValidationResponseTest,RuntimeJsonSchemaGeneratorFinalCoverageTest,SatelliteGatewayHardeningTest,SchemaQueryApiFinalCoverageTest,SchemaResolverBranchCoverageTest,SchemaResolverFinalCoverageTest,SchemaResolverTest,SemtechStatusEventFactoryTest,SerialConfigCoverageSweepTest,ServerLogMessagesTest,ServerPermissionsTest,StaticAggregatorWorkSchedulerTest,SwitchDispatchContractTest,TakEventMapperFinalCoverageTest,TakEventMapperTest,TwinConfigurationErrorResponseTest,TwinManagerConfigFinalCoverageTest,TwinManagerConfigPlanTaskTypeTest,TwinManagerConfigTest,TwinManagerGeoSpatialRegistryTest,TwinManagerTest,YamlNodeRendererBranchCoverageTest,YamlNodeRendererTest,AuthResourceValidationResponseTest -Djacoco.destFile=/tmp/msg356-all-literals-verified.exec -Djacoco.dataFile=/tmp/msg356-all-literals-verified.exec -Dexec.skip=true -Ddependency-check.skip=true test. Fresh JaCoCo report generated without class-mismatch warnings; no whole-server coverage claim.

Both changed shell scripts pass bash -n. Stubbed before/after executions match stdout, external command arguments, jar filenames and jar content exactly. curl/keytool/rm were stubbed; no network download or real certificate generation/deletion ran. Java token checks confirm all 142 exported literals now occur at most twice (usually once). All 145 Java/shell duplicated-literal findings have source changes; a fresh SonarCloud scan must confirm closure.

### Outstanding exported code-smell categories

Counts below describe the original 1,213-finding export, not refreshed SonarCloud results. Fully addressed rule categories (Override, imports, switch labels, immediate returns, modifiers, empty statement, Java/shell duplicated literals, nested-if merges and identical catches) account for 246 exported findings. The remaining 967 findings include the partly addressed 112 pattern/cast findings; they are not a count of currently open issues. Further reliability/security review is tracked in the original 106-finding table above. Full build, fresh coverage/analysis, and final merge remain pending.

| Rule | Export findings | Review status |
| --- | ---: | --- |
| java:S3776 | 114 | Pending review |
| java:S6201 | 112 | Partly addressed; repeated-getter/generic cases still need review |
| java:S9391 | 69 | Pending review |
| java:S1186 | 59 | Pending review |
| java:S1181 | 53 | Pending review |
| java:S108 | 36 | Pending review |
| java:S1172 | 35 | Pending review |
| java:S135 | 30 | Pending review |
| java:S116 | 21 | Pending review |
| java:S1135 | 19 | Pending review |
| java:S1168 | 18 | Pending review |
| java:S106 | 18 | Pending review |
| java:S107 | 16 | Pending review |
| java:S4165 | 16 | Pending review |
| java:S2925 | 14 | Pending review |
| java:S2160 | 14 | Pending review |
| java:S6885 | 13 | Pending review |
| java:S1130 | 13 | Pending review |
| java:S1481 | 12 | Pending review |
| java:S125 | 11 | Pending review |
| java:S1068 | 11 | Pending review |
| java:S8688 | 11 | Pending review |
| java:S3358 | 10 | Pending review |
| java:S9395 | 10 | Pending review |
| docker:S6570 | 10 | Pending review |
| java:S1948 | 10 | Pending review |
| java:S6880 | 9 | Pending review |
| java:S112 | 9 | Pending review |
| java:S117 | 9 | Pending review |
| java:S1118 | 8 | Pending review |
| java:S6204 | 8 | Pending review |
| java:S5665 | 8 | Pending review |
| java:S1854 | 8 | Pending review |
| java:S115 | 8 | Pending review |
| java:S1871 | 7 | Pending review |
| java:S2143 | 7 | Pending review |
| java:S1144 | 6 | Pending review |
| shelldre:S7688 | 6 | Pending review |
| java:S1141 | 5 | Pending review |
| java:S1117 | 5 | Pending review |
| java:S1845 | 5 | Pending review |
| java:S2065 | 5 | Pending review |
| powershelldre:S8677 | 5 | Pending review |
| java:S2442 | 4 | Pending review |
| java:S4144 | 4 | Pending review |
| java:S2093 | 3 | Pending review |
| java:S6355 | 3 | Pending review |
| java:S1133 | 3 | Pending review |
| java:S6916 | 3 | Pending review |
| java:S4276 | 3 | Pending review |
| java:S131 | 3 | Pending review |
| java:S3038 | 3 | Pending review |
| java:S6126 | 3 | Pending review |
| java:S1452 | 2 | Pending review |
| java:S1119 | 2 | Pending review |
| java:S1123 | 2 | Pending review |
| java:S1170 | 2 | Pending review |
| java:S1612 | 2 | Pending review |
| java:S5993 | 2 | Pending review |
| java:S1643 | 2 | Pending review |
| java:S5411 | 2 | Pending review |
| java:S6206 | 2 | Pending review |
| java:S3011 | 2 | Pending review |
| docker:S7019 | 2 | Pending review |
| java:S1700 | 2 | Pending review |
| java:S2864 | 2 | Pending review |
| java:S3252 | 2 | Pending review |
| java:S1450 | 2 | Pending review |
| java:S1659 | 2 | Pending review |
| java:S1905 | 2 | Pending review |
| java:S9396 | 2 | Pending review |
| shell:S6573 | 2 | Pending review |
| java:S3824 | 1 | Pending review |
| java:S127 | 1 | Pending review |
| java:S3398 | 1 | Pending review |
| java:S1121 | 1 | Pending review |
| java:S8786 | 1 | Pending review |
| java:S7158 | 1 | Pending review |
| java:S5976 | 1 | Pending review |
| java:S5778 | 1 | Pending review |
| java:S1313 | 1 | Pending review |
| java:S4030 | 1 | Pending review |
| java:S6213 | 1 | Pending review |
| java:S2696 | 1 | Pending review |
| java:S1075 | 1 | Pending review |
| java:S2589 | 1 | Pending review |
| java:S5361 | 1 | Pending review |
| docker:S7018 | 1 | Pending review |
| java:S2094 | 1 | Pending review |
| java:S2692 | 1 | Pending review |
| java:S3024 | 1 | Pending review |
| java:S3010 | 1 | Pending review |
| java:S4042 | 1 | Pending review |
| java:S1611 | 1 | Pending review |
| java:S9397 | 1 | Pending review |
| java:S3063 | 1 | Pending review |

### Block 6: nested conditions and identical catches

Merged all 14 exported S1066 nested-if findings across 13 files, preserving left-to-right short-circuit evaluation, null guards and existing actions. Consolidated all seven S2147 findings across five files using multi-catch; exception types, responses, messages, causes and interruption behavior remain unchanged. No suppression was added.

Validation: Java 21 compilation and 116 focused tests passed with zero failures, errors or skips, using fresh JaCoCo execution data. Added 27 regression cases for MQTT 3/5 will guards and encodings, CONNACK length handling, endpoint resume state transitions, Inmarsat authentication reset, twin altitude validation, REST error responses and satellite publication exceptions. Existing tests exercised WebSocket UTF-8 parsing, PROXY v2, file locking, schema generation, DTO resolution and NMEA parsing. Test utility edits were compiled; no external daemon or broker was required. Full build and fresh Sonar analysis remain pending.

## Latest supplied snapshot and continued cleanup

The supplied `maps-server-code-smells(1).json` snapshot was exported at 2026-09-30 20:26 UTC and contains 876 findings, compared with 1,213 originally: 355 original issue keys are absent and 18 new keys are present (net reduction 337). The user confirms the branch label should be ignored; treat this as the latest scan. The PR has not merged. Continue on the same MSG-356 branch and PR.

The largest remaining groups are cognitive complexity (113), S9391 (69), empty methods (59), broad catches (53), empty blocks (36), unused parameters (35), loop exits (30) and naming (21). Record-constructor S4165 self-assignment reports need review rather than blind assignment removal. Unused-local removals must preserve initializer side effects, casts, parsing, authentication and exceptions.

### Block 7a: discarded wire values

Remove unused locals for the MQTT 5 UNSUBACK reason code and LoRa gateway minor version while retaining both `packet.get()` calls. This addresses two S1481 findings and the overlapping LoRa S1854 finding without changing byte consumption, failure behavior or protocol dispatch.

Six new regression cases passed against unchanged production code before cleanup. Post-change Java 21 `mvn -B -Dtest=UnsubAck5ConsumptionTest,VersionHandlerConsumptionTest,ConnAck5LengthContractTest -Dexec.skip=true -Ddependency-check.skip=true test` passed all nine tests with zero failures/errors/skips and fresh isolated JaCoCo data. Six cases are new, covering zero/one/multiple reason codes, following-byte boundaries, truncation, configuration send order and missing-version failures. Full build remains deferred.
