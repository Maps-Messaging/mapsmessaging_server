package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions.handler;

import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;

public class NackProcessor implements TransactionProcessor{

  @Override
  public void handle(StreamSubscriptionInfo info, TransactionSubject transactionSubject) {
    info.getSubscribedEventManager().rollbackReceived(transactionSubject.getMessageId());
  }
}
