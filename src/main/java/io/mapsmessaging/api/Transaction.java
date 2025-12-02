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

package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.engine.TransactionManager;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.security.authorisation.ProtectedResource;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Transaction class that maintains a list of destinations that this transaction has published messages to as well as the unique transaction ID and the internal id used to identify
 * this as a unique transaction
 */
public class Transaction {

  private static final String EXCEPTION_MESSAGE = "Transaction has already been completed";

  /**
   * Used to ensure every transaction is unique
   */
  private static final AtomicLong TRANSACTION_GENERATOR = new AtomicLong(1);

  /**
   * Protocol specific transaction id, specified by the protocol
   */
  private final String transactionId;

  /**
   * The time that this transaction will expire, in milliseconds
   * -- GETTER --
   *  The current time that this transaction will last before it is automatically closed
   *
   * @return time in milliseconds that this transaction will expire

   */
  @Getter
  private final long expiryTime;

  /**
   * Unique long used to identify this transaction
   */
  private final long internalId;

  /**
   * List of destinations that have had messages published to within the context of this transaction
   */
  private final Map<String, Destination> list;

  /**
   * Indicates that this transaction is now complete
   */
  private boolean complete;

  private final Identity identity;

  /**
   * Construct the transaction so it is ready to start receiving messages
   *
   * @param id protocol specific ID
   */
  public Transaction(@NonNull @NotNull String id, Identity identity) {
    complete = false;
    transactionId = id;
    this.identity = identity;
    internalId = TRANSACTION_GENERATOR.incrementAndGet();
    expiryTime = System.currentTimeMillis() + TransactionManager.getExpiryTime();
    list = new LinkedHashMap<>();
    TransactionManager.getInstance().add(this);
  }

  /**
   * If called before commit, then all messages will automatically be aborted and removed from all the destinations that have been published previously.
   *
   * @throws IOException if the abort call caused any IO errors. This is because it makes changes to the underlying files
   */
  public void close() throws IOException {
    // Schedule task to abort and delete all messages from this transaction across all destinations
    if (!complete) {
      abort();
    }
    TransactionManager.getInstance().remove(this);
  }

  /**
   * Aborts all messages published to this transaction, meaning that they are removed from the underlying resource and all reference to them are removed, freeing up resources.
   *
   * @throws IOException If, while, clearing resources any file system exceptions are raised
   */
  public void abort() throws IOException {
    if (complete) {
      throw new TransactionException(EXCEPTION_MESSAGE);
    }
    if (!list.isEmpty()) {
      // Schedule task to abort and delete all messages from this transaction across all destinations
      for (Destination destination : list.values()) {
        destination.destinationImpl.abort(internalId);
      }
      list.clear();
    }
    complete = true;
  }

  /**
   * Allows the messages previously published in this transaction to be visible to all the subscribers on the destinations. Meaning they will then be delivered to any subscription
   * that matches the destination and message
   *
   * @throws IOException If, while, enabling the message to be seen any file exceptions are raised
   */
  public void commit() throws IOException {
    if (complete) {
      throw new TransactionException(EXCEPTION_MESSAGE);
    }
    for (Destination destination : list.values()) {
      destination.destinationImpl.commit(internalId);
    }
    list.clear();
    complete = true;
  }

  /**
   * Adds a new message within this transaction to the specified destination. This message is stored within the destination but is not enabled to be delivered. This ensures that
   * the message can be stored within the destination and as such more likely that there would be no file exceptions during the commit phase since it is a simple state change
   * rather than disk I/O
   *
   * @param destination Destination that this message is bound to
   * @param message Message to store on the Destination
   * @throws IOException Is raised if unable to store the message to the specified destination
   */
  public void add(@NonNull @NotNull Destination destination, @NonNull @NotNull Message message) throws IOException {
    if (complete) {
      throw new TransactionException(EXCEPTION_MESSAGE);
    }
    if(AuthManager.getInstance().isAuthorisationEnabled()) {
      ProtectedResource resource = new ProtectedResource(destination.getResourceType().getName(), destination.getResourceType().getName(), null);
      if (!AuthManager.getInstance().canAccess(identity, ServerPermissions.PUBLISH, resource)) {
        throw new TransactionException("You don't have permission to start transactions on this resource");
      }
    }


    long delayed = message.getDelayed();
    if (delayed > 0) {
      message.setDelayed(delayed - System.currentTimeMillis());
    }
    if (!list.containsKey(destination.getFullyQualifiedNamespace())) {
      list.put(destination.getFullyQualifiedNamespace(), destination);
    }

    if(destination instanceof Schema){
      destination.storeMessage(message);
    }
    else {
      if(destination.destinationImpl.getSchema() != null) {
        // Ensure the schema is applied to the incoming message
        message.setSchemaId(destination.destinationImpl.getSchema().getUniqueId());
      }
      destination.destinationImpl.storeTransactionalMessage(internalId, message);
    }
  }

  /**
   * The transaction identifier supplied during the constructor
   *
   * @return the string supplied during the constructor
   */
  public @NonNull @NotNull String getTransactionId() {
    return transactionId;
  }

}
