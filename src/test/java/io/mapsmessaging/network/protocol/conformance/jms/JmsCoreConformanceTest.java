/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.jms;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.network.protocol.impl.amqp.jms.BaseConnection;
import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.naming.Context;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("jms")
@Tag("amqp10")
class JmsCoreConformanceTest extends BaseConnection {

  private static final String JMS_SOURCE =
      "https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1.html";

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Sections 4.1.2 and 6.2: queue semantics and session producer/consumer creation",
      source = JMS_SOURCE)
  void queueSendReceivePreservesTextAndProperties() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        JMSProducer producer = context.createProducer();
        JMSConsumer consumer = context.createConsumer(queue);

        TextMessage sent = context.createTextMessage("queue-payload");
        sent.setStringProperty("source", "conformance");
        sent.setIntProperty("sequence", 42);
        producer.send(queue, sent);

        Message raw = consumer.receive(3_000);
        TextMessage received = assertInstanceOf(TextMessage.class, raw);
        assertEquals("queue-payload", received.getText());
        assertEquals("conformance", received.getStringProperty("source"));
        assertEquals(42, received.getIntProperty("sequence"));

        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 4.2.2: topic semantics",
      source = JMS_SOURCE)
  void topicPublicationIsDeliveredToActiveSubscriber() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        Topic topic = context.createTemporaryTopic();
        JMSConsumer consumer = context.createConsumer(topic);
        context.createProducer().send(topic, "topic-payload");

        TextMessage received = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
        assertEquals("topic-payload", received.getText());
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 3.8: message selectors use message headers and properties",
      source = JMS_SOURCE)
  void selectorDeliversOnlyMatchingMessages() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        JMSConsumer france = context.createConsumer(queue, "Country = 'France'");
        JMSProducer producer = context.createProducer();

        TextMessage germany = context.createTextMessage("berlin");
        germany.setStringProperty("Country", "Germany");
        producer.send(queue, germany);

        TextMessage matching = context.createTextMessage("paris");
        matching.setStringProperty("Country", "France");
        producer.send(queue, matching);

        TextMessage received = assertInstanceOf(TextMessage.class, france.receive(3_000));
        assertEquals("paris", received.getText());
        assertNull(france.receiveNoWait());

        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 6.2.10: CLIENT_ACKNOWLEDGE and session recovery",
      source = JMS_SOURCE)
  void clientAcknowledgeRecoverRedeliversUnacknowledgedMessage() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.CLIENT_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        context.createProducer().send(queue, "redeliver-me");
        JMSConsumer consumer = context.createConsumer(queue);

        Message first = consumer.receive(3_000);
        assertNotNull(first);
        assertFalse(first.getJMSRedelivered());

        context.recover();

        Message redelivered = consumer.receive(3_000);
        assertNotNull(redelivered);
        assertTrue(redelivered.getJMSRedelivered());
        redelivered.acknowledge();

        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 6.2.7: transacted session commit and rollback semantics",
      source = JMS_SOURCE)
  void transactionRollbackSuppressesProducedMessageAndCommitPublishesIt() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.SESSION_TRANSACTED)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        JMSProducer producer = context.createProducer();
        JMSConsumer consumer = context.createConsumer(queue);

        producer.send(queue, "rolled-back");
        context.rollback();
        assertNull(consumer.receive(300));

        producer.send(queue, "committed");
        context.commit();

        TextMessage committed = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
        assertEquals("committed", committed.getText());
        context.commit();

        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 6.2.2: temporary destination scope and lifetime",
      source = JMS_SOURCE)
  void temporaryQueueCanBeUsedAcrossSessionsInSameConnectionContext() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext parent = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE);
           JMSContext sibling = parent.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = parent.createTemporaryQueue();
        JMSConsumer consumer = sibling.createConsumer(queue);
        parent.createProducer().send(queue, "temporary");

        TextMessage received = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
        assertEquals("temporary", received.getText());

        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 8.3.3: durable subscription retains messages while consumer is inactive",
      source = JMS_SOURCE)
  void durableSubscriptionReceivesMessagePublishedWhileConsumerClosed() throws Exception {
    String clientId = "jms-durable-" + UUID.randomUUID();
    String subscription = "sub-" + UUID.randomUUID();

    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      Topic topic = (Topic) naming.lookup("topicExchange");

      try (JMSContext durable = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        durable.setClientID(clientId);
        JMSConsumer consumer = durable.createDurableConsumer(topic, subscription);
        consumer.close();
      }

      try (JMSContext publisher = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TextMessage message = publisher.createTextMessage("offline-durable");
        message.setStringProperty("conformanceId", clientId);
        publisher.createProducer().send(topic, message);
      }

      try (JMSContext durable = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        durable.setClientID(clientId);
        JMSConsumer consumer = durable.createDurableConsumer(
            topic,
            subscription,
            "conformanceId = '" + clientId + "'",
            false);
        TextMessage received = assertInstanceOf(TextMessage.class, consumer.receive(3_000));
        assertEquals("offline-durable", received.getText());
        consumer.close();
        durable.unsubscribe(subscription);
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Sections 3.7 and 3.10: standard message body types and properties",
      source = JMS_SOURCE)
  void textBytesAndMapMessagesRoundTrip() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        JMSConsumer consumer = context.createConsumer(queue);
        JMSProducer producer = context.createProducer();

        TextMessage text = context.createTextMessage("text");
        producer.send(queue, text);

        BytesMessage bytes = context.createBytesMessage();
        bytes.writeBytes("bytes".getBytes(StandardCharsets.UTF_8));
        producer.send(queue, bytes);

        MapMessage map = context.createMapMessage();
        map.setString("key", "value");
        map.setInt("count", 7);
        producer.send(queue, map);

        assertEquals("text", assertInstanceOf(TextMessage.class, consumer.receive(3_000)).getText());

        BytesMessage receivedBytes = assertInstanceOf(BytesMessage.class, consumer.receive(3_000));
        byte[] payload = new byte[(int) receivedBytes.getBodyLength()];
        assertEquals(payload.length, receivedBytes.readBytes(payload));
        assertArrayEquals("bytes".getBytes(StandardCharsets.UTF_8), payload);

        MapMessage receivedMap = assertInstanceOf(MapMessage.class, consumer.receive(3_000));
        assertEquals("value", receivedMap.getString("key"));
        assertEquals(7, receivedMap.getInt("count"));

        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 3.4.9: message expiration",
      source = JMS_SOURCE)
  void expiredMessageIsNotDelivered() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        JMSProducer producer = context.createProducer()
            .setDeliveryMode(DeliveryMode.NON_PERSISTENT)
            .setTimeToLive(200);
        producer.send(queue, "expires");

        Thread.sleep(500);

        JMSConsumer consumer = context.createConsumer(queue);
        assertNull(consumer.receive(300));
        queue.delete();
      }
    }
  }
}
