/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.jms;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.network.protocol.impl.amqp.jms.BaseConnection;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.util.UUID;
import javax.naming.Context;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("conformance")
@Tag("conformance-semantic")
@Tag("jms")
@Tag("jms-amqp")
class JmsAmqpSemanticConformanceTest extends BaseConnection {

  private static final String SOURCE =
      "https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1.html";

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Sections 6.2.1, 7 and 8: session-created producer and consumer exchange a message",
      source = SOURCE)
  void producerAndConsumerExchangeMessageOverAmqpProvider() throws Exception {
    Context context = loadContext();
    try {
      ConnectionFactory factory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
      assertNotNull(factory);

      try (Connection connection = factory.createConnection()) {
        connection.start();

        try (Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
          Topic topic = session.createTopic("conformance.jms.amqp." + UUID.randomUUID());

          try (MessageConsumer consumer = session.createConsumer(topic);
               MessageProducer producer = session.createProducer(topic)) {
            String payload = "jms-amqp-" + UUID.randomUUID();
            producer.send(session.createTextMessage(payload));

            TextMessage received = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
            assertNotNull(received);
            assertEquals(payload, received.getText());
          }
        }
      }
    } finally {
      context.close();
    }
  }
}
