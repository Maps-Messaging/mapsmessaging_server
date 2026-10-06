/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.amqp;

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
@Tag("amqp10")
class Amqp10SemanticConformanceTest extends BaseConnection {

  private static final String SOURCE =
      "https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-transport-v1.0-os.html";

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 sections 2.4, 2.5, 2.6 and 2.7: connection, session, link and transfer",
      source = SOURCE)
  void messageTransfersAcrossEstablishedConnectionSessionAndLinks() throws Exception {
    Context context = loadContext();
    try {
      ConnectionFactory factory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
      assertNotNull(factory);

      try (Connection connection = factory.createConnection()) {
        connection.start();

        try (Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
          Topic topic = session.createTopic("conformance.amqp." + UUID.randomUUID());

          try (MessageConsumer consumer = session.createConsumer(topic);
               MessageProducer producer = session.createProducer(topic)) {
            String payload = "amqp-transfer-" + UUID.randomUUID();
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
