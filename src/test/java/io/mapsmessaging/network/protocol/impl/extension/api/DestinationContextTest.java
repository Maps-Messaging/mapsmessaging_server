package io.mapsmessaging.network.protocol.impl.extension.api;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.selector.operators.ParserExecutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DestinationContextTest {

  @Test
  void nullParserStoresMessageDirectly() throws Exception {
    Destination destination = mock(Destination.class);
    Message message = mock(Message.class);
    when(destination.storeMessage(message)).thenReturn(7);

    DestinationContext context = new DestinationContext(destination);

    assertEquals(7, context.writeEvent(message, null));
    verify(destination).storeMessage(message);
  }

  @Test
  void matchingParserStoresMessage() throws Exception {
    Destination destination = mock(Destination.class);
    Message message = mock(Message.class);
    ParserExecutor parser = mock(ParserExecutor.class);

    when(parser.evaluate(message)).thenReturn(true);
    when(destination.storeMessage(message)).thenReturn(3);

    DestinationContext context = new DestinationContext(destination);

    assertEquals(3, context.writeEvent(message, parser));
    verify(parser).evaluate(message);
    verify(destination).storeMessage(message);
  }

  @Test
  void nonMatchingParserSuppressesMessage() throws Exception {
    Destination destination = mock(Destination.class);
    Message message = mock(Message.class);
    ParserExecutor parser = mock(ParserExecutor.class);

    when(parser.evaluate(message)).thenReturn(false);

    DestinationContext context = new DestinationContext(destination);

    assertEquals(0, context.writeEvent(message, parser));
    verify(parser).evaluate(message);
    verifyNoInteractions(destination);
  }
}
