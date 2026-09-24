/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.amqp.proton.tasks;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.SaslManager;
import io.mapsmessaging.network.protocol.impl.amqp.proton.listeners.EventListenerFactory;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.TransportResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProtonInputLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void consumesAcrossMultipleProtonInputBuffers() throws Exception {
      Harness h = new Harness();
      Packet packet = packet(10);

      h.nextInputBuffers(
          preparedBuffer(4, 0),
          preparedBuffer(4, 0),
          preparedBuffer(4, 0));

      new InputPacketProcessTask(h.engine, packet).call();

      verify(h.transport, atLeast(3)).processInput();
    }
  }

  @Nested
  class SadPath {

    @Test
    void sharedReadPacketMustRemainAtConsumedBoundaryNotBeCleared() throws Exception {
      Harness h = new Harness();
      Packet packet = packet(6);
      int limit = packet.limit();
      h.nextInputBuffers(preparedBuffer(8, 0));

      new InputPacketProcessTask(h.engine, packet).call();

      assertEquals(limit, packet.position());
      assertEquals(limit, packet.limit(),
          "protocol processing must not clear the shared ReadTask packet");
    }

    @Test
    void usesWritableRemainingNotBufferCapacity() throws Exception {
      Harness h = new Harness();
      Packet packet = packet(3);
      ByteBuffer input = preparedBuffer(8, 7);
      h.nextInputBuffers(input);

      assertDoesNotThrow(() -> new InputPacketProcessTask(h.engine, packet).call());
      assertEquals(8, input.position(),
          "only one byte can be appended because only one byte remains writable");
      assertEquals(2, packet.available(),
          "unconsumed input must remain in the source packet for the next Proton input buffer");
    }
  }

  @Nested
  class Murphy {

    @Test
    void eachProcessInputMayReturnDifferentInputBufferInstance() throws Exception {
      Harness h = new Harness();
      Packet packet = packet(9);

      h.nextInputBuffers(
          preparedBuffer(4, 0),
          preparedBuffer(7, 3),
          preparedBuffer(10, 8));

      new InputPacketProcessTask(h.engine, packet).call();

      assertEquals(packet.limit(), packet.position());
      verify(h.transport, atLeast(3)).getInputBuffer();
    }
  }

  private static final class Harness {
    private final ProtonEngine engine = mock(ProtonEngine.class);
    private final Transport transport = mock(Transport.class);
    private final AMQPProtocol protocol = mock(AMQPProtocol.class);
    private final SaslManager sasl = mock(SaslManager.class);
    private final Collector collector = mock(Collector.class);
    private final Connection connection = mock(Connection.class);
    private final EventListenerFactory listeners = mock(EventListenerFactory.class);

    private Harness() {
      when(engine.getTransport()).thenReturn(transport);
      when(engine.getProtocol()).thenReturn(protocol);
      when(engine.getSaslManager()).thenReturn(sasl);
      when(engine.getCollector()).thenReturn(collector);
      when(engine.getConnection()).thenReturn(connection);
      when(engine.getEventListenerFactory()).thenReturn(listeners);
      when(sasl.isDone()).thenReturn(true);
      when(collector.peek()).thenReturn(null);

      TransportResult result = mock(TransportResult.class);
      when(result.isOk()).thenReturn(true);
      when(transport.processInput()).thenReturn(result);
      when(transport.pending()).thenReturn(0);
    }

    private void nextInputBuffers(ByteBuffer... buffers) {
      when(transport.getInputBuffer()).thenReturn(
          buffers[0],
          java.util.Arrays.copyOfRange(buffers, 1, buffers.length));
    }
  }

  private static Packet packet(int size) {
    byte[] bytes = new byte[size];
    for (int i = 0; i < size; i++) {
      bytes[i] = (byte) i;
    }
    return new Packet(ByteBuffer.wrap(bytes));
  }

  private static ByteBuffer preparedBuffer(int capacity, int position) {
    ByteBuffer buffer = ByteBuffer.allocate(capacity);
    buffer.position(position);
    return buffer;
  }
}
