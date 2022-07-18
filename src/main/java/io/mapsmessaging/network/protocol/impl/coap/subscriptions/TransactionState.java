package io.mapsmessaging.network.protocol.impl.coap.subscriptions;

import io.mapsmessaging.api.SubscribedEventManager;
import java.util.LinkedHashMap;
import java.util.Map;

public class TransactionState {

  private Map<Long, TransactionContext> outstandingEvents;

  public TransactionState(){
    outstandingEvents = new LinkedHashMap<>();
  }

  public void sent(byte[] token, long messageId, SubscribedEventManager subscription){
    if(token.length > 0) {
      long key = tokenToLong(token);
      outstandingEvents.put(key, new TransactionContext(messageId, subscription));
    }
  }

  public void ack(byte[] token){
    if(token.length > 0) {
      long key = tokenToLong(token);
      TransactionContext transactionContext = outstandingEvents.remove(key);
      if (transactionContext != null) {
        transactionContext.ack();
      }
    }
  }

  private long tokenToLong(byte[] token){
    long val = 0;
    for (byte b : token) {
      val = (val << 8) | (b & 0xff);
    }
    return val;
  }

  private static class TransactionContext {

    private final long messageId;
    private final SubscribedEventManager subscription;

    public TransactionContext(long messageId, SubscribedEventManager subscription){
      this.messageId = messageId;
      this.subscription = subscription;
    }

    public void ack(){
      subscription.ackReceived(messageId);
    }
  }
}
