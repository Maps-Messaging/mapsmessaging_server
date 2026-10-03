package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.destination.delayed.DelayedMessageManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DestinationImplDelayedProcessingCoverageTest {

  @Test
  void doesNothingWithoutDelayManager() throws Exception {
    DestinationImpl destination = destination(null);

    destination.processDelayedEvents();

    verify(destination, never()).submit(any(Callable.class));
  }

  @Test
  void doesNothingWhenDelayManagerIsEmpty() throws Exception {
    DelayedMessageManager manager = mock(DelayedMessageManager.class);
    when(manager.isEmpty()).thenReturn(true);
    DestinationImpl destination = destination(manager);

    destination.processDelayedEvents();

    verify(manager, never()).getBucketIds();
    verify(destination, never()).submit(any(Callable.class));
  }

  @Test
  void submitsEveryExpiredBucket() throws Exception {
    long now = System.currentTimeMillis();
    DelayedMessageManager manager = activeManager(
        List.of(now - 3000, now - 2000, now - 1000));
    DestinationImpl destination = destination(manager);

    destination.processDelayedEvents();

    verify(destination, times(3)).submit(any(Callable.class));
  }

  @Test
  void stopsAtFirstFutureBucket() throws Exception {
    long now = System.currentTimeMillis();
    DelayedMessageManager manager = activeManager(
        List.of(now + 60_000, now + 120_000));
    DestinationImpl destination = destination(manager);

    destination.processDelayedEvents();

    verify(destination, never()).submit(any(Callable.class));
  }

  @Test
  void submitsOnlyExpiredPrefix() throws Exception {
    long now = System.currentTimeMillis();
    DelayedMessageManager manager = activeManager(
        List.of(now - 3000, now - 2000, now + 60_000, now + 120_000));
    DestinationImpl destination = destination(manager);

    destination.processDelayedEvents();

    verify(destination, times(2)).submit(any(Callable.class));
  }

  private static DelayedMessageManager activeManager(List<Long> buckets) {
    DelayedMessageManager manager = mock(DelayedMessageManager.class);
    when(manager.isEmpty()).thenReturn(false);
    when(manager.getBucketIds()).thenReturn(buckets);
    return manager;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static DestinationImpl destination(DelayedMessageManager manager) throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class, CALLS_REAL_METHODS);
    set(destination, "delayedMessageManager", manager);
    doReturn(mock(Future.class)).when(destination).submit(any(Callable.class));
    return destination;
  }

  private static void set(DestinationImpl destination, String name, Object value) throws Exception {
    Field field = DestinationImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(destination, value);
  }
}
