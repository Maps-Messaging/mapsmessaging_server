# AMQP 1.0 and JMS-over-AMQP Conformance

This suite validates AMQP 1.0 wire behaviour independently from Jakarta Messaging semantics layered on top of AMQP.

## Run modes

- All conformance: `mvn test -Dgroups=conformance`
- AMQP 1.0: `mvn test -Dgroups=amqp10`
- JMS-over-AMQP: `mvn test -Dgroups=jms`
- Core/fast: `mvn test -Dgroups=conformance-core`
- Full/stateful: `mvn test -Dgroups=conformance-full`

Normal Jenkins test runs also execute these classes because they are ordinary JUnit tests.

## AMQP transport coverage

Authoritative source:
OASIS AMQP 1.0, primarily Part 2 Transport and Part 5 Security.

Current Java-native coverage includes:

- AMQP 1.0 protocol-header negotiation;
- unsupported-version negotiation;
- protocol-header fragmentation;
- minimum frame SIZE;
- minimum and bounded DOFF;
- frame TYPE validation;
- initial 512-byte maximum-frame rule;
- OPEN negotiation and channel 0;
- OPEN-before-other-performatives state rule;
- BEGIN session establishment;
- empty heartbeat frame acceptance after OPEN;
- orderly CLOSE handshake;
- SASL protocol-header negotiation and SASL-MECHANISMS framing.

Confirmed server gaps are kept as disabled conformance tests with Jira references so the suite remains runnable while preserving executable evidence.

## JMS-over-AMQP coverage

Authoritative API source:
Jakarta Messaging 3.1.

The JMS suite uses Apache Qpid JMS as an independent AMQP 1.0 client and validates:

- queue send/receive;
- topic publish/subscribe;
- message properties;
- selectors;
- AUTO_ACKNOWLEDGE and CLIENT_ACKNOWLEDGE recovery;
- JMSRedelivered and mandatory JMSXDeliveryCount;
- local transactions, commit and rollback;
- temporary destinations and cross-session scope;
- durable subscriptions with offline retention;
- shared durable subscriptions;
- TextMessage, BytesMessage, MapMessage, StreamMessage and ObjectMessage;
- QueueBrowser non-destructive browsing;
- delivery mode, priority and expiration headers;
- message expiration;
- delivery delay / JMSDeliveryTime.

## Design rule

AMQP conformance and JMS conformance are deliberately separate:

- AMQP tests prove wire and transport/state-machine behaviour.
- JMS tests prove Jakarta Messaging API semantics as exposed through Qpid JMS over the AMQP listener.

A passing JMS test does not replace an AMQP wire-level requirement, and a passing AMQP frame test does not prove JMS API semantics.
