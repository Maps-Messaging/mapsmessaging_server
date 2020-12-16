/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.Transaction;
import org.maps.utilities.threads.SimpleTaskScheduler;

/**
 * This class manages the timeouts of transactions, if we do not have timeouts it is a simple
 * DOS attack to simply start transactions and publish messages with no commit or abort, resulting
 * in a build up of messages that can never be delivered.
 */
public class TransactionManager implements Runnable {

  private static long timeOutInterval = 100;
  private static long expiryTime = 3600000;

  public static void setTimeOutInterval(long timeout){
    timeOutInterval = timeout;
  }

  public static long getExpiryTime() {
    return expiryTime;
  }
  public static void setExpiryTime(long expiry) {
    expiryTime = expiry;
  }

  private static final TransactionManager instance = new TransactionManager();
  public static TransactionManager getInstance(){
    return instance;
  }

  private final Logger logger = LoggerFactory.getLogger(TransactionManager.class);
  /**
   * List containing the current active transactions
   */
  private final Map<String, Transaction> transactionList;

  private Future<Runnable> schedule;


  /**
   * Adds a new transaction to be monitored to see if the time is exceeded
   *
   * @param transaction object that is currently active
   */
  public synchronized void add(@NotNull Transaction transaction){
    transactionList.put(transaction.getTransactionId(), transaction);
  }

  public synchronized Transaction find(@NotNull String transactionId){
    return transactionList.get(transactionId);
  }

  /**
   * Removes the Transaction object once it is completed
   *
   * @param transaction object to remove
   * @return true of the transaction object was removed from the list, else false if not found
   */
  public synchronized boolean remove(@NotNull Transaction transaction){
    return transactionList.remove(transaction.getTransactionId()) != null;
  }

  /**
   * Task function that simply scans to see if the transaction has timed out
   */
  @Override
  public synchronized void run() {
    logger.log(LogMessages.TRANSACTION_MANAGER_SCANNING);
    long now = System.currentTimeMillis();
    List<Transaction> currentList = new ArrayList<>(transactionList.values());
    for(Transaction transaction:currentList){
      if(transaction.getExpiryTime() < now){
        logger.log(LogMessages.TRANSACTION_MANAGER_TIMEOUT_DETECTED, transaction.getTransactionId());
        try {
          transaction.close();
        } catch (IOException e) {
          logger.log(LogMessages.TRANSACTION_MANAGER_CLOSE_FAILED, e, transaction.getTransactionId());
        }
      }
    }
  }

  public synchronized void start(){
    if(schedule != null){
      schedule.cancel(true);
    }
    schedule = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, timeOutInterval, timeOutInterval, TimeUnit.MILLISECONDS);
  }

  public synchronized void stop(){
    if(schedule != null){
      schedule.cancel(true);
    }
 }

  /**
   * Hidden constructor, since this is a singleton that simply manages the timeouts of transactions and cleans up any mess
   * left over.
   */
  private TransactionManager(){
    transactionList = new LinkedHashMap<>();
    schedule = null;
  }

}
