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

package io.mapsmessaging.engine;

import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * This class manages the timeouts of transactions, if we do not have timeouts it is a simple DOS attack to simply start transactions and publish messages with no commit or abort,
 * resulting in a build up of messages that can never be delivered.
 */

@SuppressWarnings("java:S6548") // yes it is a singleton
public class TransactionManager implements Runnable, Agent {

  private static class Holder {
    static final TransactionManager INSTANCE = new TransactionManager();
  }

  public static TransactionManager getInstance() {
    return Holder.INSTANCE;
  }

  @Setter
  @Getter
  private static long timeOutInterval = 100;

  @Setter
  @Getter
  private static long expiryTime = 3600000;

  private final Logger logger = LoggerFactory.getLogger(TransactionManager.class);
  /**
   * List containing the current active transactions
   */
  private final Map<String, Transaction> transactionList;

  private Future<?> schedule;


  /**
   * Adds a new transaction to be monitored to see if the time is exceeded
   *
   * @param transaction object that is currently active
   */
  public void add(@NonNull @NotNull Transaction transaction) {
    transactionList.put(transaction.getTransactionId(), transaction);
  }

  public Transaction find(@NonNull @NotNull String transactionId) {
    return transactionList.get(transactionId);
  }

  /**
   * Removes the Transaction object once it is completed
   *
   * @param transaction object to remove
   * @return true of the transaction object was removed from the list, else false if not found
   */
  public boolean remove(@NonNull @NotNull Transaction transaction) {
    return transactionList.remove(transaction.getTransactionId()) != null;
  }

  /**
   * Task function that simply scans to see if the transaction has timed out
   */
  @Override
  public void run() {
    logger.log(ServerLogMessages.TRANSACTION_MANAGER_SCANNING);
    long now = System.currentTimeMillis();
    List<Transaction> currentList = new ArrayList<>(transactionList.values());
    for (Transaction transaction : currentList) {
      if (transaction.getExpiryTime() < now) {
        logger.log(ServerLogMessages.TRANSACTION_MANAGER_TIMEOUT_DETECTED, transaction.getTransactionId());
        try {
          transaction.close();
        } catch (IOException e) {
          logger.log(ServerLogMessages.TRANSACTION_MANAGER_CLOSE_FAILED, e, transaction.getTransactionId());
        }
      }
    }
  }

  @Override
  public String getName() {
    return "Transaction Manager";
  }

  @Override
  public String getDescription() {
    return "Transaction Life-Cycle and persistence manager";
  }

  public void start() {
    if (schedule != null) {
      schedule.cancel(false);
    }
    schedule = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, timeOutInterval, timeOutInterval, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    if (schedule != null) {
      schedule.cancel(false);
    }
  }

  /**
   * Hidden constructor, since this is a singleton that simply manages the timeouts of transactions and cleans up any mess left over.
   */
  private TransactionManager() {
    transactionList = new ConcurrentSkipListMap<>();
    schedule = null;
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("Transaction List: " + transactionList.size());
    status.setStatus(Status.OK);
    return status;
  }


}
