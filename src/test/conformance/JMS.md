# JMS-over-AMQP Java Conformance Suite

This suite validates Jakarta Messaging API semantics exercised through the AMQP provider. It is intentionally separate from the AMQP 1.0 wire-level suite.

## Authoritative source

Jakarta Messaging 3.1:
https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1.html

AMQP framing and transport behaviour belongs in the AMQP 1.0 conformance suite and uses the OASIS AMQP 1.0 specifications as its authority.

## Coverage plan

- connection and session lifecycle;
- queues and topics;
- producers and consumers;
- AUTO_ACKNOWLEDGE, CLIENT_ACKNOWLEDGE and transacted session semantics where supported;
- durable and shared subscriptions where supported;
- temporary destinations;
- selectors;
- Jakarta Messaging message types and properties;
- delivery mode, priority and expiry;
- recovery/redelivery and JMSRedelivered/JMSXDeliveryCount;
- exception and close/lifecycle behaviour.

Every behavioural test carries a ProtocolRequirement reference to the Jakarta Messaging specification section it validates.
