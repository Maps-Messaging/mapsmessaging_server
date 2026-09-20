package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.network.protocol.Protocol;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ConnectCompletionTimeoutTest {

  @Test
  void timeoutClosesProtocol() throws Exception {
    Protocol protocol = mock(Protocol.class);

    new ConnectCompletionTimeout(protocol).run();

    verify(protocol).close();
  }

  @Test
  void closeFailureIsContainedByTimeoutTask() throws Exception {
    Protocol protocol = mock(Protocol.class);
    doThrow(new IOException("close failed")).when(protocol).close();

    assertDoesNotThrow(() -> new ConnectCompletionTimeout(protocol).run());
    verify(protocol).close();
  }
}
