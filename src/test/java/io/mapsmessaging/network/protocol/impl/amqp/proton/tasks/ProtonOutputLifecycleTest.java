/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.amqp.proton.tasks;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.engine.Transport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProtonOutputLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void outputIsConsumedAfterAllBytesAreWritten() throws Exception {
      ProtonEngine engine = mock(ProtonEngine.class);
      AMQPProtocol protocol = mock(AMQPProtocol.class);
      EndPoint endPoint = mock(EndPoint.class);
      Transport transport = mock(Transport.class);
      ByteBuffer output = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});

      when(engine.getProtocol()).thenReturn(protocol);
      when(engine.getTransport()).thenReturn(transport);
      when(protocol.getEndPoint()).thenReturn(endPoint);
      when(transport.pending()).thenReturn(4, 0);
      when(transport.getOutputBuffer()).thenReturn(output);
      when(endPoint.sendPacket(any())).thenAnswer(invocation -> {
        var packet = invocation.getArgument(0, io.mapsmessaging.network.io.Packet.class);
        int count = packet.available();
        packet.position(packet.limit());
        return count;
      });

      TestPacketTask task = new TestPacketTask(engine);
      task.runOutput();

      verify(transport).outputConsumed();
      assertFalse(output.hasRemaining());
    }
  }

  @Nested
  class Murphy {

    @Test
    void zeroProgressWriteMustNotBusySpin() throws Exception {
      ProtonEngine engine = mock(ProtonEngine.class);
      AMQPProtocol protocol = mock(AMQPProtocol.class);
      EndPoint endPoint = mock(EndPoint.class);
      Transport transport = mock(Transport.class);
      ByteBuffer output = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});

      when(engine.getProtocol()).thenReturn(protocol);
      when(engine.getTransport()).thenReturn(transport);
      when(protocol.getEndPoint()).thenReturn(endPoint);
      when(transport.pending()).thenReturn(4);
      when(transport.getOutputBuffer()).thenReturn(output);
      when(endPoint.sendPacket(any())).thenReturn(0);

      TestPacketTask task = new TestPacketTask(engine);

      assertTimeoutPreemptively(Duration.ofMillis(250), task::runOutput,
          "zero-progress nonblocking write must return control instead of spinning forever");
      verify(endPoint, atMost(2)).sendPacket(any());
      verify(transport, never()).outputConsumed();
    }
  }

  private static final class TestPacketTask extends PacketTask {
    private TestPacketTask(ProtonEngine engine) {
      super(engine);
    }

    private void runOutput() throws Exception {
      processOutput();
    }

    @Override
    public Boolean call() {
      return true;
    }
  }
}
