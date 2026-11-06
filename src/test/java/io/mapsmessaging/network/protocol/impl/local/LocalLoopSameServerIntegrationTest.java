/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.local;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.io.impl.noop.NoOpEndPoint;
import io.mapsmessaging.test.BaseTestConfig;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Full messaging-engine tests: local loop connections never open a socket or a second server. */
class LocalLoopSameServerIntegrationTest extends BaseTestConfig {

  private static final AtomicInteger NEXT_ENDPOINT = new AtomicInteger(130_000);
  private final List<LocalLoopProtocol> protocols = new ArrayList<>();
  private final List<Session> sessions = new ArrayList<>();

  @AfterEach
  void closeOwnedSessionsAndProtocols() throws Exception {
    try {
      for (LocalLoopProtocol protocol : protocols) {
        protocol.close();
      }
    } finally {
      for (Session session : sessions) {
        close(session);
      }
    }
  }

  @Test
  void configuredLocalLoopRoutesAnEventBackIntoTheSameServer() throws Exception {
    String id = UUID.randomUUID().toString();
    String source = "/msg120/" + id + "/source";
    String destination = "/msg120/" + id + "/result";
    CountDownLatch received = new CountDownLatch(1);
    AtomicReference<Message> delivered = new AtomicReference<>();
    Session receiver = session(id + "-receiver", event -> {
      delivered.set(event.getMessage());
      event.getCompletionTask().run();
      received.countDown();
    });
    receiver.addSubscription(new SubscriptionContextBuilder(destination, ClientAcknowledgement.AUTO)
        .setReceiveMaximum(20).build());
    LocalLoopProtocol loop = loop(id + "-loop", source, destination);
    Session publisher = session(id + "-publisher", event -> event.getCompletionTask().run());

    Message original = new MessageBuilder()
        .setMeta(new LinkedHashMap<>(Map.of("msg120-trace", id)))
        .setOpaqueData(bytes("loop-to-self"))
        .build();
    publish(publisher, source, original);

    assertTrue(received.await(10, TimeUnit.SECONDS), "No same-server loop delivery from " + source + " to " + destination);
    assertNotNull(delivered.get());
    assertArrayEquals(bytes("loop-to-self"), delivered.get().getOpaqueData());
    assertEquals(id, delivered.get().getMeta().get("msg120-trace"));
    assertEquals(id + "-loop", loop.getSessionId());
    assertNotEquals(loop.getSessionId(), publisher.getName());
  }

  @Test
  void twoConfiguredLoopSessionsRouteThroughTwoNamespacesWithoutCycle() throws Exception {
    String id = UUID.randomUUID().toString();
    String input = "/msg120/" + id + "/input";
    String middle = "/msg120/" + id + "/middle";
    String output = "/msg120/" + id + "/output";
    CountDownLatch received = new CountDownLatch(1);
    AtomicReference<Message> delivered = new AtomicReference<>();
    Session receiver = session(id + "-receiver", event -> {
      delivered.set(event.getMessage());
      event.getCompletionTask().run();
      received.countDown();
    });
    receiver.addSubscription(new SubscriptionContextBuilder(output, ClientAcknowledgement.AUTO)
        .setReceiveMaximum(20).build());

    LocalLoopProtocol first = loop(id + "-loop-A", input, middle);
    LocalLoopProtocol second = loop(id + "-loop-B", middle, output);
    assertNotEquals(first.getSessionId(), second.getSessionId());
    assertNotSame(first.getSession(), second.getSession());
    Session publisher = session(id + "-publisher", event -> event.getCompletionTask().run());

    publish(publisher, input, new MessageBuilder()
        .setMeta(new LinkedHashMap<>(Map.of("msg120-trace", id)))
        .setOpaqueData(bytes("two-hops"))
        .build());

    assertTrue(received.await(10, TimeUnit.SECONDS), "Two-loop chain did not deliver to " + output);
    assertNotNull(delivered.get());
    assertArrayEquals(bytes("two-hops"), delivered.get().getOpaqueData());
    assertEquals(id, delivered.get().getMeta().get("msg120-trace"));
  }

  private LocalLoopProtocol loop(String sessionId, String source, String destination) throws Exception {
    NoOpEndPoint endpoint = new NoOpEndPoint(NEXT_ENDPOINT.incrementAndGet(), null, List.of());
    LocalLoopProtocol protocol = (LocalLoopProtocol) new LocalLoopProtocolFactory()
        .connect(endpoint, sessionId, "anonymous", "", Map.of(source, destination));
    protocols.add(protocol);
    protocol.subscribeLocal(source, destination, QualityOfService.AT_MOST_ONCE, null, null, null, null, null);
    return protocol;
  }

  private Session session(String id, io.mapsmessaging.api.MessageListener listener) throws Exception {
    Session session = createSession(id, 60, 0, false, listener);
    sessions.add(session);
    return session;
  }

  private static void publish(Session publisher, String topic, Message message) throws Exception {
    Destination destination = publisher.findDestination(topic, DestinationType.TOPIC).get(5, TimeUnit.SECONDS);
    assertNotNull(destination, "Unable to resolve publisher destination " + topic);
    destination.storeMessage(message);
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
