package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.impl.ClientSubscribedEventManager;
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
      if(context.isBrowser() && destination.getResourceType().isQueue()){
        // We are now looking at the base queue so we need to find "shared_<Name Of Queue>_normal"
        subscription = controller.createBrowserSubscription(context, destination.getSubscription(destination.getFullyQualifiedNamespace()), destination);
      }
      else {
        subscription = controller.get(destination);
        if (subscription != null) {
          subscription.addContext(context);
        } else {
          subscription = controller.createSubscription(context, destination);
        }
      }
    } finally {
      counter.decrementAndGet();
    }
    return new SubscriptionResponse( new ClientSubscribedEventManager(destination, subscription));
  }

}
