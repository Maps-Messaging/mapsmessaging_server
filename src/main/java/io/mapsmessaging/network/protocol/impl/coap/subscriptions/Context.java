package io.mapsmessaging.network.protocol.impl.coap.subscriptions;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import lombok.Getter;
import lombok.Setter;

public class Context {

  @Getter
  private final String path;

  @Getter
  private final BasePacket request;

  @Getter
  @Setter
  private SubscribedEventManager subscribedEventManager;

  public Context(String path, BasePacket request){
    this.path = path;
    this.request = request;
  }

}
