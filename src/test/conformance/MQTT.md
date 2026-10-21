# MQTT Java Conformance Suite

The Java conformance suite replaces the historical Python Paho interoperability wrapper and expands beyond it.

## Running

- All MQTT conformance: `mvn test -Dgroups=conformance`
- Fast/core conformance: `mvn test -Dgroups=conformance-core`
- Full/stateful conformance: `mvn test -Dgroups=conformance-full`
- MQTT 3.1.1 only: `mvn test -Dgroups=mqtt311`
- MQTT 5 only: `mvn test -Dgroups=mqtt5`

The normal Maven/Jenkins test run still executes these tests because they are ordinary JUnit tests.

## Design rules

1. Wire-format and protocol-error tests use `MqttWireClient`, not MAPS packet classes.
2. Java Paho is used only where an independent client simplifies stateful semantic testing.
3. Every test carries `@ProtocolRequirement` with the OASIS specification, section/requirement, and authoritative URL.
4. External/historical Paho behaviour is not authoritative. The OASIS MQTT specification is.
5. `conformance-core` is intended for routine CI. `conformance-full` contains slower timing, reconnect and flow-control tests.

## Historical Paho MQTT 3.1.1 parity

| Historical behaviour | Java coverage |
|---|---|
| retained messages | `Mqtt311SemanticConformanceTest.retainedMessagesAreDeliveredToNewSubscription` |
| zero-length client ID | `Mqtt311FullConformanceTest.zeroLengthClientIdentifierRequiresCleanSession` |
| offline message queueing | `Mqtt311SemanticConformanceTest.persistentSessionQueuesQosOneAndTwoWhileOffline` |
| overlapping subscriptions | `Mqtt311SemanticConformanceTest.overlappingSubscriptionsUsePermittedDeliverySemantics` |
| keep alive / Will | `Mqtt311FullConformanceTest.keepAliveTimeoutPublishesWill` |
| redelivery on reconnect | `Mqtt311FullConformanceTest.unacknowledgedQosOnePublishIsRedeliveredWithDupAfterReconnect` |
| subscribe failure | `Mqtt311SemanticConformanceTest.unauthorisedSubscriptionReturnsFailureCode` |
| $ topic wildcard isolation | `Mqtt311SemanticConformanceTest.rootWildcardDoesNotMatchDollarPrefixedTopic` |
| unsubscribe | `Mqtt311SemanticConformanceTest.unsubscribeStopsFurtherDelivery` |

## Historical Paho MQTT 5 parity

| Historical behaviour | Java coverage |
|---|---|
| basic connect/publish/subscribe | core + semantic suites |
| retained message | `retainedMessagePreservesUserProperties` |
| Will message / keep alive | full suite Will tests |
| zero-length / assigned Client ID | `emptyClientIdReceivesAssignedClientIdentifier` |
| offline queueing | `persistentSessionQueuesQosOneAndTwoWhileOffline` |
| overlapping subscriptions | `overlappingSubscriptionsUsePermittedDeliverySemantics` |
| redelivery on reconnect | `unacknowledgedQosOnePublishIsRedeliveredWithDupAfterReconnect` |
| subscribe failure | `unauthorisedSubscriptionReturnsFailureReason` |
| $ topic wildcard isolation | `rootWildcardDoesNotMatchDollarPrefixedTopic` |
| unsubscribe | `unsubscribeStopsFurtherDelivery` |
| session expiry | `zeroSessionExpiryDoesNotRestoreSession` |
| User Property | `publishUserPropertiesRoundTrip` |
| Payload Format / Content Type | `payloadFormatAndContentTypeRoundTrip` |
| publication expiry | `expiredOfflinePublicationIsNotDelivered` |
| No Local / Retain As Published / Retain Handling | semantic + full suites |
| assigned Client ID | `emptyClientIdReceivesAssignedClientIdentifier` |
| subscription identifiers | `subscriptionIdentifierIsReturnedOnMatchingPublish` |
| request/response properties | `requestResponsePropertiesRoundTrip` |
| client topic alias validation | `topicAliasZeroCausesTopicAliasInvalidDisconnect` |
| server topic alias | optional capability; validated only when negotiated, not required |
| maximum packet size | `serverDoesNotSendPacketLargerThanClientMaximum` |
| Server Keep Alive | `serverKeepAliveOverridesLargerClientValue` |
| Receive Maximum / flow control | both directions in full suite |
| Will Delay | `willDelayDefersPublicationUntilDelayExpires` |
| shared subscriptions | `sharedSubscriptionDistributesEachMessageOnce` |

## Additional conformance coverage beyond historical Paho

The Java suite additionally checks:

- arbitrary TCP fragmentation and multiple MQTT packets in one TCP write;
- MQTT Variable Byte Integer boundary encoding;
- invalid reserved fixed-header flags;
- non-zero Packet Identifier requirements;
- malformed and non-minimal Variable Byte Integers;
- malformed UTF-8;
- illegal Topic Filters and wildcard use in Topic Names;
- Receive Maximum zero/duplicate property errors;
- Subscription Identifier zero and client-side PUBLISH misuse;
- illegal Subscription Options including No Local on shared subscriptions;
- QoS value 3 rejection;
- unknown PUBREC -> PUBREL Packet Identifier not found;
- Topic Alias zero handling;
- full connection lifetime / error-close behaviour;
- outbound QoS 2 PUBLISH/PUBREC/PUBREL/PUBCOMP state machines for MQTT 3.1.1 and MQTT 5;
- MQTT 5 unsupported Enhanced Authentication method rejection and connection closure.
