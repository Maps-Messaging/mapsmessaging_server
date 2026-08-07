/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.security.uuid.UuidGenerator;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Outcome;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.engine.Delivery;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AmqpTransactionCoordinator {

  private static final int MAX_TRANSACTION_ID_LENGTH = 32;

  private final Map<String, TransactionContext> transactions;

  public AmqpTransactionCoordinator() {
    transactions = new LinkedHashMap<>();
  }

  public synchronized Binary declare(Session session) throws IOException {
    UUID uuid = UuidGenerator.getInstance().generate();
    byte[] transactionId = ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    String key = key(transactionId);
    Transaction transaction = session.startTransaction("amqp:" + key);
    transactions.put(key, new TransactionContext(transaction));
    return new Binary(transactionId);
  }

  public synchronized Transaction find(Binary transactionId) {
    TransactionContext context = findContext(transactionId);
    return context == null ? null : context.transaction;
  }

  public synchronized boolean enlist(Binary transactionId, Delivery delivery, SubscribedEventManager manager, long messageId, Outcome outcome) {
    TransactionContext context = findContext(transactionId);
    if (context == null) {
      return false;
    }
    context.pendingOutcomes.put(delivery, new PendingOutcome(delivery, manager, messageId, outcome));
    return true;
  }

  public synchronized boolean discharge(Binary transactionId, boolean fail) throws IOException {
    byte[] id = copy(transactionId);
    if (id == null) {
      return false;
    }
    String transactionKey = key(id);
    TransactionContext context = transactions.remove(transactionKey);
    if (context == null || context.isExpired()) {
      if (context != null) {
        expire(context);
      }
      return false;
    }

    IOException failure = null;
    try {
      if (fail) {
        context.transaction.abort();
        context.pendingOutcomes.values().forEach(PendingOutcome::rollback);
      } else {
        context.transaction.commit();
        context.pendingOutcomes.values().forEach(PendingOutcome::commit);
      }
    } catch (IOException e) {
      failure = e;
      context.pendingOutcomes.values().forEach(PendingOutcome::rollback);
    }
    try {
      context.transaction.close();
    } catch (IOException e) {
      if (failure == null) {
        failure = e;
      } else {
        failure.addSuppressed(e);
      }
    }
    if (failure != null) {
      throw failure;
    }
    return true;
  }

  public synchronized void close() throws IOException {
    IOException failure = null;
    for (TransactionContext context : transactions.values()) {
      try {
        context.transaction.close();
      } catch (IOException e) {
        if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
      context.pendingOutcomes.values().forEach(PendingOutcome::rollback);
    }
    transactions.clear();
    if (failure != null) {
      throw failure;
    }
  }

  public synchronized long expireTransactions(long now) {
    long nextExpiry = 0;
    Iterator<Map.Entry<String, TransactionContext>> iterator = transactions.entrySet().iterator();
    while (iterator.hasNext()) {
      TransactionContext context = iterator.next().getValue();
      long expiryTime = context.transaction.getExpiryTime();
      if (expiryTime <= now) {
        iterator.remove();
        expire(context);
      } else if (nextExpiry == 0 || expiryTime < nextExpiry) {
        nextExpiry = expiryTime;
      }
    }
    return nextExpiry;
  }

  public static byte[] copy(Binary binary) {
    if (binary == null || binary.getLength() <= 0 || binary.getLength() > MAX_TRANSACTION_ID_LENGTH) {
      return null;
    }
    return BinaryHelper.copy(binary);
  }

  private TransactionContext findContext(Binary transactionId) {
    byte[] id = copy(transactionId);
    if (id == null) {
      return null;
    }
    String transactionKey = key(id);
    TransactionContext context = transactions.get(transactionKey);
    if (context != null && context.isExpired()) {
      transactions.remove(transactionKey);
      expire(context);
      return null;
    }
    return context;
  }

  private void expire(TransactionContext context) {
    context.pendingOutcomes.values().forEach(PendingOutcome::rollback);
    try {
      context.transaction.close();
    } catch (IOException ignored) {
      // The global transaction manager also owns expiry cleanup.
    }
  }

  private static String key(byte[] transactionId) {
    return Base64.getEncoder().encodeToString(transactionId);
  }

  private static final class TransactionContext {

    private final Transaction transaction;
    private final Map<Delivery, PendingOutcome> pendingOutcomes;

    private TransactionContext(Transaction transaction) {
      this.transaction = transaction;
      pendingOutcomes = new IdentityHashMap<>();
    }

    private boolean isExpired() {
      return transaction.getExpiryTime() <= System.currentTimeMillis();
    }
  }

  private static final class PendingOutcome {

    private final Delivery delivery;
    private final SubscribedEventManager manager;
    private final long messageId;
    private final Outcome outcome;

    private PendingOutcome(Delivery delivery, SubscribedEventManager manager, long messageId, Outcome outcome) {
      this.delivery = delivery;
      this.manager = manager;
      this.messageId = messageId;
      this.outcome = outcome;
    }

    private void commit() {
      if (outcome instanceof Accepted || outcome instanceof Rejected) {
        manager.ackReceived(messageId);
      } else {
        manager.rollbackReceived(messageId);
      }
      delivery.settle();
    }

    private void rollback() {
      manager.rollbackReceived(messageId);
      delivery.settle();
    }
  }
}
