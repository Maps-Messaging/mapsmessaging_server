package io.mapsmessaging.network.protocol.impl.semtech;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Getter;
import lombok.Setter;

public class GatewayInfo {

  @Getter
  private final Destination inbound;
  @Getter
  private final Destination status;

  @Getter
  private final SubscribedEventManager outbound;
  @Getter
  private final byte[] raw_identifier;
  @Getter
  private final String name;
  @Getter
  @Setter
  private long lastAccess;


  @Getter
  private final Queue<MessageEvent> waitingMessages;

  public GatewayInfo(byte[] raw_identifier, String name, Destination inbound, Destination status, SubscribedEventManager outbound) {
    this.raw_identifier = raw_identifier;
    this.name = name;
    this.inbound = inbound;
    this.status = status;
    this.outbound = outbound;
    waitingMessages = new ConcurrentLinkedQueue<>();
    lastAccess = System.currentTimeMillis();
  }

  public void close(Session session) {
    session.removeSubscription(outbound.getContext().getAlias());
    waitingMessages.clear();
  }
}
