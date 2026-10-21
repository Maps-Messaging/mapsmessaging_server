package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.delayed.DelayedMessageManager;
import io.mapsmessaging.engine.destination.delayed.TransactionalMessageManager;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.destination.tasks.DelayedStoreMessageTask;
import io.mapsmessaging.engine.destination.tasks.NonDelayedStoreMessageTask;
import io.mapsmessaging.engine.destination.tasks.QueueBasedStoreMessageTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DestinationImplStoreTaskCoverageTest {

  @Test
  void normalStoreRejectsBoundMessage() throws Exception {
    Fixture fixture = new Fixture();
    Message message = message(true, 0);

    assertThrows(IOException.class, () -> fixture.destination.storeMessage(message));
    verify(fixture.destination, never()).handleTask(any());
  }

  @Test
  void delayedStoreUsesDelayedTask() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("delayedMessageManager", mock(DelayedMessageManager.class));
    fixture.set("destinationType", DestinationType.TOPIC);

    assertEquals(3, fixture.destination.storeMessage(message(false, 5000)));
    fixture.assertTask(DelayedStoreMessageTask.class);
  }

  @Test
  void delayedMessageFallsBackWhenManagerMissing() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("delayedMessageManager", null);
    fixture.set("destinationType", DestinationType.TOPIC);

    assertEquals(3, fixture.destination.storeMessage(message(false, 5000)));
    fixture.assertTask(NonDelayedStoreMessageTask.class);
  }

  @Test
  void immediateTopicUsesNonDelayedTask() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("delayedMessageManager", mock(DelayedMessageManager.class));
    fixture.set("destinationType", DestinationType.TOPIC);

    assertEquals(3, fixture.destination.storeMessage(message(false, 0)));
    fixture.assertTask(NonDelayedStoreMessageTask.class);
  }

  @Test
  void immediateQueueUsesQueueTask() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("delayedMessageManager", null);
    fixture.set("destinationType", DestinationType.QUEUE);

    assertEquals(3, fixture.destination.storeMessage(message(false, 0)));
    fixture.assertTask(QueueBasedStoreMessageTask.class);
  }

  @Test
  void transactionalStoreRejectsBoundMessage() throws Exception {
    Fixture fixture = new Fixture();

    assertThrows(
        IOException.class,
        () -> fixture.destination.storeTransactionalMessage(91, message(true, 0)));
    verify(fixture.destination, never()).handleTask(any());
  }

  @Test
  void transactionalStoreUsesTransactionManager() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("transactionMessageManager", mock(TransactionalMessageManager.class));

    fixture.destination.storeTransactionalMessage(92, message(false, 0));

    fixture.assertTask(DelayedStoreMessageTask.class);
  }

  @Test
  void transactionalStoreFallsBackWhenManagerMissing() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("transactionMessageManager", null);

    fixture.destination.storeTransactionalMessage(93, message(false, 0));

    fixture.assertTask(NonDelayedStoreMessageTask.class);
  }

  private static Message message(boolean bound, long delayed) {
    Message message = mock(Message.class);
    when(message.isBound()).thenReturn(bound);
    when(message.getDelayed()).thenReturn(delayed);
    return message;
  }

  private static final class Fixture {
    private final DestinationImpl destination =
        mock(DestinationImpl.class, CALLS_REAL_METHODS);

    private Fixture() throws Exception {
      set("subscriptionManager", mock(DestinationSubscriptionManager.class));
      set("messageOverrides", null);
      doReturn(3).when(destination).handleTask(any());
    }

    private void set(String name, Object value) throws Exception {
      Field field = DestinationImpl.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(destination, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertTask(Class<?> expected) throws Exception {
      ArgumentCaptor<Callable> captor = ArgumentCaptor.forClass(Callable.class);
      verify(destination).handleTask(captor.capture());
      assertInstanceOf(expected, captor.getValue());
    }
  }
}
