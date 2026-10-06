/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.jms;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.network.protocol.impl.amqp.jms.BaseConnection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.QueueBrowser;
import jakarta.jms.StreamMessage;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.Context;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-full")
@Tag("jms")
@Tag("amqp10")
class JmsFullConformanceTest extends BaseConnection {

  private static final String JMS_SOURCE =
      "https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1.html";

  @Test
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

    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      Topic topic = (Topic) naming.lookup("topicExchange");

      try (JMSContext subscriber = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE);
           JMSContext publisher = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        String selector = "conformanceId = '" + marker + "'";
        JMSConsumer one = subscriber.createSharedDurableConsumer(topic, subscription, selector);
        JMSConsumer two = subscriber.createSharedDurableConsumer(topic, subscription, selector);

        one.setMessageListener(message -> {
          first.incrementAndGet();
          received.countDown();
        });
        two.setMessageListener(message -> {
          second.incrementAndGet();
          received.countDown();
        });

        JMSProducer producer = publisher.createProducer();
        for (int i = 0; i < 12; i++) {
          TextMessage message = publisher.createTextMessage("shared-" + i);
          message.setStringProperty("conformanceId", marker);
          producer.send(topic, message);
        }

        assertTrue(received.await(5, TimeUnit.SECONDS), "Expected every shared message exactly once");
        assertEquals(12, first.get() + second.get());

        one.close();
        two.close();
        subscriber.unsubscribe(subscription);
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 6.2: Session is a factory for QueueBrowser; browsing does not consume queue messages",
      source = JMS_SOURCE)
  void queueBrowserDoesNotConsumeMessages() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        JMSProducer producer = context.createProducer();
        for (int i = 0; i < 3; i++) {
          producer.send(queue, "browse-" + i);
        }

        QueueBrowser browser = context.createBrowser(queue);
        Enumeration<?> enumeration = browser.getEnumeration();
        int browsed = 0;
        while (enumeration.hasMoreElements()) {
          assertNotNull(enumeration.nextElement());
          browsed++;
        }
        browser.close();
        assertEquals(3, browsed);

        JMSConsumer consumer = context.createConsumer(queue);
        for (int i = 0; i < 3; i++) {
          assertNotNull(consumer.receive(3_000));
        }
        assertNull(consumer.receiveNoWait());
        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Sections 3.4.3, 3.4.9, 3.4.10 and 7.4: delivery mode, expiration and priority headers",
      source = JMS_SOURCE)
  void deliveryOptionsAreReflectedInReceivedHeaders() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        JMSProducer producer = context.createProducer()
            .setDeliveryMode(DeliveryMode.PERSISTENT)
            .setPriority(8)
            .setTimeToLive(0);

        producer.send(queue, "headers");

        Message received = context.createConsumer(queue).receive(3_000);
        assertNotNull(received);
        assertEquals(DeliveryMode.PERSISTENT, received.getJMSDeliveryMode());
        assertEquals(8, received.getJMSPriority());
        assertEquals(0, received.getJMSExpiration());
        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 7.9: delivery delay prevents delivery before JMSDeliveryTime",
      source = JMS_SOURCE)
  void deliveryDelayDefersMessageAvailability() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        JMSConsumer consumer = context.createConsumer(queue);

        long sentAt = System.currentTimeMillis();
        context.createProducer().setDeliveryDelay(400).send(queue, "delayed");

        assertNull(consumer.receive(150));
        Message delayed = consumer.receive(2_000);
        assertNotNull(delayed);
        assertTrue(delayed.getJMSDeliveryTime() >= sentAt + 300);
        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 3.10.4: StreamMessage body is a stream of typed values",
      source = JMS_SOURCE)
  void streamMessageRoundTripsTypedValues() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        StreamMessage sent = context.createStreamMessage();
        sent.writeString("stream");
        sent.writeInt(7);
        sent.writeLong(9_000_000_000L);
        context.createProducer().send(queue, sent);

        StreamMessage received =
            assertInstanceOf(StreamMessage.class, context.createConsumer(queue).receive(3_000));
        assertEquals("stream", received.readString());
        assertEquals(7, received.readInt());
        assertEquals(9_000_000_000L, received.readLong());
        queue.delete();
      }
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "Jakarta-Messaging-3.1",
      value = "Section 3.10.5: ObjectMessage contains a Serializable Java object",
      source = JMS_SOURCE)
  void objectMessageRoundTripsSerializableValue() throws Exception {
    try (Context naming = loadContext()) {
      ConnectionFactory factory = (ConnectionFactory) naming.lookup("qpidConnectionfactory");
      try (JMSContext context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
        TemporaryQueue queue = context.createTemporaryQueue();
        ObjectMessage sent = context.createObjectMessage("serializable-value");
        context.createProducer().send(queue, sent);

        ObjectMessage received =
            assertInstanceOf(ObjectMessage.class, context.createConsumer(queue).receive(3_000));
        assertEquals("serializable-value", received.getObject());
        queue.delete();
      }
    }
  }
}
