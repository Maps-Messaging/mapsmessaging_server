package io.mapsmessaging.rest.api.impl.destination;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class FqnListenerRegistryTest {

  @Test
  void subscribedListenerReceivesMatchingPathChanges() {
    FqnListenerRegistry<String> registry = new FqnListenerRegistry<>();
    @SuppressWarnings("unchecked")
    FqnListener<String> listener = mock(FqnListener.class);

    registry.subscribe("/a/b", listener);
    registry.notifyIfRegistered("/a/b", "value", ChangeType.ADD);

    verify(listener).onChange("/a/b", "value", ChangeType.ADD);
  }

  @Test
  void listenersAreScopedByExactPath() {
    FqnListenerRegistry<String> registry = new FqnListenerRegistry<>();
    @SuppressWarnings("unchecked")
    FqnListener<String> listener = mock(FqnListener.class);

    registry.subscribe("/a/b", listener);
    registry.notifyIfRegistered("/a", "value", ChangeType.UPDATE);
    registry.notifyIfRegistered("/a/b/c", "value", ChangeType.UPDATE);

    verifyNoInteractions(listener);
  }

  @Test
  void unsubscribeRemovesListenerAndEmptyPathEntry() {
    FqnListenerRegistry<String> registry = new FqnListenerRegistry<>();
    @SuppressWarnings("unchecked")
    FqnListener<String> first = mock(FqnListener.class);
    @SuppressWarnings("unchecked")
    FqnListener<String> second = mock(FqnListener.class);

    registry.subscribe("/a", first);
    registry.subscribe("/a", second);

    registry.unsubscribe("/a", first);
    registry.notifyIfRegistered("/a", "one", ChangeType.REMOVE);
    verifyNoInteractions(first);
    verify(second).onChange("/a", "one", ChangeType.REMOVE);

    registry.unsubscribe("/a", second);
    registry.notifyIfRegistered("/a", "two", ChangeType.REMOVE);
    verifyNoMoreInteractions(second);

    registry.unsubscribe("/missing", first);
  }
}
