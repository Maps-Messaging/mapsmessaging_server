package io.mapsmessaging.engine.destination.subscription.builders;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.impl.SchemaSubscription;
import io.mapsmessaging.engine.session.SessionImpl;
import java.io.IOException;

public class SchemaSubscriptionBuilder extends SubscriptionBuilder {

  public SchemaSubscriptionBuilder(DestinationImpl destination, SubscriptionContext context) throws IOException {
    super(destination, context);
  }

  public Subscription construct(SessionImpl session, String sessionId, String uniqueSessionId) throws IOException {
    return new SchemaSubscription(session, destination, context);
  }
}