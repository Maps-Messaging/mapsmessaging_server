package io.mapsmessaging.engine.destination.subscription.modes;

import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.engine.destination.subscription.tasks.UnsubscribeTask;
import io.mapsmessaging.engine.tasks.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionModeManagerBranchCoverageTest {

  @Test
  void addGetRemoveAndDetailsOperateOnActiveSubscriptions() {
    TestManager manager = new TestManager();
    DestinationImpl destination = mock(DestinationImpl.class);
    Subscription subscription = mock(Subscription.class);
    SubscriptionStateDTO state = new SubscriptionStateDTO();
    when(subscription.getState()).thenReturn(state);

    manager.add(subscription, destination);

    assertSame(subscription, manager.get(destination));
    assertEquals(java.util.List.of(state), manager.getDetails());
    assertSame(subscription, manager.remove(destination));
    assertNull(manager.get(destination));
  }

  @Test
  void closeAndShutdownSelectDeleteVersusCloseBehavior() throws Exception {
    TestManager manager = new TestManager();
    DestinationImpl one = mock(DestinationImpl.class);
    Subscription first = mock(Subscription.class);
    manager.add(first, one);

    manager.close(false);
    verify(first).delete();

    DestinationImpl two = mock(DestinationImpl.class);
    Subscription second = mock(Subscription.class);
    manager.add(second, two);

    manager.shutdown();
    verify(second).close();
    assertTrue(manager.getDetails().isEmpty());
  }

  @Test
  void hibernateAndHibernateSubscriptionReachMatchingSubscriptions() {
    TestManager manager = new TestManager();
    DestinationImpl matching = mock(DestinationImpl.class);
    DestinationImpl other = mock(DestinationImpl.class);
    when(matching.getFullyQualifiedNamespace()).thenReturn("/match");
    when(other.getFullyQualifiedNamespace()).thenReturn("/other");
    Subscription first = mock(Subscription.class);
    Subscription second = mock(Subscription.class);
    manager.add(first, matching);
    manager.add(second, other);

    manager.hibernateSubscription("/match");
    verify(first).hibernate();
    verify(second, never()).hibernate();

    manager.hibernate();
    verify(first, times(2)).hibernate();
    verify(second).hibernate();
  }

  private static final class TestManager extends SubscriptionModeManager {
    TestManager() {
      super(DestinationMode.NORMAL);
    }

    @Override
    protected String adjustUnsubscribeId(String id) {
      return id;
    }

    @Override
    protected UnsubscribeTask createUnsubscribeTask(
        SubscriptionController controller,
        DestinationImpl destination,
        DestinationSet destinationSet,
        AtomicLong counter) {
      return null;
    }

    @Override
    protected Future<Response> scheduleSubscription(
        SubscriptionController controller,
        SubscriptionContext context,
        DestinationImpl destinationImpl,
        AtomicLong counter) {
      return null;
    }
  }
}