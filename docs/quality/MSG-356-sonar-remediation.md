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
