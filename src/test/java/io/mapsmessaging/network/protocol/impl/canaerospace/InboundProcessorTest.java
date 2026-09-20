package io.mapsmessaging.network.protocol.impl.canaerospace;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InboundProcessorTest {

  @Test
  void runProcessesPacketsUntilProcessorIsClosed() throws Exception {
    CanaerospaceProtocol protocol = mock(CanaerospaceProtocol.class);
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
    CanaerospaceProtocol protocol = mock(CanaerospaceProtocol.class);
    InboundProcessor processor = new InboundProcessor(protocol);

    processor.close();
    processor.run();

    verifyNoInteractions(protocol);
  }
}
