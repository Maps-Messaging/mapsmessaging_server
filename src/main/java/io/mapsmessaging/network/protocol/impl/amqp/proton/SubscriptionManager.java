package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.api.Session;
import org.apache.qpid.proton.engine.Sender;

import java.util.LinkedHashMap;
import java.util.Map;

public class SubscriptionManager {

  private final Map<String, Sender> subscriptions;

  public SubscriptionManager() {
    subscriptions = new LinkedHashMap<>();
  }

  public void close() {
    for (Map.Entry<String, Sender> entry : subscriptions.entrySet()) {
      Object sessionContext = entry.getValue().getSession().getContext();
      if (sessionContext != null) {
        Session session = (Session) sessionContext;
        session.removeSubscription(entry.getKey());
      }
    }
    subscriptions.clear();
  }

  public synchronized void put(String alias, Sender sender) {
    subscriptions.put(alias, sender);
  }

  public synchronized void remove(String alias) {
    subscriptions.remove(alias);
  }

  public synchronized Sender get(String alias) {
    return subscriptions.get(alias);
  }
}
