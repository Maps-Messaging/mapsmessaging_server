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

class AmqpInputBufferContractTest {

  @Nested
  class HappyPath {

    @Test
    void packetLargerThanProtonInputBufferIsConsumedInChunks() throws Exception {
      Harness harness = new Harness(4);
      Packet packet = packet(10);

      InputPacketProcessTask task = new InputPacketProcessTask(harness.engine, packet);
      task.call();

      verify(harness.transport, atLeast(3)).processInput();
    }
  }

  @Nested
  class SadPath {

    @Test
    void amqpMustNotClearSharedReadPacketAfterConsumption() throws Exception {
      Harness harness = new Harness(4);
      Packet packet = packet(10);
      int expectedLimit = packet.limit();

      InputPacketProcessTask task = new InputPacketProcessTask(harness.engine, packet);
      task.call();

      assertEquals(expectedLimit, packet.limit(),
          "protocol layer must not reset shared ReadTask packet limit");
      assertEquals(expectedLimit, packet.position(),
          "consumed AMQP bytes should leave packet at the consumed boundary for ReadTask to own lifecycle");
    }
  }

  @Nested
  class Murphy {

    @Test
    void exactMultipleOfProtonInputCapacityDoesNotLoseOrRepeatBytes() throws Exception {
      Harness harness = new Harness(4);
      Packet packet = packet(12);

      InputPacketProcessTask task = new InputPacketProcessTask(harness.engine, packet);
      task.call();

      verify(harness.transport, times(3)).processInput();
      assertEquals(12, packet.position(),
          "shared packet must remain consumed, not cleared back to zero");
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

    private Harness(int protonInputCapacity) {
      when(engine.getTransport()).thenReturn(transport);
      when(engine.getProtocol()).thenReturn(protocol);
      when(engine.getSaslManager()).thenReturn(sasl);
      when(engine.getCollector()).thenReturn(collector);
      when(engine.getConnection()).thenReturn(connection);
      when(engine.getEventListenerFactory()).thenReturn(listeners);

      when(transport.getInputBuffer()).thenAnswer(invocation -> ByteBuffer.allocate(protonInputCapacity));
      TransportResult result = mock(TransportResult.class);
      when(result.isOk()).thenReturn(true);
      when(transport.processInput()).thenReturn(result);
      when(transport.pending()).thenReturn(0);
      when(sasl.isDone()).thenReturn(true);
      when(collector.peek()).thenReturn(null);
    }
  }

  private static Packet packet(int length) {
    byte[] data = new byte[length];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) i;
    }
    return new Packet(ByteBuffer.wrap(data));
  }
}
