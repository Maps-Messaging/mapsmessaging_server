package io.mapsmessaging.network.protocol.impl.n2k;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InboundProcessorTest {

  @Test
  void runProcessesPacketsUntilProcessorIsClosed() throws Exception {
    N2kProtocol protocol = mock(N2kProtocol.class);
    AtomicReference<InboundProcessor> ref = new AtomicReference<>();
    doAnswer(invocation -> {
      ref.get().close();
      return true;
    }).when(protocol).processPacket(any());

    InboundProcessor processor = new InboundProcessor(protocol);
    ref.set(processor);

    processor.run();

    verify(protocol, times(1)).processPacket(any());
  }

  @Test
  void closeBeforeRunPreventsPacketProcessing() {
    N2kProtocol protocol = mock(N2kProtocol.class);
    InboundProcessor processor = new InboundProcessor(protocol);

    processor.close();
    processor.run();

    verifyNoInteractions(protocol);
  }
}
