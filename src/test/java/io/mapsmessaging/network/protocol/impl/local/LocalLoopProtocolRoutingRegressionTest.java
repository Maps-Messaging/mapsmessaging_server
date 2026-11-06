/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.local;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.network.io.impl.noop.NoOpEndPoint;
import io.mapsmessaging.utilities.admin.JMXManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the real loop protocol and its asynchronous delivery path without sockets. */
class LocalLoopProtocolRoutingRegressionTest {

  private static final AtomicInteger NEXT_ENDPOINT = new AtomicInteger(120_000);

  private final Map<String, Session> sessions = new HashMap<>();
  private final List<LocalLoopProtocol> protocols = new ArrayList<>();
  private MockedStatic<SessionManager> sessionManagerStatic;
  private SessionManager sessionManager;
  private boolean previousJmx;

  @BeforeEach
  void setUp() throws Exception {
    previousJmx = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    sessionManager = mock(SessionManager.class);
    sessionManagerStatic = mockStatic(SessionManager.class);
    sessionManagerStatic.when(SessionManager::getInstance).thenReturn(sessionManager);
    when(sessionManager.create(any(SessionContext.class), any(MessageListener.class)))
        .thenAnswer(invocation -> sessions.get(((SessionContext) invocation.getArgument(0)).getId()));
  }

  @AfterEach
  void tearDown() throws Exception {
    try {
      for (LocalLoopProtocol protocol : protocols) {
        protocol.close();
      }
    } finally {
      sessionManagerStatic.close();
      JMXManager.setEnableJMX(previousJmx);
    }
  }

  @Test
  void twoLoopConnectionsKeepSeparateSessionIdentitiesAndTopicMaps() throws Exception {
    Session first = registerSession("msg120-loop-A");
    Session second = registerSession("msg120-loop-B");

    LocalLoopProtocol loopA = loop("msg120-loop-A", Map.of("/source/a", "/target/a"));
    LocalLoopProtocol loopB = loop("msg120-loop-B", Map.of("/source/b", "/target/b"));

    assertSame(first, loopA.getSession());
    assertSame(second, loopB.getSession());
    assertNotSame(loopA.getSession(), loopB.getSession());
    assertEquals("msg120-loop-A", loopA.getSessionId());
    assertEquals("msg120-loop-B", loopB.getSessionId());
    assertEquals("/target/a", loopA.getTopicNameMapping().get("/source/a"));
    assertEquals("/target/b", loopB.getTopicNameMapping().get("/source/b"));
    assertFalse(loopA.getTopicNameMapping().containsKey("/source/b"));
    assertFalse(loopB.getTopicNameMapping().containsKey("/source/a"));
  }

  @Test
  void restartAfterCloseCreatesNewLoopSessionWithTheSameConfiguredIdentity() throws Exception {
    Session oldSession = registerSession("msg120-reconnect");
    LocalLoopProtocol oldProtocol = loop("msg120-reconnect", Map.of());
    oldProtocol.close();
    assertTrue(oldProtocol.getEndPoint().isClosed());

    Session replacement = registerSession("msg120-reconnect");
    LocalLoopProtocol newProtocol = loop("msg120-reconnect", Map.of());

    assertSame(oldSession, oldProtocol.getSession());
    assertSame(replacement, newProtocol.getSession());
    assertNotSame(oldProtocol.getSession(), newProtocol.getSession());
    assertEquals("msg120-reconnect", newProtocol.getSessionId());
    assertFalse(newProtocol.getEndPoint().isClosed());
    verify(sessionManager, times(1)).close(oldSession, false);
  }

