package io.mapsmessaging.api;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.session.security.SecurityContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetricsTest {

  @Test
  void metricsDestinationNeverStoresPublishedMessages() throws Exception {
    DestinationImpl destination = destination();
    Metrics metrics = new Metrics(destination, mock(SecurityContext.class));

    assertEquals(0, metrics.storeMessage(mock(Message.class)));
    assertEquals(0L, metrics.getStoredMessages());

    verify(destination, never()).storeMessage(any());
  }

  private static DestinationImpl destination() {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getResourceType()).thenReturn(DestinationType.TOPIC);
    when(destination.getFullyQualifiedNamespace()).thenReturn("/metrics");
    return destination;
  }
}
