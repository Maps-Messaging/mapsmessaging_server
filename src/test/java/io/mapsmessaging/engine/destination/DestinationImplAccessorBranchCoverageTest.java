package io.mapsmessaging.engine.destination;

import io.mapsmessaging.admin.DestinationJMX;
import io.mapsmessaging.engine.destination.delayed.DelayedMessageManager;
import io.mapsmessaging.engine.destination.delayed.TransactionalMessageManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DestinationImplAccessorBranchCoverageTest {

  @Test
  void delayedMessageCountIsZeroWithoutManager() throws Exception {
    DestinationImpl destination = destination();
    set(destination, "delayedMessageManager", null);

    assertEquals(0L, destination.getDelayedMessages());
  }

  @Test
  void delayedMessageCountComesFromManager() throws Exception {
    DestinationImpl destination = destination();
    DelayedMessageManager manager = mock(DelayedMessageManager.class);
    when(manager.size()).thenReturn(7L);
    set(destination, "delayedMessageManager", manager);

    assertEquals(7L, destination.getDelayedMessages());
  }

  @Test
  void pendingTransactionCountIsZeroWithoutManager() throws Exception {
    DestinationImpl destination = destination();
    set(destination, "transactionMessageManager", null);

    assertEquals(0L, destination.getPendingTransactions());
  }

  @Test
  void pendingTransactionCountComesFromManager() throws Exception {
    DestinationImpl destination = destination();
    TransactionalMessageManager manager = mock(TransactionalMessageManager.class);
    when(manager.size()).thenReturn(9L);
    set(destination, "transactionMessageManager", manager);

    assertEquals(9L, destination.getPendingTransactions());
  }

  @Test
  void typePathIsEmptyWithoutJmxBean() throws Exception {
    DestinationImpl destination = destination();
    set(destination, "destinationJMXBean", null);

    assertTrue(destination.getTypePath().isEmpty());
  }

  @Test
  void typePathComesFromJmxBean() throws Exception {
    DestinationImpl destination = destination();
    DestinationJMX jmx = mock(DestinationJMX.class);
    when(jmx.getTypePath()).thenReturn(List.of("type=Destination", "name=test"));
    set(destination, "destinationJMXBean", jmx);

    assertEquals(List.of("type=Destination", "name=test"), destination.getTypePath());
  }

  private static DestinationImpl destination() {
    return mock(DestinationImpl.class, CALLS_REAL_METHODS);
  }

  private static void set(DestinationImpl destination, String name, Object value) throws Exception {
    Field field = DestinationImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(destination, value);
  }
}