  @Test
  void subscriptionAndRoutingUseTheConfiguredDestinationAndCopyTheMessage() throws Exception {
    String source = "/msg120/source/a";
    String target = "/msg120/result/a";
    Session session = registerSession("msg120-route");
    Destination destination = mock(Destination.class);
    when(session.findDestination(target, DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    LocalLoopProtocol protocol = loop("msg120-route", Map.of());
    protocol.subscribeLocal(source, target, QualityOfService.AT_MOST_ONCE, null, null, null, null, null);

    CountDownLatch completed = new CountDownLatch(1);
    Message original = message("unchanged");
    protocol.sendMessage(event(source, original, completed));

    assertTrue(completed.await(5, TimeUnit.SECONDS), "No loop completion for " + source + " -> " + target);
    ArgumentCaptor<Message> stored = ArgumentCaptor.forClass(Message.class);
    verify(destination, times(1)).storeMessage(stored.capture());
    assertNotSame(original, stored.getValue());
    assertArrayEquals(original.getOpaqueData(), stored.getValue().getOpaqueData());
    verify(session).addSubscription(any(SubscriptionContext.class));
    verify(session).resumeState();
    verify(session, times(1)).findDestination(target, DestinationType.TOPIC);
  }

  @Test
  void transformerRunsBeforeDestinationStoreAndDoesNotRunAgainOnDelivery() throws Exception {
    String source = "/msg120/source/transformed";
    String target = "/msg120/result/transformed";
    Session session = registerSession("msg120-transform");
    Destination destination = mock(Destination.class);
    when(session.findDestination(target, DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    InterServerTransformation transformation = mock(InterServerTransformation.class);
    when(transformation.transform(eq(source), any(ParsedMessage.class))).thenAnswer(invocation -> {
      ParsedMessage parsed = invocation.getArgument(1);
      parsed.setMessage(new MessageBuilder(parsed.getMessage()).setOpaqueData(bytes("transformed")).build());
      return parsed;
    });
    LocalLoopProtocol protocol = loop("msg120-transform", Map.of());
    protocol.subscribeLocal(source, target, QualityOfService.AT_MOST_ONCE, null, transformation, null, null, null);

    CountDownLatch completed = new CountDownLatch(1);
    protocol.sendMessage(event(source, message("original"), completed));

    assertTrue(completed.await(5, TimeUnit.SECONDS));
    ArgumentCaptor<Message> stored = ArgumentCaptor.forClass(Message.class);
    verify(destination).storeMessage(stored.capture());
    assertArrayEquals(bytes("transformed"), stored.getValue().getOpaqueData());
    verify(transformation, times(1)).transform(eq(source), any(ParsedMessage.class));
  }

  @Test
  void simultaneousLoopsDeliverToTheirOwnDestinations() throws Exception {
    Session first = registerSession("msg120-parallel-A");
    Session second = registerSession("msg120-parallel-B");
    Destination targetA = mock(Destination.class);
    Destination targetB = mock(Destination.class);
    when(first.findDestination("/msg120/parallel/outA", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(targetA));
    when(second.findDestination("/msg120/parallel/outB", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(targetB));
    LocalLoopProtocol a = loop("msg120-parallel-A", Map.of("/msg120/parallel/inA", "/msg120/parallel/outA"));
    LocalLoopProtocol b = loop("msg120-parallel-B", Map.of("/msg120/parallel/inB", "/msg120/parallel/outB"));

    CountDownLatch completed = new CountDownLatch(2);
    a.sendMessage(event("/msg120/parallel/inA", message("A"), completed));
    b.sendMessage(event("/msg120/parallel/inB", message("B"), completed));

    assertTrue(completed.await(5, TimeUnit.SECONDS), "Both independent loop completions are required");
    ArgumentCaptor<Message> aMessage = ArgumentCaptor.forClass(Message.class);
    ArgumentCaptor<Message> bMessage = ArgumentCaptor.forClass(Message.class);
    verify(targetA, times(1)).storeMessage(aMessage.capture());
    verify(targetB, times(1)).storeMessage(bMessage.capture());
    assertArrayEquals(bytes("A"), aMessage.getValue().getOpaqueData());
    assertArrayEquals(bytes("B"), bMessage.getValue().getOpaqueData());
    verify(first, never()).findDestination("/msg120/parallel/outB", DestinationType.TOPIC);
    verify(second, never()).findDestination("/msg120/parallel/outA", DestinationType.TOPIC);
  }

  @Test
  void destinationLookupFailureCompletesWithoutAttemptingToStore() throws Exception {
    Session session = registerSession("msg120-lookup-failure");
    CompletableFuture<Destination> failed = new CompletableFuture<>();
    failed.completeExceptionally(new IOException("lookup failed"));
    when(session.findDestination("/msg120/missing", DestinationType.TOPIC)).thenReturn(failed);
    LocalLoopProtocol protocol = loop("msg120-lookup-failure", Map.of("/msg120/source", "/msg120/missing"));

    CountDownLatch completed = new CountDownLatch(1);
    protocol.sendMessage(event("/msg120/source", message("event"), completed));

    assertTrue(completed.await(5, TimeUnit.SECONDS), "Failed lookup must complete its delivery callback");
    verify(session).findDestination("/msg120/missing", DestinationType.TOPIC);
  }

  @Test
  void nullDestinationCompletesWithoutRetryStorm() throws Exception {
    Session session = registerSession("msg120-null-destination");
    when(session.findDestination("/msg120/absent", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(null));
    LocalLoopProtocol protocol = loop("msg120-null-destination", Map.of("/msg120/source", "/msg120/absent"));

    CountDownLatch completed = new CountDownLatch(1);
    protocol.sendMessage(event("/msg120/source", message("event"), completed));

    assertTrue(completed.await(5, TimeUnit.SECONDS));
    verify(session, times(1)).findDestination("/msg120/absent", DestinationType.TOPIC);
  }

  @Test
  void closingTwoLoopsClosesEachOwnedSessionExactlyOnce() throws Exception {
    Session first = registerSession("msg120-close-A");
    Session second = registerSession("msg120-close-B");
    LocalLoopProtocol a = loop("msg120-close-A", Map.of());
    LocalLoopProtocol b = loop("msg120-close-B", Map.of());

    a.close();
    a.close();
    b.close();
    b.close();

    verify(sessionManager, times(1)).close(first, false);
    verify(sessionManager, times(1)).close(second, false);
  }

  private Session registerSession(String id) {
    Session session = mock(Session.class);
    sessions.put(id, session);
    return session;
  }

  private LocalLoopProtocol loop(String id, Map<String, String> mappings) throws Exception {
    NoOpEndPoint endpoint = new NoOpEndPoint(NEXT_ENDPOINT.incrementAndGet(), null, List.of());
    LocalLoopProtocol protocol = (LocalLoopProtocol) new LocalLoopProtocolFactory()
        .connect(endpoint, id, "anonymous", "", mappings);
    protocols.add(protocol);
    return protocol;
  }

  private static MessageEvent event(String source, Message message, CountDownLatch completed) {
    SubscribedEventManager subscribed = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContextBuilder(source, ClientAcknowledgement.AUTO).build();
    when(subscribed.getContext()).thenReturn(context);
    return new MessageEvent(source, subscribed, message, completed::countDown);
  }

  private static Message message(String text) {
    return new MessageBuilder().setOpaqueData(bytes(text)).build();
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
