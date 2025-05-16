/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.coap.subscriptions;

import io.mapsmessaging.api.SubscribedEventManager;

import java.util.LinkedHashMap;
import java.util.Map;

public class TransactionState {

  private final Map<Long, TransactionContext> outstandingEvents;
  private final Map<Integer, Long> messageIdToToken;


  public TransactionState(){
    outstandingEvents = new LinkedHashMap<>();
    messageIdToToken = new LinkedHashMap<>();
  }

  public void sent(byte[] token, int coapMessageId, long messageId, SubscribedEventManager subscription){
    long key = 0;
    if(token.length > 0) {
      key = tokenToLong(token);
      outstandingEvents.put(key, new TransactionContext(messageId, subscription));
    }
    messageIdToToken.put(coapMessageId, key);
  }

  public void ack(int coapMessageId, byte[] token){
    Long key = messageIdToToken.remove(coapMessageId);
    if(token.length > 0 || key != null) {
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
