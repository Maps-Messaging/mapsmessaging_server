package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.impl.SchemaSubscribedEventManager;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import java.util.concurrent.atomic.AtomicLong;

public class SchemaSubscriptionTask extends SubscriptionTask {

  public SchemaSubscriptionTask(SubscriptionController controller,
      SubscriptionContext context, DestinationImpl destination,
      AtomicLong counter) {
    super(controller, context, destination, counter);
  }

  @Override
  public Response taskCall() throws Exception {
    Subscription subscription;
    try {
      subscription = controller.getSchema(destination);
      if (subscription != null) {
        subscription.addContext(context);
      } else {
        subscription = controller.createSchemaSubscription(context, destination);
      }
    } finally {
      counter.decrementAndGet();
    }
    return new SubscriptionResponse(new SchemaSubscribedEventManager(subscription));
  }
}
