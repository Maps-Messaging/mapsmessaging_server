package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions.handler;

import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;

public interface TransactionProcessor {
  void handle(StreamSubscriptionInfo info, TransactionSubject transactionSubject);
}
