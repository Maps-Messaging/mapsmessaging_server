package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

class MessageStateManagerImplTest {

  private static final String MANAGER_NAME = "test-manager";
  private static final long UNIQUE_SESSION_ID = 12345L;

  @Test
  void newInstance_isEmpty() {
    MessageStateManagerImpl manager = createManager();

    Assertions.assertTrue(manager.isEmpty());
    Assertions.assertEquals(0, manager.size());
    Assertions.assertEquals(0, manager.pending());
    Assertions.assertFalse(manager.hasMessagesInFlight());
    Assertions.assertFalse(manager.hasAtRestMessages());
    Assertions.assertEquals(-1L, manager.nextMessageId());
  }

  @Test
  void register_message_addsToAtRest_andIsVisibleInQueries() {
    MessageStateManagerImpl manager = createManager();

    Message message = createMessage(10L, 3);

    manager.register(message);

    Assertions.assertFalse(manager.isEmpty());
    Assertions.assertTrue(manager.hasAtRestMessages());
    Assertions.assertFalse(manager.hasMessagesInFlight());
    Assertions.assertEquals(1, manager.pending());
    Assertions.assertEquals(1, manager.size());
    Assertions.assertTrue(manager.hasMessage(10L));

    Queue<Long> atRest = manager.getAllAtRest();
    Assertions.assertEquals(1, atRest.size());
    Assertions.assertTrue(atRest.contains(10L));

    Queue<Long> all = manager.getAll();
    Assertions.assertEquals(1, all.size());
    Assertions.assertTrue(all.contains(10L));

    Assertions.assertEquals(10L, manager.nextMessageId());
  }

  @Test
  void register_messageId_addsToAtRest() {
    MessageStateManagerImpl manager = createManager();

    manager.register(42L);

    Assertions.assertTrue(manager.hasAtRestMessages());
    Assertions.assertEquals(1, manager.pending());
    Assertions.assertTrue(manager.hasMessage(42L));
    Assertions.assertEquals(42L, manager.nextMessageId());
  }

  @Test
  void allocate_movesMessageFromAtRestToInFlight_whenPresent() {
    MessageStateManagerImpl manager = createManager();

    Message message = createMessage(100L, 2);
    manager.register(message);

    manager.allocate(message);

    Assertions.assertFalse(manager.hasAtRestMessages());
    Assertions.assertTrue(manager.hasMessagesInFlight());
    Assertions.assertEquals(0, manager.pending());
    Assertions.assertEquals(1, manager.size());
    Assertions.assertTrue(manager.hasMessage(100L));

    Queue<Long> all = manager.getAll();
    Assertions.assertEquals(1, all.size());
    Assertions.assertTrue(all.contains(100L));
  }

  @Test
  void allocate_doesNotLeaveInFlight_whenNotAtRest() {
    MessageStateManagerImpl manager = createManager();

    Message message = createMessage(200L, 1);

    manager.allocate(message);

    Assertions.assertTrue(manager.isEmpty());
    Assertions.assertEquals(0, manager.size());
    Assertions.assertFalse(manager.hasMessage(200L));
  }

  @Test
  void commit_removesMessageFromInFlight() {
    MessageStateManagerImpl manager = createManager();

    Message message = createMessage(300L, 1);
    manager.register(message);
    manager.allocate(message);

    manager.commit(300L);

    Assertions.assertTrue(manager.isEmpty());
    Assertions.assertEquals(0, manager.size());
    Assertions.assertFalse(manager.hasMessage(300L));
  }

  @Test
  void rollback_movesMessageFromInFlightBackToAtRest() {
    MessageStateManagerImpl manager = createManager();

    Message message = createMessage(400L, 1);
    manager.register(message);
    manager.allocate(message);

    boolean rolledBack = manager.rollback(400L);

    Assertions.assertTrue(rolledBack);
    Assertions.assertTrue(manager.hasAtRestMessages());
    Assertions.assertFalse(manager.hasMessagesInFlight());
    Assertions.assertTrue(manager.hasMessage(400L));
    Assertions.assertEquals(1, manager.pending());
    Assertions.assertEquals(400L, manager.nextMessageId());
  }

