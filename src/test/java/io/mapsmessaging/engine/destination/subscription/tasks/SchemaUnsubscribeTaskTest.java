package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaUnsubscribeTaskTest {

  @Test
  void schemaLookupAndRemovalUseSchemaControllerPaths() {
    SubscriptionController controller = mock(SubscriptionController.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    Subscription subscription = mock(Subscription.class);
    when(controller.getSchema(destination)).thenReturn(subscription);
    when(controller.removeSchema(destination)).thenReturn(subscription);

    TestTask task = new TestTask(
        controller,
        destination,
        mock(DestinationSet.class),
        new AtomicLong(1)
    );

    assertSame(subscription, task.lookup(destination));
    assertSame(subscription, task.remove(destination));
    verify(controller).getSchema(destination);
    verify(controller).removeSchema(destination);
  }

  private static final class TestTask extends SchemaUnsubscribeTask {
    TestTask(
        SubscriptionController controller,
        DestinationImpl destination,
        DestinationSet set,
        AtomicLong counter
    ) {
      super(controller, destination, set, counter);
    }

    Subscription lookup(DestinationImpl destination) {
      return lookupSubscription(destination);
    }

    Subscription remove(DestinationImpl destination) {
      return removeDestination(destination);
    }
  }
}
