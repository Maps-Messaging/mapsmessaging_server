/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.destination.delayed.TransactionalMessageManager;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.destination.tasks.BulkRemoveMessageTask;
import io.mapsmessaging.engine.destination.tasks.TransactionalMessageProcessor;
import io.mapsmessaging.engine.tasks.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EngineTransactionSchedulingTest {

  @Test
  void commitQueuesTransactionProcessorWithoutProcessingBeforeScheduledExecution() throws Exception {
    DestinationImpl destination = fixture();
    destination.commit(50L);

    ArgumentCaptor<Callable<Response>> task = taskCaptor();
    verify(destination).submit(task.capture());
    assertInstanceOf(TransactionalMessageProcessor.class, task.getValue());
  }

  @Test
  void abortQueuesBulkRemovalAtDeletePriority() throws Exception {
    DestinationImpl destination = fixture();
    destination.abort(50L);

    ArgumentCaptor<Callable<Response>> task = taskCaptor();
    verify(destination).submit(task.capture(), eq(DestinationImpl.DELETE_PRIORITY));
    assertInstanceOf(BulkRemoveMessageTask.class, task.getValue());
  }

  @Test
  void abortUnknownTransactionStillUsesSafeEmptyRemovalTask() throws Exception {
    DestinationImpl destination = fixture();
    destination.abort(999L);

    ArgumentCaptor<Callable<Response>> task = taskCaptor();
    verify(destination).submit(task.capture(), eq(DestinationImpl.DELETE_PRIORITY));
    assertInstanceOf(BulkRemoveMessageTask.class, task.getValue());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static ArgumentCaptor<Callable<Response>> taskCaptor() {
    return (ArgumentCaptor) ArgumentCaptor.forClass(Callable.class);
  }

  private static DestinationImpl fixture() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class, CALLS_REAL_METHODS);
    set(destination, "transactionMessageManager", mock(TransactionalMessageManager.class));
    set(destination, "subscriptionManager", mock(DestinationSubscriptionManager.class));
    return destination;
  }

  private static void set(Object target, String field, Object value) throws Exception {
    Field declared = DestinationImpl.class.getDeclaredField(field);
    declared.setAccessible(true);
    declared.set(target, value);
  }
}
