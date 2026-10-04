/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static java.nio.channels.SelectionKey.OP_READ;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatsProtocolFailureCoverageTest {

  @Test
  void partialFrameRearmsReadWithoutClosingEndpoint() throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    SelectorTask selectorTask = mock(SelectorTask.class);
    FrameFactory factory = mock(FrameFactory.class);
    Packet packet = mock(Packet.class);
    when(packet.hasRemaining()).thenReturn(true, false);
    when(factory.parseFrame(packet)).thenThrow(new EndOfBufferException());

    NatsProtocol protocol = protocol(endPoint, selectorTask, factory);

    assertFalse(protocol.processPacket(packet));
    verify(selectorTask).register(OP_READ);
    verify(endPoint, never()).close();
  }

  @Test
  void malformedFrameClosesEndpointAndPropagatesIOException() throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    SelectorTask selectorTask = mock(SelectorTask.class);
    FrameFactory factory = mock(FrameFactory.class);
    Packet packet = mock(Packet.class);
    when(packet.hasRemaining()).thenReturn(true);
    when(factory.parseFrame(packet)).thenThrow(new NatsProtocolException("bad frame"));

    NatsProtocol protocol = protocol(endPoint, selectorTask, factory);

    assertThrows(NatsProtocolException.class, () -> protocol.processPacket(packet));
    verify(endPoint).close();
  }

  private static NatsProtocol protocol(
      EndPoint endPoint, SelectorTask selectorTask, FrameFactory factory) throws Exception {
    NatsProtocol protocol = mock(NatsProtocol.class, CALLS_REAL_METHODS);
    set(Protocol.class, protocol, "endPoint", endPoint);
    set(NatsProtocol.class, protocol, "selectorTask", selectorTask);
    set(NatsProtocol.class, protocol, "logger", mock(Logger.class));
    set(NatsProtocol.class, protocol, "factory", factory);
    return protocol;
  }

  private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
