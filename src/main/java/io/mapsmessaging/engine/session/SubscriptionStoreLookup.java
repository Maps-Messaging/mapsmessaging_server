package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.serializer.MapDBSerializer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.mapdb.DB;
import org.mapdb.Serializer;

public class SubscriptionStoreLookup {

  private static final String SUBSCRIPTION = "subscription_";

  private final DB dataStore;

  public SubscriptionStoreLookup(DB dataStore) {
    this.dataStore = dataStore;
  }

  public Map<String, SubscriptionContext> getSubscriptionContextMap(String sessionId, boolean isPersistent) {
    Map<String, SubscriptionContext> map;
    if (isPersistent) {
       String mapName = SUBSCRIPTION + sessionId;
       map = dataStore.hashMap(mapName, Serializer.STRING, new MapDBSerializer<>(SubscriptionContext.class)).createOrOpen();
     } else {
        map = new LinkedHashMap<>();
     }
    return map;
  }
}