  @Test
  void rollback_returnsFalse_whenMessageNotInFlight() {
    MessageStateManagerImpl manager = createManager();

    Message message = createMessage(500L, 1);
    manager.register(message);

    boolean rolledBack = manager.rollback(500L);

    Assertions.assertFalse(rolledBack);
    Assertions.assertTrue(manager.hasAtRestMessages());
    Assertions.assertFalse(manager.hasMessagesInFlight());
    Assertions.assertTrue(manager.hasMessage(500L));
  }

  @Test
  void rollbackInFlightMessages_movesAllBackToAtRest_andClearsInFlight() {
    MessageStateManagerImpl manager = createManager();

    Message message1 = createMessage(600L, 1);
    Message message2 = createMessage(601L, 1);
    manager.register(message1);
    manager.register(message2);

    manager.allocate(message1);
    manager.allocate(message2);

    Assertions.assertTrue(manager.hasMessagesInFlight());
    Assertions.assertFalse(manager.hasAtRestMessages());

    manager.rollbackInFlightMessages();

    Assertions.assertFalse(manager.hasMessagesInFlight());
    Assertions.assertTrue(manager.hasAtRestMessages());
    Assertions.assertEquals(2, manager.pending());
    Assertions.assertTrue(manager.hasMessage(600L));
    Assertions.assertTrue(manager.hasMessage(601L));
  }

  @Test
  void expired_removesFromBothAtRestAndInFlight() {
    MessageStateManagerImpl manager = createManager();

    Message message = createMessage(700L, 1);
    manager.register(message);
    manager.allocate(message);

    Assertions.assertTrue(manager.hasMessage(700L));

    manager.expired(700L);

    Assertions.assertFalse(manager.hasMessage(700L));
    Assertions.assertTrue(manager.isEmpty());
  }

  @Test
  void register_notifiesListeners() {
    MessageStateManagerImpl manager = createManager();
    RecordingMessageStateManagerListener listener = new RecordingMessageStateManagerListener();
    manager.add(listener);

    Message message = createMessage(800L, 9);

    manager.register(message);

    Assertions.assertEquals(1, listener.addCalls.size());
    Assertions.assertEquals(0, listener.addAllCalls.size());
    Assertions.assertEquals(0, listener.removeCalls.size());

    RecordedAddCall addCall = listener.addCalls.get(0);
    Assertions.assertEquals(800L, addCall.messageIdentifier);
    Assertions.assertEquals(9, addCall.priority);
  }

  @Test
  void commit_notifiesListeners_remove() {
    MessageStateManagerImpl manager = createManager();
    RecordingMessageStateManagerListener listener = new RecordingMessageStateManagerListener();
    manager.add(listener);

    Message message = createMessage(900L, 1);
    manager.register(message);
    manager.allocate(message);

    listener.reset();

    manager.commit(900L);

    Assertions.assertEquals(0, listener.addCalls.size());
    Assertions.assertEquals(0, listener.addAllCalls.size());
    Assertions.assertEquals(1, listener.removeCalls.size());
    Assertions.assertEquals(900L, listener.removeCalls.get(0));
  }

  @Test
  void rollbackInFlightMessages_notifiesListeners_addAll_beforeRollback() {
    MessageStateManagerImpl manager = createManager();
    RecordingMessageStateManagerListener listener = new RecordingMessageStateManagerListener();
    manager.add(listener);

    Message message1 = createMessage(1000L, 1);
    Message message2 = createMessage(1001L, 1);

    manager.register(message1);
    manager.register(message2);
    manager.allocate(message1);
    manager.allocate(message2);

    listener.reset();

    manager.rollbackInFlightMessages();

    Assertions.assertEquals(0, listener.addCalls.size());
    Assertions.assertEquals(1, listener.addAllCalls.size());
    Assertions.assertEquals(0, listener.removeCalls.size());

    Set<Long> seen = listener.addAllCalls.get(0);
    Assertions.assertTrue(seen.contains(1000L));
    Assertions.assertTrue(seen.contains(1001L));
  }

