package io.mapsmessaging.network.protocol.impl.coap.subscriptions;

import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import java.util.LinkedHashMap;
import java.util.Map;


public class SubscriptionState {


  private final Map<String, Context> state;

  public SubscriptionState() {
    state = new LinkedHashMap<>();
  }

  public Context create(String path, BasePacket request) {
    Context context = new Context(path, request);
    state.put(path, context);
    return context;
  }

  public Context find(String path){
    return state.get(path);
  }


  public Context remove(String path) {
    return state.remove(path);
  }

  public boolean exists(String path) {
    return state.containsKey(path);
  }

  public boolean isEmpty() {
    return state.isEmpty();
  }
}
