package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.tasks.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DestinationImplTaskFailureCoverageTest {

  @Test
  void cancellationIsRethrownWhileOpen() throws Exception {
    DestinationImpl destination = destinationThrowing(new CancellationException("cancelled"));

    assertThrows(
        CancellationException.class,
        () -> destination.handleTask(mock(Callable.class)));
  }

  @Test
  void cancellationReturnsZeroAfterClose() throws Exception {
    DestinationImpl destination = destinationThrowing(new CancellationException("cancelled"));
    set(destination, "closed", true);

    assertEquals(0, destination.handleTask(mock(Callable.class)));
  }

  @Test
  void interruptionIsWrappedAndInterruptFlagRestored() throws Exception {
    DestinationImpl destination = destinationThrowing(new InterruptedException("stop"));

    try {
      IOException exception = assertThrows(
          IOException.class,
          () -> destination.handleTask(mock(Callable.class)));

      assertEquals("Thread interrupted", exception.getMessage());
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void timeoutCauseIsPreserved() throws Exception {
    IOException root = new IOException("storage timeout");
    TimeoutException timeout = new TimeoutException("timeout");
    timeout.initCause(root);
    DestinationImpl destination = destinationThrowing(timeout);

    IOException exception = assertThrows(
        IOException.class,
        () -> destination.handleTask(mock(Callable.class)));

    assertSame(root, exception.getCause());
  }

  @Test
  void executionFailureIsWrapped() throws Exception {
    ExecutionException failure =
        new ExecutionException(new IllegalStateException("failed"));
    DestinationImpl destination = destinationThrowing(failure);

    IOException exception = assertThrows(
        IOException.class,
        () -> destination.handleTask(mock(Callable.class)));

    assertSame(failure, exception.getCause());
    assertEquals(failure.getMessage(), exception.getMessage());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static DestinationImpl destinationThrowing(Exception failure) throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class, CALLS_REAL_METHODS);
    Future future = mock(Future.class);
    when(future.get(60, TimeUnit.SECONDS)).thenThrow(failure);
    doReturn(future)
        .when(destination)
        .submit(any(Callable.class), eq(DestinationImpl.PUBLISH_PRIORITY));
    return destination;
  }

  private static void set(DestinationImpl destination, String name, Object value) throws Exception {
    Field field = DestinationImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(destination, value);
  }
}
