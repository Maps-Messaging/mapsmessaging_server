package io.mapsmessaging.state;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.network.io.EndPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class StateLoopProtocolTest {

  @Test
  void outboundEventIsDeliveredDirectlyToStateHandler() {
    EndPoint endpoint = mock(EndPoint.class);
    when(endpoint.getJMXTypePath()).thenReturn(List.of());
    MessageHandler handler = mock(MessageHandler.class);
    MessageEvent event = mock(MessageEvent.class);

    StateLoopProtocol protocol = new StateLoopProtocol(endpoint, handler);
    protocol.sendMessage(event);

    verify(handler).handle(event);
    verify(endpoint).setBoundProtocol(protocol);
  }
}
