/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.n2k;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InboundProcessorRecoveryCoverageTest {

  @Test
  void processorRecoversAfterIoFailureAndProcessesNextPacket() throws Exception {
    N2kProtocol protocol = mock(N2kProtocol.class);
    AtomicReference<InboundProcessor> processorRef = new AtomicReference<>();
    AtomicInteger calls = new AtomicInteger();

    when(protocol.processPacket(any())).thenAnswer(invocation -> {
      if (calls.getAndIncrement() == 0) {
        throw new IOException("transient");
      }
      processorRef.get().close();
      return true;
    });

    InboundProcessor processor = new InboundProcessor(protocol);
    processorRef.set(processor);

    processor.run();

    verify(protocol, times(2)).processPacket(any());
  }

  @Test
  void processorRecoversAfterRuntimeFailureAndProcessesNextPacket() throws Exception {
    N2kProtocol protocol = mock(N2kProtocol.class);
    AtomicReference<InboundProcessor> processorRef = new AtomicReference<>();
    AtomicInteger calls = new AtomicInteger();

    when(protocol.processPacket(any())).thenAnswer(invocation -> {
      if (calls.getAndIncrement() == 0) {
        throw new IllegalStateException("runtime");
      }
      processorRef.get().close();
      return true;
    });

    InboundProcessor processor = new InboundProcessor(protocol);
    processorRef.set(processor);

    processor.run();

    verify(protocol, times(2)).processPacket(any());
  }

  @Test
  void processorContainsErrorsAndCanStillBeClosed() throws Exception {
    N2kProtocol protocol = mock(N2kProtocol.class);
    AtomicReference<InboundProcessor> processorRef = new AtomicReference<>();
    AtomicInteger calls = new AtomicInteger();

    when(protocol.processPacket(any())).thenAnswer(invocation -> {
      if (calls.getAndIncrement() == 0) {
        throw new AssertionError("error");
      }
      processorRef.get().close();
      return true;
    });

    InboundProcessor processor = new InboundProcessor(protocol);
    processorRef.set(processor);

    processor.run();

    verify(protocol, times(2)).processPacket(any());
  }

  @Test
  void interruptedRecoveryRestoresInterruptFlagAndStopsCleanly() throws Exception {
    N2kProtocol protocol = mock(N2kProtocol.class);
    AtomicReference<InboundProcessor> processorRef = new AtomicReference<>();
    AtomicBoolean interruptedAfterRun = new AtomicBoolean();

    when(protocol.processPacket(any())).thenAnswer(invocation -> {
      processorRef.get().close();
      throw new IOException("stop");
    });

    InboundProcessor processor = new InboundProcessor(protocol);
    processorRef.set(processor);

    Thread worker = new Thread(() -> {
      Thread.currentThread().interrupt();
      processor.run();
      interruptedAfterRun.set(Thread.currentThread().isInterrupted());
    });
    worker.start();
    worker.join(2_000);

    assertFalse(worker.isAlive());
    assertTrue(interruptedAfterRun.get());
    verify(protocol, times(1)).processPacket(any());
  }
}
