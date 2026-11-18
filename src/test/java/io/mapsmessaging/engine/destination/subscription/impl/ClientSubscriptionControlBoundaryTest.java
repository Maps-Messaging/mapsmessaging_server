/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.tasks.CreditUpdateTask;
import io.mapsmessaging.engine.destination.subscription.tasks.SubscriptionTransactionTask;
import io.mapsmessaging.engine.tasks.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientSubscriptionControlBoundaryTest {

  @Test
  void ackIsDispatchedToEngineQueueAndNotExecutedInline() throws Exception {
    Fixture fixture = fixture();
    fixture.wrapper.ackReceived(17L);

    Callable<Response> task = fixture.captured();
    assertInstanceOf(SubscriptionTransactionTask.class, task);
    verify(fixture.target, never()).ackReceived(anyLong());
    task.call();
    verify(fixture.target).ackReceived(17L);
  }

  @Test
  void rollbackIsDispatchedToEngineQueueAndNotExecutedInline() throws Exception {
    Fixture fixture = fixture();
    fixture.wrapper.rollbackReceived(18L);

    Callable<Response> task = fixture.captured();
    assertInstanceOf(SubscriptionTransactionTask.class, task);
    verify(fixture.target, never()).rollbackReceived(anyLong());
    task.call();
    verify(fixture.target).rollbackReceived(18L);
  }

  @Test
  void creditUpdateIsSerializedOnEngineQueue() throws Exception {
    Fixture fixture = fixture();
    fixture.wrapper.updateCredit(5);

    Callable<Response> task = fixture.captured();
    assertInstanceOf(CreditUpdateTask.class, task);
    verify(fixture.target, never()).updateCredit(anyInt());
    task.call();
    verify(fixture.target).updateCredit(5);
  }

  @Test
  void wrapperDelegatesPendingDepthAndEmptyState() {
    Fixture fixture = fixture();
    when(fixture.target.getPending()).thenReturn(2);
    when(fixture.target.getDepth()).thenReturn(3);
    when(fixture.target.isEmpty()).thenReturn(false);

    assertEquals(2, fixture.wrapper.getPending());
    assertEquals(3, fixture.wrapper.getDepth());
    assertFalse(fixture.wrapper.isEmpty());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Fixture fixture() {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscribedEventManager target = mock(SubscribedEventManager.class);
    return new Fixture(destination, target, new ClientSubscribedEventManager(destination, target));
  }

  private record Fixture(DestinationImpl destination, SubscribedEventManager target,
      ClientSubscribedEventManager wrapper) {

    @SuppressWarnings({"unchecked", "rawtypes"})
    Callable<Response> captured() {
      ArgumentCaptor<Callable> captor = ArgumentCaptor.forClass(Callable.class);
      verify(destination).submit(captor.capture());
      return (Callable<Response>) captor.getValue();
    }
  }
}
