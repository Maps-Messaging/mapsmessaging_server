package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.tasks.FutureResponse;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DestinationImplTaskResponseCoverageTest {

  @Test
  void returnsDirectLongResponse() throws Exception {
    DestinationImpl destination = destinationWith(new LongResponse(17));

    assertEquals(17, destination.handleTask(mock(Callable.class)));
  }

  @Test
  void returnsNestedLongResponse() throws Exception {
    FutureResponse nested =
        new FutureResponse(CompletableFuture.completedFuture(new LongResponse(23)));
    DestinationImpl destination = destinationWith(nested);

    assertEquals(23, destination.handleTask(mock(Callable.class)));
  }

  @Test
  void returnsZeroForDirectUnknownResponse() throws Exception {
    DestinationImpl destination = destinationWith(mock(Response.class));

    assertEquals(0, destination.handleTask(mock(Callable.class)));
  }

  @Test
  void returnsZeroForNestedUnknownResponse() throws Exception {
    FutureResponse nested =
        new FutureResponse(CompletableFuture.completedFuture(mock(Response.class)));
    DestinationImpl destination = destinationWith(nested);

    assertEquals(0, destination.handleTask(mock(Callable.class)));
  }

  @Test
  void returnsZeroForNullResponse() throws Exception {
    DestinationImpl destination = destinationWith(null);

    assertEquals(0, destination.handleTask(mock(Callable.class)));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static DestinationImpl destinationWith(Response response) throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class, CALLS_REAL_METHODS);
    Future future = mock(Future.class);
    when(future.get(60, TimeUnit.SECONDS)).thenReturn(response);
    doReturn(future)
        .when(destination)
        .submit(any(Callable.class), eq(DestinationImpl.PUBLISH_PRIORITY));
    return destination;
  }
}
