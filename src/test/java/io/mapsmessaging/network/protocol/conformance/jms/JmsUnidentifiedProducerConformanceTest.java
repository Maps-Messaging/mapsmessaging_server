/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.jms;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Tag("conformance")
@Tag("conformance-core")
@Tag("jms")
@Tag("amqp10")
class JmsUnidentifiedProducerConformanceTest extends JmsConformanceSupport {

  private static final String JMS_SOURCE =
      "https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1.html";

  @Test
  @Disabled("Known conformance gap: MSG-398")
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Sections 2.8 and 7.1: JMSProducer.send supplies the Destination on every send",
      source = JMS_SOURCE)
  void simplifiedApiProducerSendsToSuppliedDestination() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         JMSContext context =
             ((ConnectionFactory) naming.lookup("qpidConnectionfactory"))
                 .createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = context.createTemporaryQueue();
      context.createProducer().send(queue, "simplified");
      TextMessage received =
          assertInstanceOf(TextMessage.class, context.createConsumer(queue).receive(3_000));
      assertEquals("simplified", received.getText());
    }
  }

  @Test
  @Disabled("Known conformance gap: MSG-398")
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 7.1: unidentified MessageProducer created with null Destination sends to Destination supplied per call",
      source = JMS_SOURCE)
  void classicUnidentifiedProducerSendsToSuppliedDestination() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection =
             ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(null);
           MessageConsumer consumer = session.createConsumer(queue)) {
        connection.start();
        producer.send(queue, session.createTextMessage("classic-unidentified"));
        TextMessage received = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
        assertEquals("classic-unidentified", received.getText());
      }
      queue.delete();
    }
  }
}
