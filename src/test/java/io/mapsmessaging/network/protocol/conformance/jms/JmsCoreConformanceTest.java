/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.jms;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("jms")
@Tag("amqp10")
class JmsCoreConformanceTest extends JmsConformanceSupport {

  private static final String JMS_SOURCE =
      "https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1.html";

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Sections 4.1.2, 6.2 and 7: queue producer/consumer send and receive",
      source = JMS_SOURCE)
  void queueSendReceivePreservesTextAndProperties() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue);
           MessageConsumer consumer = session.createConsumer(queue)) {
        connection.start();

        TextMessage sent = session.createTextMessage("queue-payload");
        sent.setStringProperty("source", "conformance");
        sent.setIntProperty("sequence", 42);
        producer.send(sent);

        TextMessage received = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
        assertEquals("queue-payload", received.getText());
        assertEquals("conformance", received.getStringProperty("source"));
        assertEquals(42, received.getIntProperty("sequence"));
      }
      queue.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Sections 4.2.2 and 8: topic publish/subscribe semantics",
      source = JMS_SOURCE)
  void topicPublicationIsDeliveredToActiveSubscriber() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryTopic topic = session.createTemporaryTopic();
      try (MessageProducer producer = session.createProducer(topic);
           MessageConsumer consumer = session.createConsumer(topic)) {
        connection.start();
        producer.send(session.createTextMessage("topic-payload"));

        TextMessage received = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
        assertEquals("topic-payload", received.getText());
      }
      topic.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 3.8: message selectors use message headers and properties",
      source = JMS_SOURCE)
  void selectorDeliversOnlyMatchingMessages() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue);
           MessageConsumer france = session.createConsumer(queue, "Country = 'France'")) {
        connection.start();

        TextMessage germany = session.createTextMessage("berlin");
        germany.setStringProperty("Country", "Germany");
        producer.send(germany);

        TextMessage matching = session.createTextMessage("paris");
        matching.setStringProperty("Country", "France");
        producer.send(matching);

        TextMessage received = assertInstanceOf(TextMessage.class, france.receive(3_000));
        assertEquals("paris", received.getText());
        assertNull(france.receiveNoWait());
      }
      queue.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 6.2.10 and 3.5.11: CLIENT_ACKNOWLEDGE, recover, JMSRedelivered and JMSXDeliveryCount",
      source = JMS_SOURCE)
  void clientAcknowledgeRecoverRedeliversUnacknowledgedMessage() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.CLIENT_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue);
           MessageConsumer consumer = session.createConsumer(queue)) {
        connection.start();
        producer.send(session.createTextMessage("redeliver-me"));

        Message first = consumer.receive(3_000);
        assertNotNull(first);
        assertFalse(first.getJMSRedelivered());
        assertEquals(1, first.getIntProperty("JMSXDeliveryCount"));

        session.recover();

        Message redelivered = consumer.receive(3_000);
        assertNotNull(redelivered);
        assertTrue(redelivered.getJMSRedelivered());
        assertTrue(redelivered.getIntProperty("JMSXDeliveryCount") >= 2);
        redelivered.acknowledge();
      }
      queue.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 6.2.7: transacted Session commit and rollback semantics",
      source = JMS_SOURCE)
  void transactionRollbackSuppressesProducedMessageAndCommitPublishesIt() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.SESSION_TRANSACTED)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue);
           MessageConsumer consumer = session.createConsumer(queue)) {
        connection.start();

        producer.send(session.createTextMessage("rolled-back"));
        session.rollback();
        assertNull(consumer.receive(300));

        producer.send(session.createTextMessage("committed"));
        session.commit();

        TextMessage committed = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
        assertEquals("committed", committed.getText());
        session.commit();
      }
      queue.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 6.2.2: temporary destinations are scoped to a connection and may be consumed by another Session on it",
      source = JMS_SOURCE)
  void temporaryQueueCanBeUsedAcrossSessionsInSameConnection() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session producerSession = connection.createSession(Session.AUTO_ACKNOWLEDGE);
         Session consumerSession = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = producerSession.createTemporaryQueue();
      try (MessageProducer producer = producerSession.createProducer(queue);
           MessageConsumer consumer = consumerSession.createConsumer(queue)) {
        connection.start();
        producer.send(producerSession.createTextMessage("temporary"));

        TextMessage received = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
        assertEquals("temporary", received.getText());
      }
      queue.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 8.3.3: durable subscription retains messages while inactive",
      source = JMS_SOURCE)
  void durableSubscriptionReceivesMessagePublishedWhileConsumerClosed() throws Exception {
    String clientId = "jms-durable-" + UUID.randomUUID();
    String subscription = "sub-" + UUID.randomUUID();

    try (CloseableNamingContext naming = namingContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      Topic topic = (Topic) naming.lookup("topicExchange");
      String selector = "conformanceId = '" + clientId + "'";

      try (Connection connection = factory.createConnection()) {
        connection.setClientID(clientId);
        try (Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
             MessageConsumer consumer = session.createDurableConsumer(topic, subscription, selector, false)) {
          connection.start();
        }
      }

      try (Connection publisherConnection = factory.createConnection();
           Session publisherSession = publisherConnection.createSession(Session.AUTO_ACKNOWLEDGE);
           MessageProducer producer = publisherSession.createProducer(topic)) {
        publisherConnection.start();
        TextMessage message = publisherSession.createTextMessage("offline-durable");
        message.setStringProperty("conformanceId", clientId);
        producer.send(message);
      }

      try (Connection connection = factory.createConnection()) {
        connection.setClientID(clientId);
        try (Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
             MessageConsumer consumer = session.createDurableConsumer(topic, subscription, selector, false)) {
          connection.start();
          TextMessage received = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
          assertEquals("offline-durable", received.getText());
          consumer.close();
          session.unsubscribe(subscription);
        }
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Sections 3.7 and 3.10: TextMessage, BytesMessage and MapMessage bodies and properties",
      source = JMS_SOURCE)
  void textBytesAndMapMessagesRoundTrip() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue);
           MessageConsumer consumer = session.createConsumer(queue)) {
        connection.start();

        producer.send(session.createTextMessage("text"));

        BytesMessage bytes = session.createBytesMessage();
        bytes.writeBytes("bytes".getBytes(StandardCharsets.UTF_8));
        producer.send(bytes);

        MapMessage map = session.createMapMessage();
        map.setString("key", "value");
        map.setInt("count", 7);
        producer.send(map);

        assertEquals("text", assertInstanceOf(TextMessage.class, consumer.receive(3_000)).getText());

        BytesMessage receivedBytes = assertInstanceOf(BytesMessage.class, consumer.receive(3_000));
        byte[] payload = new byte[(int) receivedBytes.getBodyLength()];
        assertEquals(payload.length, receivedBytes.readBytes(payload));
        assertArrayEquals("bytes".getBytes(StandardCharsets.UTF_8), payload);

        MapMessage receivedMap = assertInstanceOf(MapMessage.class, consumer.receive(3_000));
        assertEquals("value", receivedMap.getString("key"));
        assertEquals(7, receivedMap.getInt("count"));
      }
      queue.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 3.4.9: expired messages must not be delivered",
      source = JMS_SOURCE)
  void expiredMessageIsNotDelivered() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue)) {
        producer.setTimeToLive(200);
        producer.send(session.createTextMessage("expires"));
      }

      Thread.sleep(500);
      try (MessageConsumer consumer = session.createConsumer(queue)) {
        connection.start();
        assertNull(consumer.receive(300));
      }
      queue.delete();
    }
  }
}
