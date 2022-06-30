package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.impl.SchemaSubscribedEventManager;
import io.mapsmessaging.engine.schema.Schema;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import java.nio.charset.StandardCharsets;
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
      Schema schema = destination.getSchema();
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(schema.toString().getBytes(StandardCharsets.UTF_8));

     // subscription.sendMessage(messageBuilder.build());
    } finally {
      counter.decrementAndGet();
    }
    return new SubscriptionResponse(new SchemaSubscribedEventManager(subscription));
  }
}
