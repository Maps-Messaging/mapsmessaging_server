package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.destination.tasks.RemoveMessageTask;
import io.mapsmessaging.engine.resources.Resource;
import io.mapsmessaging.utilities.queue.EventReaperQueue;
import io.mapsmessaging.utilities.threads.tasks.PriorityTaskScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DestinationImplMessageOperationCoverageTest {

  @Test
  void missingMessageReturnsNullWithoutUpdatingMessageStats() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.resource.get(11)).thenReturn(null);

    assertNull(fixture.destination.getMessage(11));

    verify(fixture.stats, never()).retrievedMessage();
    verify(fixture.stats, never()).expiredMessage();
    verify(fixture.subscriptionManager, never()).expired(anyLong());
  }

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1, 0})
  void nonExpiringMessageIsReturnedAndCounted(long expiry) throws Exception {
    Fixture fixture = new Fixture();
    Message message = message(expiry);
    when(fixture.resource.get(12)).thenReturn(message);

    assertSame(message, fixture.destination.getMessage(12));

    verify(fixture.stats).retrievedMessage();
    verify(fixture.stats, never()).expiredMessage();
    verify(fixture.subscriptionManager, never()).expired(anyLong());
  }

  @Test
  void futureExpiringMessageIsReturnedAndCounted() throws Exception {
    Fixture fixture = new Fixture();
    Message message = message(System.currentTimeMillis() + 60_000);
    when(fixture.resource.get(13)).thenReturn(message);

    assertSame(message, fixture.destination.getMessage(13));

    verify(fixture.stats).retrievedMessage();
    verify(fixture.subscriptionManager, never()).expired(anyLong());
  }

  @ParameterizedTest
  @ValueSource(longs = {1, 10, 1000, 60_000})
  void expiredMessageSchedulesRemovalAndExpiresSubscriptionState(long ageMillis) throws Exception {
    Fixture fixture = new Fixture();
    long id = 100 + ageMillis;
    Message message = message(System.currentTimeMillis() - ageMillis);
    when(fixture.resource.get(id)).thenReturn(message);

    assertNull(fixture.destination.getMessage(id));

    ArgumentCaptor<Callable> captor = ArgumentCaptor.forClass(Callable.class);
    verify(fixture.resourceTaskQueue)
        .submit(captor.capture(), eq(DestinationImpl.RETRIEVE_PRIORITY));
    assertInstanceOf(RemoveMessageTask.class, captor.getValue());
    verify(fixture.subscriptionManager).expired(id);
    verify(fixture.stats).expiredMessage();
    verify(fixture.stats, never()).retrievedMessage();
  }

  @Test
  void resourceReadFailureIsPropagated() throws Exception {
    Fixture fixture = new Fixture();
    IOException failure = new IOException("read failed");
    when(fixture.resource.get(14)).thenThrow(failure);

    assertSame(failure, assertThrows(IOException.class, () -> fixture.destination.getMessage(14)));
  }

  @Test
  void completingRetainedMessageDoesNotQueueRemoval() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.retainManager.current()).thenReturn(21L);

    fixture.destination.complete(21);

    verify(fixture.completionQueue, never()).add(anyLong());
    verify(fixture.stats).deliveredMessage();
  }

  @Test
  void completingNonRetainedMessageQueuesRemoval() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.retainManager.current()).thenReturn(21L);

    fixture.destination.complete(22);

    verify(fixture.completionQueue).add(22);
    verify(fixture.stats).deliveredMessage();
  }

  @Test
  void removeMessageAlwaysRemovesResourceEntry() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.retainManager.current()).thenReturn(-1L);

    fixture.destination.removeMessage(31);

    verify(fixture.resource).remove(31);
    verify(fixture.retainManager, never()).replace(anyLong());
    verify(fixture.stats).messageDeleteTime(anyLong());
  }

  @Test
  void removeRetainedMessageClearsRetainState() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.retainManager.current()).thenReturn(32L);

    fixture.destination.removeMessage(32);

    verify(fixture.resource).remove(32);
    verify(fixture.retainManager).replace(-1);
    verify(fixture.stats).retainedMessages(-1);
  }

  @Test
  void removeDifferentMessageLeavesRetainStateUnchanged() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.retainManager.current()).thenReturn(32L);

    fixture.destination.removeMessage(33);

    verify(fixture.retainManager, never()).replace(anyLong());
    verify(fixture.stats, never()).retainedMessages(anyInt());
  }

  @Test
  void resourceRemoveFailureIsPropagatedBeforeRetainChanges() throws Exception {
    Fixture fixture = new Fixture();
    IOException failure = new IOException("remove failed");
    doThrow(failure).when(fixture.resource).remove(34);

    assertSame(failure, assertThrows(IOException.class, () -> fixture.destination.removeMessage(34)));

    verify(fixture.retainManager, never()).replace(anyLong());
    verify(fixture.stats, never()).messageDeleteTime(anyLong());
  }

  @Test
  void addNonRetainedMessageLeavesRetainStateUnchanged() throws Exception {
    Fixture fixture = new Fixture();
    Message message = mock(Message.class);
    when(message.isRetain()).thenReturn(false);

    fixture.destination.addMessage(message);

    verify(fixture.resource).add(message);
    verify(fixture.retainManager, never()).replace(anyLong());
    verify(fixture.stats).messageWriteTime(anyLong());
  }

  @Test
  void addRetainedMessageWithNullPayloadClearsRetainState() throws Exception {
    Fixture fixture = new Fixture();
    Message message = retainedMessage(41, null);

    fixture.destination.addMessage(message);

    verify(fixture.retainManager).replace(-1);
    verify(fixture.stats).retainedMessages(-1);
  }

  @Test
  void addRetainedMessageWithEmptyPayloadClearsRetainState() throws Exception {
    Fixture fixture = new Fixture();
    Message message = retainedMessage(42, new byte[0]);

    fixture.destination.addMessage(message);

    verify(fixture.retainManager).replace(-1);
    verify(fixture.stats).retainedMessages(-1);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 32, 1024})
  void addRetainedMessageWithPayloadStoresIdentifier(int payloadLength) throws Exception {
    Fixture fixture = new Fixture();
    Message message = retainedMessage(43, new byte[payloadLength]);

    fixture.destination.addMessage(message);

    verify(fixture.retainManager).replace(43);
    verify(fixture.stats).retainedMessages(1);
  }

  @Test
  void resourceAddFailureIsPropagatedBeforeRetainChanges() throws Exception {
    Fixture fixture = new Fixture();
    Message message = retainedMessage(44, new byte[]{1});
    IOException failure = new IOException("add failed");
    doThrow(failure).when(fixture.resource).add(message);

    assertSame(failure, assertThrows(IOException.class, () -> fixture.destination.addMessage(message)));

    verify(fixture.retainManager, never()).replace(anyLong());
    verify(fixture.stats, never()).messageWriteTime(anyLong());
  }

  private static Message message(long expiry) {
    Message message = mock(Message.class);
    when(message.getExpiry()).thenReturn(expiry);
    return message;
  }

  private static Message retainedMessage(long identifier, byte[] payload) {
    Message message = mock(Message.class);
    when(message.isRetain()).thenReturn(true);
    when(message.getIdentifier()).thenReturn(identifier);
    when(message.getOpaqueData()).thenReturn(payload);
    return message;
  }

  private static final class Fixture {
    private final DestinationImpl destination = mock(DestinationImpl.class, CALLS_REAL_METHODS);
    private final Resource resource = mock(Resource.class);
    private final DestinationStats stats = mock(DestinationStats.class);
    private final RetainManager retainManager = mock(RetainManager.class);
    private final DestinationSubscriptionManager subscriptionManager =
        mock(DestinationSubscriptionManager.class);
    private final EventReaperQueue completionQueue = mock(EventReaperQueue.class);
    private final PriorityTaskScheduler resourceTaskQueue = mock(PriorityTaskScheduler.class);

    private Fixture() throws Exception {
      set("resource", resource);
      set("stats", stats);
      set("retainManager", retainManager);
      set("subscriptionManager", subscriptionManager);
      set("completionQueue", completionQueue);
      set("resourceTaskQueue", resourceTaskQueue);
    }

    private void set(String name, Object value) throws Exception {
      Field field = DestinationImpl.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(destination, value);
    }
  }
}
