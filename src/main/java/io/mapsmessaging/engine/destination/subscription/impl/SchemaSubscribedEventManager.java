package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import java.util.List;

public class SchemaSubscribedEventManager implements SubscribedEventManager {

  private final SubscribedEventManager subscription;

  public SchemaSubscribedEventManager(SubscribedEventManager subscription) {
    this.subscription = subscription;
  }

  @Override
  public void rollbackReceived(long messageId) {
    // A Schema subscription is a simple deliver a single message when it changes
  }

  @Override
  public void ackReceived(long messageId) {
    // A Schema subscription is a simple deliver a single message when it changes
  }

  @Override
  public SubscriptionContext getContext() {
    return subscription.getContext();
  }

  @Override
  public List<SubscriptionContext> getContexts() {
    return subscription.getContexts();
  }

  @Override
  public void updateCredit(int credit) {
    // A Schema subscription is a simple deliver a single message when it changes
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public int getPending() {
    return 0;
  }


  @Override
  public int getDepth() {
    return subscription.getDepth();
  }
}