  @Test
  void register_allPriorities_fromLowestToHighest_nextMessageId_returnsHighestFirst_andAllPresent() {
    MessageStateManagerImpl manager = createManager();

    // Register one message per priority value (0..10)
    for (int priorityValue = 0; priorityValue <= io.mapsmessaging.api.features.Priority.HIGHEST.getValue(); priorityValue++) {
      long messageId = 1000L + priorityValue;
      Message message = createMessage(messageId, priorityValue);
      manager.register(message);
    }

    int expectedCount = io.mapsmessaging.api.features.Priority.HIGHEST.getValue() + 1;

    Assertions.assertEquals(expectedCount, manager.pending());
    Assertions.assertEquals(expectedCount, manager.size());
    Assertions.assertTrue(manager.hasAtRestMessages());
    Assertions.assertFalse(manager.hasMessagesInFlight());

    for (int priorityValue = 0; priorityValue <= io.mapsmessaging.api.features.Priority.HIGHEST.getValue(); priorityValue++) {
      long messageId = 1000L + priorityValue;
      Assertions.assertTrue(manager.hasMessage(messageId));
    }

    // Expect highest priority to be delivered first.
    // If your PriorityQueue uses the opposite ordering, flip the loop.
    for (int priorityValue = io.mapsmessaging.api.features.Priority.HIGHEST.getValue(); priorityValue >= 0; priorityValue--) {
      long expectedNextId = 1000L + priorityValue;
      long actualNextId = manager.nextMessageId();
      Assertions.assertEquals(expectedNextId, actualNextId);

      Message message = createMessage(expectedNextId, priorityValue);
      manager.allocate(message);

      Assertions.assertTrue(manager.hasMessage(expectedNextId));
      Assertions.assertTrue(manager.hasMessagesInFlight() || manager.pending() > 0);
    }

    Assertions.assertEquals(0, manager.pending());
    Assertions.assertEquals(expectedCount, manager.size());
    Assertions.assertTrue(manager.hasMessagesInFlight());
    Assertions.assertFalse(manager.hasAtRestMessages());
    Assertions.assertEquals(-1L, manager.nextMessageId());
  }


  private MessageStateManagerImpl createManager() {
    BitSetFactory bitSetFactory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    return new MessageStateManagerImpl(MANAGER_NAME, UNIQUE_SESSION_ID, bitSetFactory);
  }

  private Message createMessage(long id, int priorityValue) {
    Message message = Mockito.mock(Message.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(message.getIdentifier()).thenReturn(id);
    Mockito.when(message.getPriority().getValue()).thenReturn(priorityValue);
    return message;
  }

  private static final class RecordingMessageStateManagerListener implements MessageStateManagerListener {

    private final List<RecordedAddCall> addCalls;
    private final List<Set<Long>> addAllCalls;
    private final List<Long> removeCalls;

    private RecordingMessageStateManagerListener() {
      this.addCalls = new ArrayList<>();
      this.addAllCalls = new ArrayList<>();
      this.removeCalls = new ArrayList<>();
    }

    @Override
    public void add(long messageIdentifier, int priority) {
      addCalls.add(new RecordedAddCall(messageIdentifier, priority));
    }

    @Override
    public void addAll(Collection<Long> queue) {
      addAllCalls.add(new HashSet<>(queue));
    }

    @Override
    public void remove(long messageIdentifier) {
      removeCalls.add(messageIdentifier);
    }

    private void reset() {
      addCalls.clear();
      addAllCalls.clear();
      removeCalls.clear();
    }
  }

  private static final class RecordedAddCall {

    private final long messageIdentifier;
    private final int priority;

    private RecordedAddCall(long messageIdentifier, int priority) {
      this.messageIdentifier = messageIdentifier;
      this.priority = priority;
    }
  }
}
