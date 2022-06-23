package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsSubscriptionTask extends SubscriptionTask {

  public MetricsSubscriptionTask(SubscriptionController controller,
      SubscriptionContext context, DestinationImpl destination,
      AtomicLong counter) {
    super(controller, context, destination, counter);
  }
}
