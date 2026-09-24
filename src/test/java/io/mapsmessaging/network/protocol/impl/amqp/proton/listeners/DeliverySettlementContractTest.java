/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Sender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class DeliverySettlementContractTest {

  @Nested
  class HappyPath {

    @Test
    void acceptedRemoteDispositionAcknowledgesMapsMessage() {
      AMQPProtocol protocol = mock(AMQPProtocol.class);
      ProtonEngine engine = mock(ProtonEngine.class);
      DeliveryEventListener listener = new DeliveryEventListener(protocol, engine);

      Event event = mock(Event.class);
      Delivery delivery = mock(Delivery.class);
      Sender sender = mock(Sender.class);
      SubscribedEventManager manager = mock(SubscribedEventManager.class);

      when(event.getDelivery()).thenReturn(delivery);
      when(event.getLink()).thenReturn(sender);
      when(delivery.getLink()).thenReturn(sender);
      when(delivery.getRemoteState()).thenReturn(Accepted.getInstance());
      when(delivery.getTag()).thenReturn(longTag(42L));
      when(delivery.getContext()).thenReturn(manager);

      listener.handleEvent(event);

      verify(manager).ackReceived(42L);
      verify(delivery, atLeastOnce()).settle();
    }
  }

  @Nested
  class SadPath {

    @Test
    void releasedRemoteDispositionMustNotAcknowledgeMapsMessage() {
      AMQPProtocol protocol = mock(AMQPProtocol.class);
      ProtonEngine engine = mock(ProtonEngine.class);
      DeliveryEventListener listener = new DeliveryEventListener(protocol, engine);

      Event event = mock(Event.class);
      Delivery delivery = mock(Delivery.class);
      Sender sender = mock(Sender.class);
      SubscribedEventManager manager = mock(SubscribedEventManager.class);

      when(event.getDelivery()).thenReturn(delivery);
      when(event.getLink()).thenReturn(sender);
      when(delivery.getLink()).thenReturn(sender);
      when(delivery.getRemoteState()).thenReturn(Released.getInstance());
      when(delivery.getTag()).thenReturn(longTag(43L));
      when(delivery.getContext()).thenReturn(manager);

      listener.handleEvent(event);

      verify(manager, never()).ackReceived(anyLong());
    }

    @Test
    void missingRemoteDispositionMustNotAcknowledgeMapsMessage() {
      AMQPProtocol protocol = mock(AMQPProtocol.class);
      ProtonEngine engine = mock(ProtonEngine.class);
      DeliveryEventListener listener = new DeliveryEventListener(protocol, engine);

      Event event = mock(Event.class);
      Delivery delivery = mock(Delivery.class);
      Sender sender = mock(Sender.class);
      SubscribedEventManager manager = mock(SubscribedEventManager.class);

      when(event.getDelivery()).thenReturn(delivery);
      when(event.getLink()).thenReturn(sender);
      when(delivery.getLink()).thenReturn(sender);
      when(delivery.getRemoteState()).thenReturn(null);
      when(delivery.getTag()).thenReturn(longTag(44L));
      when(delivery.getContext()).thenReturn(manager);

      listener.handleEvent(event);

      verify(manager, never()).ackReceived(anyLong());
    }
  }

  @Nested
  class Murphy {

    @Test
    void duplicateDeliveryEventsAcknowledgeAtMostOnce() {
      AMQPProtocol protocol = mock(AMQPProtocol.class);
      ProtonEngine engine = mock(ProtonEngine.class);
      DeliveryEventListener listener = new DeliveryEventListener(protocol, engine);

      Event event = mock(Event.class);
      Delivery delivery = mock(Delivery.class);
      Sender sender = mock(Sender.class);
      SubscribedEventManager manager = mock(SubscribedEventManager.class);

      when(event.getDelivery()).thenReturn(delivery);
      when(event.getLink()).thenReturn(sender);
      when(delivery.getLink()).thenReturn(sender);
      when(delivery.getRemoteState()).thenReturn(Accepted.getInstance());
      when(delivery.getTag()).thenReturn(longTag(45L));
      when(delivery.getContext()).thenReturn(manager);
      when(delivery.isSettled()).thenReturn(false, true);

      listener.handleEvent(event);
      listener.handleEvent(event);

      verify(manager, atMostOnce()).ackReceived(45L);
    }
  }

  private static byte[] longTag(long value) {
    byte[] tag = new byte[8];
    for (int i = 0; i < tag.length; i++) {
      tag[i] = (byte) ((value >>> (8 * i)) & 0xff);
    }
    return tag;
  }
}
