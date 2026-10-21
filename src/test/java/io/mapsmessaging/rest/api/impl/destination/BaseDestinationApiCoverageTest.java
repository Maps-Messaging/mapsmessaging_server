/*
 *
 * Copyright [ 2020 - 2024 ] Matthew Buckton
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */

package io.mapsmessaging.rest.api.impl.destination;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseDestinationApiCoverageTest {

  @Test
  void schemaAndSystemStyleNamesAreRejectedBeforeLookup() throws Exception {
    BaseDestinationApi api = new BaseDestinationApi();

    assertNull(api.lookup("$schema/test"));
    assertNull(api.lookup("$SYS/test"));
  }

  @Test
  void absoluteNameIsLookedUpOnce() throws Exception {
    BaseDestinationApi api = new BaseDestinationApi();
    MessageDaemon daemon = mock(MessageDaemon.class);
    DestinationManager manager = mock(DestinationManager.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(daemon.getDestinationManager()).thenReturn(manager);
    when(manager.find("/topic")).thenReturn(CompletableFuture.completedFuture(destination));

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      assertSame(destination, api.lookup("/topic"));

      verify(manager).find("/topic");
      verifyNoMoreInteractions(manager);
    }
  }

  @Test
  void relativeNameFallsBackToAbsoluteNameWhenFirstLookupMisses() throws Exception {
    BaseDestinationApi api = new BaseDestinationApi();
    MessageDaemon daemon = mock(MessageDaemon.class);
    DestinationManager manager = mock(DestinationManager.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(daemon.getDestinationManager()).thenReturn(manager);
    when(manager.find("topic")).thenReturn(CompletableFuture.completedFuture(null));
    when(manager.find("/topic")).thenReturn(CompletableFuture.completedFuture(destination));

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      assertSame(destination, api.lookup("topic"));

      verify(manager).find("topic");
      verify(manager).find("/topic");
    }
  }

  @Test
  void relativeNameReturnsInitialMatchWithoutSecondLookup() throws Exception {
    BaseDestinationApi api = new BaseDestinationApi();
    MessageDaemon daemon = mock(MessageDaemon.class);
    DestinationManager manager = mock(DestinationManager.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(daemon.getDestinationManager()).thenReturn(manager);
    when(manager.find("topic")).thenReturn(CompletableFuture.completedFuture(destination));

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      assertSame(destination, api.lookup("topic"));

      verify(manager).find("topic");
      verify(manager, never()).find("/topic");
    }
  }

  @Test
  void relativeNameReturnsNullWhenBothLookupsMiss() throws Exception {
    BaseDestinationApi api = new BaseDestinationApi();
    MessageDaemon daemon = mock(MessageDaemon.class);
    DestinationManager manager = mock(DestinationManager.class);
    when(daemon.getDestinationManager()).thenReturn(manager);
    when(manager.find(anyString())).thenReturn(CompletableFuture.completedFuture(null));

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      assertNull(api.lookup("topic"));

      verify(manager).find("topic");
      verify(manager).find("/topic");
    }
  }
}
