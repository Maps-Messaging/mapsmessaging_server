/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.aggregator;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorContributionMode;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StreamHandlerBranchCoverageTest {

  @Test
  void exposesConfiguredTopicAndContributionMode() {
    AggregatorInputConfigDTO config = config("/sensor/+/value", null);
    config.setContributionMode(AggregatorContributionMode.FIRST);
    StreamHandler handler = new StreamHandler(config);

    assertEquals("/sensor/+/value", handler.getTopicName());
    assertEquals(AggregatorContributionMode.FIRST, handler.getContributionMode());
  }

  @Test
  void startAddsAutoAcknowledgedSubscriptionWithoutSelector() throws Exception {
    Session session = mock(Session.class);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    when(session.addSubscription(any())).thenReturn(subscription);
    StreamHandler handler = new StreamHandler(config("/sensor/+/value", null));

    handler.start(session);

    ArgumentCaptor<SubscriptionContext> captor = ArgumentCaptor.forClass(SubscriptionContext.class);
    verify(session).addSubscription(captor.capture());
    SubscriptionContext context = captor.getValue();
    assertEquals("/sensor/+/value", context.getDestinationName());
    assertEquals(ClientAcknowledgement.AUTO, context.getAcknowledgementController());
    assertNull(context.getSelector());
  }

  @Test
  void startCopiesSelectorIntoSubscription() throws Exception {
    Session session = mock(Session.class);
    when(session.addSubscription(any())).thenReturn(mock(SubscribedEventManager.class));
    StreamHandler handler = new StreamHandler(config("/sensor/value", "speed > 10"));

    handler.start(session);

    ArgumentCaptor<SubscriptionContext> captor = ArgumentCaptor.forClass(SubscriptionContext.class);
    verify(session).addSubscription(captor.capture());
    assertEquals("speed > 10", captor.getValue().getSelector());
  }

  @Test
  void stopBeforeStartDoesNotRemoveSubscription() {
    Session session = mock(Session.class);
    StreamHandler handler = new StreamHandler(config("/sensor/value", null));

    handler.stop(session);

    verifyNoInteractions(session);
  }

  @Test
  void stopAfterStartRemovesSubscriptionAndClearsTransformations() throws Exception {
    Session session = mock(Session.class);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("/sensor/value");
    when(subscription.getContext()).thenReturn(context);
    when(session.addSubscription(any())).thenReturn(subscription);
    StreamHandler handler = new StreamHandler(config("/sensor/value", null));
    transformations(handler).add(mock(InterServerTransformation.class));

    handler.start(session);
    handler.stop(session);

    verify(session).removeSubscription(context.getKey());
    assertTrue(transformations(handler).isEmpty());
  }

  @Test
  void processWithoutTransformationsPreservesEventPayloadAndCompletion() {
    StreamHandler handler = new StreamHandler(config("/sensor/value", null));
    Message message = new MessageBuilder().setOpaqueData(new byte[]{1, 2, 3}).build();
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    Runnable completion = mock(Runnable.class);
    MessageEvent input = new MessageEvent("/sensor/value", subscription, message, completion);

    MessageEvent output = handler.process(input);

    assertNotSame(input, output);
    assertEquals("/sensor/value", output.getDestinationName());
    assertSame(subscription, output.getSubscription());
    assertSame(message, output.getMessage());
    assertSame(completion, output.getCompletionTask());
  }

  @Test
  void nullTransformationResultDropsEvent() throws Exception {
    StreamHandler handler = new StreamHandler(config("/sensor/value", null));
    InterServerTransformation transformation = mock(InterServerTransformation.class);
    when(transformation.transform(any(), any())).thenReturn(null);
    transformations(handler).add(transformation);

    assertNull(handler.process(event("/sensor/value", new MessageBuilder().build())));
  }

  @Test
  void transformationWithNullDestinationDropsEvent() throws Exception {
    StreamHandler handler = new StreamHandler(config("/sensor/value", null));
    InterServerTransformation transformation = mock(InterServerTransformation.class);
    ParsedMessage transformed = new ParsedMessage(null, new MessageBuilder().build());
    when(transformation.transform(any(), any())).thenReturn(transformed);
    transformations(handler).add(transformation);

    assertNull(handler.process(event("/sensor/value", new MessageBuilder().build())));
  }

  @Test
  void transformationWithNullMessageDropsEvent() throws Exception {
    StreamHandler handler = new StreamHandler(config("/sensor/value", null));
    InterServerTransformation transformation = mock(InterServerTransformation.class);
    ParsedMessage transformed = new ParsedMessage("/rewritten", null);
    when(transformation.transform(any(), any())).thenReturn(transformed);
    transformations(handler).add(transformation);

    assertNull(handler.process(event("/sensor/value", new MessageBuilder().build())));
  }

  @Test
  void transformationCanRewriteDestinationAndMessage() throws Exception {
    StreamHandler handler = new StreamHandler(config("/sensor/value", null));
    InterServerTransformation transformation = mock(InterServerTransformation.class);
    Message original = new MessageBuilder().setOpaqueData(new byte[]{1}).build();
    Message rewritten = new MessageBuilder().setOpaqueData(new byte[]{2}).build();
    ParsedMessage transformed = new ParsedMessage("/rewritten", rewritten);
    when(transformation.transform(eq("/sensor/value"), any())).thenReturn(transformed);
    transformations(handler).add(transformation);

    MessageEvent output = handler.process(event("/sensor/value", original));

    assertNotNull(output);
    assertEquals("/rewritten", output.getDestinationName());
    assertSame(rewritten, output.getMessage());
  }

  @Test
  void transformationChainReceivesPreviousTransformedMessage() throws Exception {
    StreamHandler handler = new StreamHandler(config("/sensor/value", null));
    InterServerTransformation first = mock(InterServerTransformation.class);
    InterServerTransformation second = mock(InterServerTransformation.class);
    Message original = new MessageBuilder().setOpaqueData(new byte[]{1}).build();
    Message intermediate = new MessageBuilder().setOpaqueData(new byte[]{2}).build();
    Message finalMessage = new MessageBuilder().setOpaqueData(new byte[]{3}).build();
    ParsedMessage firstResult = new ParsedMessage("/intermediate", intermediate);
    ParsedMessage secondResult = new ParsedMessage("/final", finalMessage);
    when(first.transform(eq("/sensor/value"), any())).thenReturn(firstResult);
    when(second.transform("/sensor/value", firstResult)).thenReturn(secondResult);
    transformations(handler).add(first);
    transformations(handler).add(second);

    MessageEvent output = handler.process(event("/sensor/value", original));

    assertNotNull(output);
    assertEquals("/final", output.getDestinationName());
    assertSame(finalMessage, output.getMessage());
    verify(second).transform("/sensor/value", firstResult);
  }

  @Test
  void startPropagatesSubscriptionFailure() throws Exception {
    Session session = mock(Session.class);
    java.io.IOException failure = new java.io.IOException("subscription failed");
    when(session.addSubscription(any())).thenThrow(failure);
    StreamHandler handler = new StreamHandler(config("/sensor/value", null));

    java.io.IOException thrown = assertThrows(java.io.IOException.class, () -> handler.start(session));

    assertSame(failure, thrown);
  }

  private MessageEvent event(String destination, Message message) {
    return new MessageEvent(destination, mock(SubscribedEventManager.class), message, mock(Runnable.class));
  }

  @SuppressWarnings("unchecked")
  private List<InterServerTransformation> transformations(StreamHandler handler) throws Exception {
    Field field = StreamHandler.class.getDeclaredField("transformation");
    field.setAccessible(true);
    return (List<InterServerTransformation>) field.get(handler);
  }

  private AggregatorInputConfigDTO config(String topic, String selector) {
    AggregatorInputConfigDTO config = new AggregatorInputConfigDTO();
    config.setTopicName(topic);
    config.setSelector(selector);
    return config;
  }
}
