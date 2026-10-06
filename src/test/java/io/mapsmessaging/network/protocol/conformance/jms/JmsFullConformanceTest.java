/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.jms;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSConsumer;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-full")
@Tag("jms")
@Tag("amqp10")
class JmsFullConformanceTest extends JmsConformanceSupport {

  private static final String JMS_SOURCE =
      "https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1.html";

  @Test
  @Disabled("Known AMQP link-close conformance gap: MSG-397")
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 8.3.4: shared durable subscription delivers each message to only one consumer",
      source = JMS_SOURCE)
  void sharedDurableSubscriptionDistributesEachMessageOnce() throws Exception {
    String subscription = "shared-" + UUID.randomUUID();
    String marker = UUID.randomUUID().toString();
    CountDownLatch received = new CountDownLatch(12);
    AtomicInteger first = new AtomicInteger();
    AtomicInteger second = new AtomicInteger();

    try (CloseableNamingContext naming = namingContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      Topic topic = (Topic) naming.lookup("topicExchange");

      try (Connection subscriberConnection = factory.createConnection();
           Session subscriberSession = subscriberConnection.createSession(Session.AUTO_ACKNOWLEDGE);
           MessageConsumer one = subscriberSession.createSharedDurableConsumer(
               topic, subscription, "conformanceId = '" + marker + "'");
           MessageConsumer two = subscriberSession.createSharedDurableConsumer(
               topic, subscription, "conformanceId = '" + marker + "'");
           Connection publisherConnection = factory.createConnection();
           Session publisherSession = publisherConnection.createSession(Session.AUTO_ACKNOWLEDGE);
           MessageProducer producer = publisherSession.createProducer(topic)) {
        subscriberConnection.start();
        publisherConnection.start();

        one.setMessageListener(message -> {
          first.incrementAndGet();
          received.countDown();
        });
        two.setMessageListener(message -> {
          second.incrementAndGet();
          received.countDown();
        });

        for (int i = 0; i < 12; i++) {
          TextMessage message = publisherSession.createTextMessage("shared-" + i);
          message.setStringProperty("conformanceId", marker);
          producer.send(message);
        }

        assertTrue(received.await(5, TimeUnit.SECONDS), "Expected every shared message exactly once");
        assertEquals(12, first.get() + second.get());

        one.close();
        two.close();
        subscriberSession.unsubscribe(subscription);
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 4.1.6: QueueBrowser enumerates queue messages without removing them",
      source = JMS_SOURCE)
  void queueBrowserDoesNotConsumeMessages() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue)) {
        for (int i = 0; i < 3; i++) {
          producer.send(session.createTextMessage("browse-" + i));
        }
      }

      connection.start();
      try (QueueBrowser browser = session.createBrowser(queue)) {
        Enumeration<?> enumeration = browser.getEnumeration();
        int browsed = 0;
        while (enumeration.hasMoreElements()) {
          assertNotNull(enumeration.nextElement());
          browsed++;
        }
        assertEquals(3, browsed);
      }

      try (MessageConsumer consumer = session.createConsumer(queue)) {
        for (int i = 0; i < 3; i++) {
          assertNotNull(consumer.receive(3_000));
        }
        assertNull(consumer.receiveNoWait());
      }
      queue.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Sections 3.4.3, 3.4.9, 3.4.10 and 7.4: delivery mode, expiration and priority headers",
      source = JMS_SOURCE)
  void deliveryOptionsAreReflectedInReceivedHeaders() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue);
           MessageConsumer consumer = session.createConsumer(queue)) {
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer.setPriority(8);
        producer.setTimeToLive(0);
        connection.start();

        producer.send(session.createTextMessage("headers"));

        Message received = consumer.receive(3_000);
        assertNotNull(received);
        assertEquals(DeliveryMode.PERSISTENT, received.getJMSDeliveryMode());
        assertEquals(8, received.getJMSPriority());
        assertEquals(0, received.getJMSExpiration());
      }
      queue.delete();
    }
  }

  @Test
  @Disabled("Known conformance gap: MSG-399")
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 7.9: delivery delay prevents delivery before JMSDeliveryTime",
      source = JMS_SOURCE)
  void deliveryDelayDefersMessageAvailability() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue);
           MessageConsumer consumer = session.createConsumer(queue)) {
        producer.setDeliveryDelay(400);
        connection.start();

        long sentAt = System.currentTimeMillis();
        producer.send(session.createTextMessage("delayed"));

        assertNull(consumer.receive(150));
        Message delayed = consumer.receive(2_000);
        assertNotNull(delayed);
        assertTrue(delayed.getJMSDeliveryTime() >= sentAt + 300);
      }
      queue.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 3.10.4: StreamMessage body is a stream of typed values",
      source = JMS_SOURCE)
  void streamMessageRoundTripsTypedValues() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue);
           MessageConsumer consumer = session.createConsumer(queue)) {
        StreamMessage sent = session.createStreamMessage();
        sent.writeString("stream");
        sent.writeInt(7);
        sent.writeLong(9_000_000_000L);
        producer.send(sent);

        connection.start();
        StreamMessage received = assertInstanceOf(StreamMessage.class, consumer.receive(3_000));
        assertEquals("stream", received.readString());
        assertEquals(7, received.readInt());
        assertEquals(9_000_000_000L, received.readLong());
      }
      queue.delete();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 3.10.5: ObjectMessage contains a Serializable Java object",
      source = JMS_SOURCE)
  void objectMessageRoundTripsSerializableValue() throws Exception {
    try (CloseableNamingContext naming = namingContext();
         Connection connection = ((ConnectionFactory) naming.lookup("qpidConnectionfactory")).createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
      TemporaryQueue queue = session.createTemporaryQueue();
      try (MessageProducer producer = session.createProducer(queue);
           MessageConsumer consumer = session.createConsumer(queue)) {
        producer.send(session.createObjectMessage("serializable-value"));
        connection.start();

        ObjectMessage received = assertInstanceOf(ObjectMessage.class, consumer.receive(3_000));
        assertEquals("serializable-value", received.getObject());
      }
      queue.delete();
    }
  }
}
