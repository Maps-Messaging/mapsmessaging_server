package io.mapsmessaging.engine.destination;

import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import io.mapsmessaging.engine.destination.delayed.TransactionalMessageManager;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.destination.subscription.Subscribable;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscriptionManager;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscriptionRegister;
import io.mapsmessaging.engine.destination.tasks.BulkRemoveMessageTask;
import io.mapsmessaging.engine.destination.tasks.TransactionalMessageProcessor;
import io.mapsmessaging.engine.resources.Resource;
import io.mapsmessaging.utilities.threads.tasks.PriorityTaskScheduler;
import io.mapsmessaging.utilities.threads.tasks.TaskScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DestinationImplSubscriptionMaintenanceCoverageTest {

  @Test
  void addSubscriptionRegistersSessionAndUpdatesStats() throws Exception {
    Fixture fixture = new Fixture();
    Subscription subscription = mock(Subscription.class);
    when(subscription.getSessionId()).thenReturn("session-1");

    fixture.destination.addSubscription(subscription);

    verify(fixture.subscriptionManager).put("session-1", subscription);
    verify(fixture.stats).subscriptionAdded();
  }

  @Test
  void removeMissingSubscriptionOnlyUpdatesStats() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.subscriptionManager.remove("missing")).thenReturn(null);

    assertNull(fixture.destination.removeSubscription("missing"));

    verify(fixture.stats).subscriptionRemoved();
    verify(fixture.subscriptionManager, never()).scanForInterest(any());
    verify(fixture.resourceTaskQueue, never()).submit(any(Callable.class), anyInt());
  }

  @Test
  void removeSubscriptionWithNoAtRestMessagesDoesNotSubmitCleanup() throws Exception {
    Fixture fixture = new Fixture();
    Subscribable subscription = mock(Subscribable.class);
    when(subscription.getAllAtRest()).thenReturn(new ArrayDeque<>());
    when(fixture.subscriptionManager.remove("session-2")).thenReturn(subscription);

    assertSame(subscription, fixture.destination.removeSubscription("session-2"));

    verify(fixture.subscriptionManager, never()).scanForInterest(any());
    verify(fixture.resourceTaskQueue, never()).submit(any(Callable.class), anyInt());
  }

  @Test
  void removeSubscriptionSkipsCleanupWhenOtherSubscribersNeedEverything() throws Exception {
    Fixture fixture = new Fixture();
    Subscribable subscription = subscribableWithAtRest(71L, 72L);
    when(fixture.subscriptionManager.remove("session-3")).thenReturn(subscription);
    when(fixture.subscriptionManager.scanForInterest(any())).thenAnswer(invocation -> {
      Queue<Long> queue = invocation.getArgument(0);
      queue.clear();
      return queue;
    });

    fixture.destination.removeSubscription("session-3");

    verify(fixture.resourceTaskQueue, never()).submit(any(Callable.class), anyInt());
    verify(fixture.stats, never()).storedMessages(anyInt());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void removeSubscriptionSubmitsMessagesNoOtherSubscriptionNeeds(int remainingCount) throws Exception {
    Fixture fixture = new Fixture();
    Subscribable subscription = subscribableWithAtRest(81L, 82L, 83L);
    when(fixture.subscriptionManager.remove("session-4")).thenReturn(subscription);
    when(fixture.subscriptionManager.scanForInterest(any())).thenAnswer(invocation -> {
      Queue<Long> queue = invocation.getArgument(0);
      while (queue.size() > remainingCount) {
        queue.poll();
      }
      return queue;
    });

    assertSame(subscription, fixture.destination.removeSubscription("session-4"));

    ArgumentCaptor<Callable> captor = ArgumentCaptor.forClass(Callable.class);
    verify(fixture.resourceTaskQueue)
        .submit(captor.capture(), eq(DestinationImpl.PUBLISH_PRIORITY));
    assertInstanceOf(BulkRemoveMessageTask.class, captor.getValue());
    verify(fixture.stats).storedMessages(remainingCount);
  }

  @Test
  void schemaSubscriptionIsRegisteredAndCounted() throws Exception {
    Fixture fixture = new Fixture();
    Subscription subscription = mock(Subscription.class);
    when(subscription.getSessionId()).thenReturn("schema-session");

    fixture.destination.addSchemaSubscription(subscription);

    verify(fixture.schemaSubscriptionManager).put("schema-session", subscription);
    verify(fixture.stats).subscriptionAdded();
  }

  @Test
  void schemaSubscriptionRemovalIsRegisteredAndCounted() throws Exception {
    Fixture fixture = new Fixture();
    Subscription subscription = mock(Subscription.class);
    when(subscription.getSessionId()).thenReturn("schema-session");

    fixture.destination.removeSchemaSubscription(subscription);

    verify(fixture.schemaSubscriptionManager).remove("schema-session");
    verify(fixture.stats).subscriptionRemoved();
  }

  @Test
  void destinationSubscriptionIsReturnedForConcreteDestinationSubscription() throws Exception {
    Fixture fixture = new Fixture();
    DestinationSubscription subscription = mock(DestinationSubscription.class);
    when(fixture.subscriptionManager.getSubscription("sub")).thenReturn(subscription);

    assertSame(subscription, fixture.destination.getSubscription("sub"));
  }

  @Test
  void nonDestinationSubscriptionReturnsNull() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.subscriptionManager.getSubscription("sub")).thenReturn(mock(Subscribable.class));

    assertNull(fixture.destination.getSubscription("sub"));
  }

  @Test
  void missingSubscriptionReturnsNull() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.subscriptionManager.getSubscription("sub")).thenReturn(null);

    assertNull(fixture.destination.getSubscription("sub"));
  }

  @Test
  void orphanScanSkipsSystemDestinations() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("fullyQualifiedNamespace", "$SYS/server");

    fixture.destination.scanForOrphanedMessages();

    verify(fixture.resource, never()).size();
    verify(fixture.resource, never()).keepOnly(anyList());
  }

  @Test
  void orphanScanDoesNothingWhenStoredSizeMatchesInterestSet() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("fullyQualifiedNamespace", "sensor/data");
    when(fixture.retainManager.current()).thenReturn(-1L);
    when(fixture.subscriptionManager.getAll()).thenReturn(queue(91L, 92L));
    when(fixture.resource.size()).thenReturn(2L);

    fixture.destination.scanForOrphanedMessages();

    verify(fixture.resource, never()).keepOnly(anyList());
  }

  @Test
  void orphanScanKeepsSubscriptionInterestWhenStoredSizeDiffers() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("fullyQualifiedNamespace", "sensor/data");
    when(fixture.retainManager.current()).thenReturn(-1L);
    when(fixture.subscriptionManager.getAll()).thenReturn(queue(101L, 102L));
    when(fixture.resource.size()).thenReturn(4L);
    when(fixture.resource.contains(anyLong())).thenReturn(true);

    fixture.destination.scanForOrphanedMessages();

    ArgumentCaptor<List<Long>> keep = listCaptor();
    verify(fixture.resource).keepOnly(keep.capture());
    assertEquals(2, keep.getValue().size());
    assertTrue(keep.getValue().containsAll(List.of(101L, 102L)));
    verify(fixture.subscriptionManager, never()).expired(anyLong());
  }

  @Test
  void orphanScanIncludesPositiveRetainedMessage() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("fullyQualifiedNamespace", "sensor/data");
    when(fixture.retainManager.current()).thenReturn(110L);
    when(fixture.subscriptionManager.getAll()).thenReturn(queue(111L, 112L));
    when(fixture.resource.size()).thenReturn(5L);
    when(fixture.resource.contains(anyLong())).thenReturn(true);

    fixture.destination.scanForOrphanedMessages();

    ArgumentCaptor<List<Long>> keep = listCaptor();
    verify(fixture.resource).keepOnly(keep.capture());
    assertEquals(3, keep.getValue().size());
    assertTrue(keep.getValue().containsAll(List.of(110L, 111L, 112L)));
  }

  @Test
  void orphanScanIncludesRetainedIdentifierZero() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("fullyQualifiedNamespace", "sensor/data");
    when(fixture.retainManager.current()).thenReturn(0L);
    when(fixture.subscriptionManager.getAll()).thenReturn(queue(1L));
    when(fixture.resource.size()).thenReturn(3L);
    when(fixture.resource.contains(anyLong())).thenReturn(true);

    fixture.destination.scanForOrphanedMessages();

    ArgumentCaptor<List<Long>> keep = listCaptor();
    verify(fixture.resource).keepOnly(keep.capture());
    assertTrue(keep.getValue().containsAll(List.of(0L, 1L)));
  }

  @Test
  void orphanScanExpiresInterestedMessagesMissingFromResource() throws Exception {
    Fixture fixture = new Fixture();
    fixture.set("fullyQualifiedNamespace", "sensor/data");
    when(fixture.retainManager.current()).thenReturn(-1L);
    when(fixture.subscriptionManager.getAll()).thenReturn(queue(121L, 122L, 123L));
    when(fixture.resource.size()).thenReturn(5L);
    when(fixture.resource.contains(121L)).thenReturn(true);
    when(fixture.resource.contains(122L)).thenReturn(false);
    when(fixture.resource.contains(123L)).thenReturn(false);

    fixture.destination.scanForOrphanedMessages();

    verify(fixture.subscriptionManager, never()).expired(121L);
    verify(fixture.subscriptionManager).expired(122L);
    verify(fixture.subscriptionManager).expired(123L);
  }

  @Test
  void subscriptionStatesCombineNormalSchemaAndSharedStates() throws Exception {
    Fixture fixture = new Fixture();
    SubscriptionStateDTO normal = mock(SubscriptionStateDTO.class);
    SubscriptionStateDTO schema = mock(SubscriptionStateDTO.class);
    SubscriptionStateDTO shared = mock(SubscriptionStateDTO.class);
    when(fixture.subscriptionManager.getSubscriptionStates()).thenReturn(List.of(normal));
    when(fixture.schemaSubscriptionManager.getSubscriptionStates()).thenReturn(List.of(schema));
    when(fixture.sharedSubscriptionRegistry.getState()).thenReturn(List.of(shared));

    assertEquals(List.of(normal, schema, shared), fixture.destination.getSubscriptionStates());
  }

  @Test
  void pauseClientRequestsDelegatesToSubscriptionManager() throws Exception {
    Fixture fixture = new Fixture();

    fixture.destination.pauseClientRequests();

    verify(fixture.subscriptionManager).pause();
  }

  @Test
  void stopSubscriptionsShutsDownSubscriptionTaskQueue() throws Exception {
    Fixture fixture = new Fixture();

    fixture.destination.stopSubscriptions();

    verify(fixture.subscriptionTaskQueue).shutdown();
  }

  @Test
  void abortRemovesTransactionBucketAtDeletePriority() throws Exception {
    Fixture fixture = new Fixture();
    Queue<Long> bucket = queue(131L, 132L);
    when(fixture.transactionMessageManager.removeBucket(77L)).thenReturn(bucket);

    fixture.destination.abort(77L);

    ArgumentCaptor<Callable> captor = ArgumentCaptor.forClass(Callable.class);
    verify(fixture.resourceTaskQueue)
        .submit(captor.capture(), eq(DestinationImpl.DELETE_PRIORITY));
    assertInstanceOf(BulkRemoveMessageTask.class, captor.getValue());
  }

  @Test
  void commitSubmitsTransactionalProcessorToSubscriptionQueue() throws Exception {
    Fixture fixture = new Fixture();

    fixture.destination.commit(78L);

    ArgumentCaptor<Callable> captor = ArgumentCaptor.forClass(Callable.class);
    verify(fixture.subscriptionTaskQueue).submit(captor.capture());
    assertInstanceOf(TransactionalMessageProcessor.class, captor.getValue());
  }

  @Test
  void sharedRegistryAddUsesManagerName() throws Exception {
    Fixture fixture = new Fixture();
    SharedSubscriptionManager manager = mock(SharedSubscriptionManager.class);
    when(manager.getName()).thenReturn("shared-A");

    fixture.destination.addShareRegistry(manager);

    verify(fixture.sharedSubscriptionRegistry).add("shared-A", manager);
  }

  @Test
  void sharedRegistryLookupDelegatesByName() throws Exception {
    Fixture fixture = new Fixture();
    SharedSubscriptionManager manager = mock(SharedSubscriptionManager.class);
    when(fixture.sharedSubscriptionRegistry.get("shared-A")).thenReturn(manager);

    assertSame(manager, fixture.destination.findShareRegister("shared-A"));
  }

  @Test
  void sharedRegistryDeleteDelegatesByName() throws Exception {
    Fixture fixture = new Fixture();

    fixture.destination.delShareRegistry("shared-A");

    verify(fixture.sharedSubscriptionRegistry).del("shared-A");
  }

  private static Subscribable subscribableWithAtRest(Long... ids) {
    Subscribable subscription = mock(Subscribable.class);
    when(subscription.getAllAtRest()).thenReturn(queue(ids));
    return subscription;
  }

  private static Queue<Long> queue(Long... ids) {
    return new ArrayDeque<>(List.of(ids));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static ArgumentCaptor<List<Long>> listCaptor() {
    return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
  }

  private static final class Fixture {
    private final DestinationImpl destination = mock(DestinationImpl.class, CALLS_REAL_METHODS);
    private final Resource resource = mock(Resource.class);
    private final DestinationStats stats = mock(DestinationStats.class);
    private final RetainManager retainManager = mock(RetainManager.class);
    private final DestinationSubscriptionManager subscriptionManager =
        mock(DestinationSubscriptionManager.class);
    private final DestinationSubscriptionManager schemaSubscriptionManager =
        mock(DestinationSubscriptionManager.class);
    private final SharedSubscriptionRegister sharedSubscriptionRegistry =
        mock(SharedSubscriptionRegister.class);
    private final PriorityTaskScheduler resourceTaskQueue =
        mock(PriorityTaskScheduler.class);
    private final TaskScheduler subscriptionTaskQueue = mock(TaskScheduler.class);
    private final TransactionalMessageManager transactionMessageManager =
        mock(TransactionalMessageManager.class);

    private Fixture() throws Exception {
      set("resource", resource);
      set("stats", stats);
      set("retainManager", retainManager);
      set("subscriptionManager", subscriptionManager);
      set("schemaSubscriptionManager", schemaSubscriptionManager);
      set("sharedSubscriptionRegistry", sharedSubscriptionRegistry);
      set("resourceTaskQueue", resourceTaskQueue);
      set("subscriptionTaskQueue", subscriptionTaskQueue);
      set("transactionMessageManager", transactionMessageManager);
      set("fullyQualifiedNamespace", "coverage/destination");
    }

    private void set(String name, Object value) throws Exception {
      Field field = DestinationImpl.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(destination, value);
    }
  }
}
