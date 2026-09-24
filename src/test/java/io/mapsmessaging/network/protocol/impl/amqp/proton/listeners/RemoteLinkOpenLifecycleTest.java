/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemoteLinkOpenLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void receiverRemoteOpenOpensAndTopsUpCredit() {
      AMQPProtocol protocol = mock(AMQPProtocol.class);
      ProtonEngine engine = mock(ProtonEngine.class);
      LinkRemoteOpenEventListener listener = new LinkRemoteOpenEventListener(protocol, engine);
      Event event = mock(Event.class);
      Receiver receiver = mock(Receiver.class);

      when(event.getLink()).thenReturn(receiver);
      when(receiver.getCredit()).thenReturn(0);

      assertTrue(listener.handleEvent(event));

      verify(receiver).open();
      verify(receiver).flow(10);
    }

    @Test
    void senderRemoteOpenMirrorsRemoteTerminiAndRemainsUsable() {
      AMQPProtocol protocol = mock(AMQPProtocol.class);
      ProtonEngine engine = mock(ProtonEngine.class);
      LinkRemoteOpenEventListener listener = new LinkRemoteOpenEventListener(protocol, engine);
      Event event = mock(Event.class);
      Sender sender = mock(Sender.class);
      Source remoteSource = new Source();

      when(event.getLink()).thenReturn(sender);
      when(sender.getSource()).thenReturn(remoteSource);
      when(sender.getRemoteSource()).thenReturn(remoteSource);
      when(sender.getRemoteTarget()).thenReturn(mock(org.apache.qpid.proton.amqp.transport.Target.class));
      when(sender.getLocalState()).thenReturn(EndpointState.ACTIVE);

      assertTrue(listener.handleEvent(event));

      verify(sender).open();
      verify(sender, never()).free();
      verify(sender, never()).advance();
    }
  }

  @Nested
  class Murphy {

    @Test
    void senderWithoutLocalSourceCanConstructDurableSourceWithoutFreeingLink() {
      AMQPProtocol protocol = mock(AMQPProtocol.class);
      ProtonEngine engine = mock(ProtonEngine.class);
      LinkRemoteOpenEventListener listener = new LinkRemoteOpenEventListener(protocol, engine);
      Event event = mock(Event.class);
      Sender sender = mock(Sender.class);

      when(event.getLink()).thenReturn(sender);
      when(sender.getSource()).thenReturn(null);
      when(sender.getName()).thenReturn("orders");
      when(sender.getRemoteTarget()).thenReturn(mock(org.apache.qpid.proton.amqp.transport.Target.class));
      when(sender.getLocalState()).thenReturn(EndpointState.ACTIVE);

      assertTrue(listener.handleEvent(event));

      verify(sender).setSource(any(Source.class));
      verify(sender).open();
      verify(sender, never()).free();
      verify(sender, never()).advance();
    }
  }
}
