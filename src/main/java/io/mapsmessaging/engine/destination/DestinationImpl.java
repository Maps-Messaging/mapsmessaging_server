/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.admin.DestinationJMX;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.delayed.DelayedMessageManager;
import io.mapsmessaging.engine.destination.delayed.TransactionalMessageManager;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.destination.subscription.Subscribable;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscriptionManager;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscriptionRegister;
import io.mapsmessaging.engine.destination.tasks.BulkRemoveMessageTask;
import io.mapsmessaging.engine.destination.tasks.DelayedStoreMessageTask;
import io.mapsmessaging.engine.destination.tasks.MessageDeliveryTask;
import io.mapsmessaging.engine.destination.tasks.NonDelayedStoreMessageTask;
import io.mapsmessaging.engine.destination.tasks.QueueBasedStoreMessageTask;
import io.mapsmessaging.engine.destination.tasks.RemoveMessageTask;
import io.mapsmessaging.engine.destination.tasks.TransactionalMessageProcessor;
import io.mapsmessaging.engine.resources.MemoryResource;
import io.mapsmessaging.engine.resources.Resource;
import io.mapsmessaging.engine.resources.ResourceFactory;
import io.mapsmessaging.engine.tasks.FutureResponse;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.utilities.threads.tasks.PriorityConcurrentTaskScheduler;
import io.mapsmessaging.utilities.threads.tasks.PriorityTaskScheduler;
import io.mapsmessaging.utilities.threads.tasks.SingleConcurrentTaskScheduler;
import io.mapsmessaging.utilities.threads.tasks.TaskScheduler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents a mechanism for clients to publish to a known point, subscribe to this point and the complex mechanisms around that, including
 * transactional publishing, transactional subscribing, delayed publishing, statistics etc. It does not perform any of these operations it is basically
 * a container for structures that do. As such this class provides the API for all functions around subscribing and publishing to a single destination. It does not
 * understand about other destinations or protocols etc, just what a single destination needs to do.
 */
public class DestinationImpl implements BaseDestination {

  //<editor-fold desc="Global static final fields used by all destinations">
  private static final int TASK_QUEUE_PRIORITY_SIZE = 2;

  public static final String RESOURCE_TASK_KEY = "ResourceAccessKey";
  public static final String SUBSCRIPTION_TASK_KEY = "SubscriptionAccessKey";
  public static final int PUBLISH_PRIORITY = 0;
  public static final int DELETE_PRIORITY = 1;
  public static final int RETRIEVE_PRIORITY = 1;
  //</editor-fold>

  //<editor-fold desc="Destination specific fields">
  protected final DestinationSubscriptionManager subscriptionManager;
  private final SharedSubscriptionRegister sharedSubscriptionRegistry;

  protected final DestinationJMX destinationJMXBean;

  private final PriorityTaskScheduler<Response> resourceTaskQueue;
  private final TaskScheduler<Response> subscriptionTaskQueue;

  private final DelayedMessageManager delayedMessageManager;
  private final TransactionalMessageManager transactionMessageManager;
  private final DestinationStats stats;
  private final Resource resource;
  private final DestinationType destinationType;

  private final String name;

  private volatile boolean closed;
  //</editor-fold>

  //<editor-fold desc="Constructors">
  /**
   * This constructor builds a destination from scratch, including all the meta data and other files required for a normal
   * running destination
   *
   * @param name a client unique name to use for the destination, typically supplied via an API
   * @param path the physical location to store the files for this destination
   * @param uuid a UUID that makes this destination a very unique entity
   * @param destinationType the type of resource that this destination represents
   * @throws IOException if, at anytime, the file system was unable to construct, read or write to the required files
   */
  public DestinationImpl( @NonNull @NotNull String name, @NonNull @NotNull  String path, @NonNull @NotNull  UUID uuid, @NonNull @NotNull DestinationType destinationType) throws IOException {
    this.name = name;
    resourceTaskQueue = new PriorityConcurrentTaskScheduler<>(RESOURCE_TASK_KEY, TASK_QUEUE_PRIORITY_SIZE);
    subscriptionTaskQueue = new SingleConcurrentTaskScheduler<>(SUBSCRIPTION_TASK_KEY);
    this.destinationType = destinationType;
    subscriptionManager = new DestinationSubscriptionManager(name);
    resource = ResourceFactory.getInstance().create(path, name, uuid, destinationType);
    stats = new DestinationStats();
    if (MessageDaemon.getInstance() != null) {
      destinationJMXBean = new DestinationJMX(this, resourceTaskQueue, subscriptionTaskQueue);
    } else {
      destinationJMXBean = null;
    }
    sharedSubscriptionRegistry = new SharedSubscriptionRegister();
    delayedMessageManager = DestinationStateManagerFactory.getInstance().createDelayed(this, true, "delayed");
    transactionMessageManager= DestinationStateManagerFactory.getInstance().createTransaction(this, true, "transactions");
    closed = false;
  }

  /**
   * This constructor is used during the restart of the server where a resource has already been found and loaded, we now continue with the
   * loading of the other structures required for the destionation to function correctly
   *
   * @param resource preloaded resource found during restart
   * @param destinationType the resource type detected during the reload
   * @throws IOException if, at any point, the underlying file structures are corrupt or unable to be used
   */
  public DestinationImpl( @NonNull @NotNull Resource resource, @NonNull @NotNull DestinationType destinationType) throws IOException {
    this.name = resource.getMappedName();
    resourceTaskQueue = new PriorityConcurrentTaskScheduler<>(RESOURCE_TASK_KEY, TASK_QUEUE_PRIORITY_SIZE);
    subscriptionTaskQueue = new SingleConcurrentTaskScheduler<>(SUBSCRIPTION_TASK_KEY);
    this.destinationType = destinationType;
    subscriptionManager = new DestinationSubscriptionManager(name);
    this.resource = resource;
    stats = new DestinationStats();
    if (MessageDaemon.getInstance() != null) {
      destinationJMXBean = new DestinationJMX(this, resourceTaskQueue, subscriptionTaskQueue);
    } else {
      destinationJMXBean = null;
    }
    sharedSubscriptionRegistry = new SharedSubscriptionRegister();

    // Delayed Messages are automatically dealt with once the structure has been reloaded
    delayedMessageManager = DestinationStateManagerFactory.getInstance().createDelayed(this, true, "delayed");

    transactionMessageManager= DestinationStateManagerFactory.getInstance().createTransaction(this, true, "transactions");
    rollbackTransactionsOnReload();
    closed = false;
  }

  //
  // We now need to rollback all events found in the transaction manager
  //
  private void rollbackTransactionsOnReload() throws IOException {
    List<Long> transactionIds = transactionMessageManager.getTransactions();
    for(Long transaction:transactionIds){
      abort(transaction);
    }
  }

  /**
   * This constructor is only used for internal topics, typically $SYS topics and as such have a limited feature set, like no delayed publishing, no transactional publishing
   * and things like that, they are simply in memory resources that manage the system statistics
   *
    * @param name of the destination
   * @param destinationType the type of the destination
   */
  public DestinationImpl( @NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType) {
    this.name = name;
    resourceTaskQueue = new PriorityConcurrentTaskScheduler<>(RESOURCE_TASK_KEY, TASK_QUEUE_PRIORITY_SIZE);
    subscriptionTaskQueue = new SingleConcurrentTaskScheduler<>(SUBSCRIPTION_TASK_KEY);
    this.destinationType = destinationType;
    subscriptionManager = new DestinationSubscriptionManager(name);
    resource = new MemoryResource(name);
    stats = new DestinationStats();
    if (MessageDaemon.getInstance() != null) {
      destinationJMXBean = new DestinationJMX(this, resourceTaskQueue, subscriptionTaskQueue);
    } else {
      destinationJMXBean = null;
    }
    sharedSubscriptionRegistry = new SharedSubscriptionRegister();
    delayedMessageManager = null;
    transactionMessageManager= null;
    closed = false;
  }
  //</editor-fold>

  //<editor-fold desc="Shutdown functions">
  public void close() throws IOException {
    closed = true;
    resource.stop();
    if(delayedMessageManager != null) {
      delayedMessageManager.close();
    }
    if(transactionMessageManager != null) {
      transactionMessageManager.close();
    }
  }

  public void stopSubscriptions(){
    subscriptionTaskQueue.shutdown(true);
  }

  public void pauseClientRequests(){
    subscriptionManager.pause();
    resourceTaskQueue.shutdown(true);
  }

  public void delete() throws IOException {
    if(!closed) {
      closed = true;
      subscriptionManager.close();
      transactionMessageManager.delete();
      delayedMessageManager.delete();
      resource.delete();
      if (destinationJMXBean != null) {
        destinationJMXBean.close();
      }
      File location = new File(getPhysicalLocation());
      deleteFile(location);
    }
  }

  public static void deleteFile(File directoryToBeDeleted) throws IOException {
    StringBuilder failedFiles = new StringBuilder();
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      List<File> failed = new ArrayList<>();
      for (File file : allContents) {
        try {
          Files.delete(file.toPath());
        } catch (IOException e) {
          failed.add(file);
        }
      }
      // Try once more
      for (File file : failed) {
        try {
          Files.delete(file.toPath());
        }
        catch (IOException io){
          failedFiles.append(file.getAbsolutePath()).append(",");
        }
      }
    }
    try {
      Files.delete(directoryToBeDeleted.toPath());
    } catch (IOException e) {
      failedFiles.append(directoryToBeDeleted.getAbsolutePath()).append(",");
      throw new IOException("Failed to delete the following files: "+failedFiles, e);
    }
  }

  public boolean isClosed() {
    return closed;
  }

  //</editor-fold>

  //<editor-fold desc="Get functions to query the destination">

  /**
   * If the underlying resource is file based or memory only based
   *
   * @return True if not using the MemoryResource type
   */
  public boolean isPersistent() {
    return !(resource instanceof MemoryResource);
  }

  /**
   * This returns the user supplied name for the destination
   *
   * @return String name of the destination
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the resource type for this destination
   *
   * @return ResourceType for this destination
   */
  public DestinationType getResourceType(){
    return destinationType;
  }

  /**
   * All destinations have a physical location within the file system, this can be configured based on the namespace. This returns the
   * root directory for this specific destination
   * @return File path for this destination
   */
  public String getPhysicalLocation() {
    return resource.getName();
  }

  /**
   * All destinations have the ability to store a specific message Id as a "retained" message, meaning on new subscriptions this message
   * is delivered first. This function returns the message Id that represents the current "retained" message
   *
   * @return message Id of the retained message or -1 indicating no retained message
   */
  public long getRetainedIdentifier() {
    return resource.getRetainedIdentifier();
  }

  /**
   * This function returns the JMX type path so that any child JMX Beans can use to add to it
   *
   * @return This destinations JMX path
   */
  public List<String> getTypePath() {
    if (destinationJMXBean != null) {
      return destinationJMXBean.getTypePath();
    } else {
      return new ArrayList<>();
    }
  }

  /**
   * Returns the stats object for this destination. All metrics about this destination are
   * maintained in this class
   *
   * @return DestinationStats for this instance
   */
  public DestinationStats getStats(){
    return stats;
  }

  /**
   * There are 2 types of messages, ones that are visible to subscribers, normal message flow, and then ones that we have but are not
   * able to release to the clients yet. This could be because they are delayed publishes or they are part of a transaction that has
   * yet to be committed
   *
   * @return the current number of messages that are waiting a state change so they can be delivered to any subscribers
   */
  public long getDelayedMessages(){
    if(delayedMessageManager != null) {
      return delayedMessageManager.size();
    }
    return 0;
  }

  /**
   * There are 2 types of messages, ones that are visible to subscribers, normal message flow, and then ones that we have but are not
   * able to release to the clients yet. This could be because they are delayed publishes or they are part of a transaction that has
   * yet to be committed
   *
   * @return the current number of messages that are waiting a state change so they can be delivered to any subscribers
   */
  public long getPendingTransactions(){
    if(transactionMessageManager != null) {
      return transactionMessageManager.size();
    }
    return 0;
  }

  /**
   * Returns the total number of messages currently stored, this includes both types of messages
   *
   * @return number of messages currently at rest in this destination
   * @throws IOException If unable to get the size from the underlying resource implementation
   */
  public long getStoredMessages() throws IOException {
    return resource.size();
  }

  //</editor-fold>

  //<editor-fold desc="Subscription management APIs">

  /**
   * Adds a subscription to this destination
   *
   * @param subscription subscription object to be added
   */
  public void addSubscription( @NonNull @NotNull Subscription subscription) {
    stats.subscriptionAdded();
    subscriptionManager.put(subscription.getSessionId(), subscription);
  }

  /**
   * Removes the indicated subscription ID and will delete any messages that are held by this subscription and are not required
   * by any other subscription. Once this is completed messages are, potentially, gone forever.
   *
   * @param subscriptionId  Subscription identifier to remove
   * @return the subscription that was removed
   */
  public Subscribable removeSubscription( @NonNull @NotNull String subscriptionId) throws IOException {
    stats.subscriptionRemoved();
    Subscribable subscription = subscriptionManager.remove(subscriptionId);
    if(subscription != null) {
      Queue<Long> eventQueue = subscription.getAllAtRest();
      // We now need to filter this queue against other subscriptions. If they have interest we MUST NOT remove the event
      // If no other subscriptions have any interest in the event, we must remove it
      if(!eventQueue.isEmpty()) {
        eventQueue = subscriptionManager.scanForInterest(eventQueue);
        if (!eventQueue.isEmpty()) {
          stats.getStoredMessageAverages().subtract(eventQueue.size());
          handleTask(new BulkRemoveMessageTask(this, eventQueue));
        }
      }
    }
    return subscription;
  }
  //</editor-fold>

  //<editor-fold desc="Message delivery and completion APIs">

  /**
   * Retrieves a message from this destination that matches the unique message Id.
   *
   * @param messageId The message Id of the message to retrieve
   * @return The Message that matches the ID or NULL if no such message is found
   * @throws IOException If, at any point, there are File I/O exceptions when trying to retrieve the message
   */
  public @Nullable Message getMessage(long messageId) throws IOException {
    long nano = System.nanoTime();
    Message message = resource.get(messageId);
    if (message != null) {
      nano = (System.nanoTime() - nano)/1000;
      getStats().getReadTimeAverages().add(nano);
      long expiry = message.getExpiry();
      if (expiry > 0 && expiry < System.currentTimeMillis()) {
        submit(new RemoveMessageTask(this, messageId), RETRIEVE_PRIORITY);
        subscriptionManager.expired(messageId);
        message = null;
        stats.expiredMessage();
      }
      else{
        stats.retrievedMessage();
      }
    }
    return message;
  }

  /**
   * Called when the completion task for the message delivery is called, we than scan to see if any other subscriptions require the message, if not, then
   * we simply schedule a removal of the message from the destination
   *
   * @param messageId that the delivery is complete
   */
  public void complete(long messageId) {
    if (!subscriptionManager.hasInterest(messageId)) {
      stats.removedMessage();
      submit(new RemoveMessageTask(this, messageId), DELETE_PRIORITY);
    }
    stats.deliveredMessage();
  }
  //</editor-fold>

  //<editor-fold desc="Non transactional publishing">

  /**
   * Queue a task to store the supplied message directly to the resource. If the message is indicated to be delivered in the future
   * then a delayed task is run to ensure the correct handling of the event, else it is handled immediately
   *
   * @param message to store
   * @return the number of subscribers that are interested in this message
   * @throws IOException If, at any point, a file I/O exception was raised while storing this message
   */
  public int storeMessage( @NonNull @NotNull Message message) throws IOException {
    Callable<Response> task;
    if(message.getDelayed() > 0 && delayedMessageManager != null){
      task = new DelayedStoreMessageTask(this, message, delayedMessageManager, message.getDelayed());
    }
    else {
      if(getResourceType().isTopic()) {
        task = new NonDelayedStoreMessageTask(this, subscriptionManager, message);
      }
      else{
        task = new QueueBasedStoreMessageTask(this, subscriptionManager, message);
      }
    }
    return handleTask(task);
  }
  //</editor-fold>

  //<editor-fold desc="Transactional publishing APIs">

  /**
   * Removes ALL messages that this transaction Id references. Typically called if the client disconnected before a commit was called or if another exception
   * was raised during the processing of another part of the transaction
   *
   * @param transactionId This is the internal transaction id that we are to use
   * @throws IOException If the file system raises any File I/O exceptions during the operation
   */
  public void abort(long transactionId) throws IOException {
    submit(new BulkRemoveMessageTask(this, transactionMessageManager.removeBucket(transactionId)), DELETE_PRIORITY);
  }

  /**
   * Commits all messages that are referenced by this transaction id. Basically this function makes the messages visible to any
   * subscribers and can now be delivered.
   *
   * @param transactionId The transaction id to use to release the messages
   * @throws IOException If the file system raises any File I/O exceptions during the operation
   */
  public void commit(long transactionId) throws IOException {
    submit(new TransactionalMessageProcessor(this, subscriptionManager, transactionMessageManager, transactionId));
  }

  /**
   * Adds a new message to the resource and registers the message with the transaction manager using the transaction Id
   *
   * @param transactionId The unique transaction Id to register this message with
   * @param message The message to store but not forward to subscribers yet
   * @throws IOException If the file system raises any File I/O exceptions during the operation
   */
  public void storeTransactionalMessage(long transactionId, @NonNull @NotNull Message message) throws IOException {
    Callable<Response> task;
    if(transactionMessageManager != null){
      task = new DelayedStoreMessageTask(this, message, transactionMessageManager, transactionId);
    }
    else {
      task = new NonDelayedStoreMessageTask(this, subscriptionManager, message);
    }
    handleTask(task);
  }
  //</editor-fold>

  //<editor-fold desc="Specific shred subscription functions">
  public void addShareRegistry ( @NonNull @NotNull SharedSubscriptionManager shareRegistry) {
    sharedSubscriptionRegistry.add(shareRegistry.getName(), shareRegistry);
  }

  public SharedSubscriptionManager findShareRegister( @NonNull @NotNull String sharedName) {
    return sharedSubscriptionRegistry.get(sharedName);
  }

  public void delShareRegistry ( @NonNull @NotNull String sharedName) {
    sharedSubscriptionRegistry.del(sharedName);
  }
  //</editor-fold>

  //<editor-fold desc="Pre-delivery functions for messages that we have but can not be distributed to clients yet">
  public DelayedMessageManager getDelayedStatus() {
    return delayedMessageManager;
  }

  public TransactionalMessageManager getTransactionManager() {
    return transactionMessageManager;
  }
  //</editor-fold>

  //<editor-fold desc="Task Queue functions">

  /**
   * This function will submit a task to deliver events to a subscriber but only if the subscriber are able to receive events and there are messages
   *
   * @param subscription to initiate the scan on
   */
  public void scanForDelivery( @NonNull @NotNull Subscription subscription) {
    MessageDeliveryTask deliveryTask = new MessageDeliveryTask(subscription);
    submit(deliveryTask);
  }

  /**
   * Submits a task to update the subscription structures, this could be an add subscription, remove, ack, rollback etc. Basically
   * anything that updates the state of a subscription
   *
   * @param task to add on the subscription task queue.
   * @return the future response of the task that has been queued
   */
  public FutureTask<Response> submit( @NonNull @NotNull Callable<Response> task){
    FutureTask<Response> future = new FutureTask<>(task);
    subscriptionTaskQueue.addTask(future);
    return future;
  }

  /**
   * Submits a generic task to update the resource of this destination, typically Message addition or deletion are queued here since there
   * is no subscription structure changes they can occur in parallel to the subscription queue
   *
   * @param task to queue on the resource task queue
   * @param priority the priority to process this task, some tasks can be done before other tasks
   * @return the future response of the task that has been queued
   */
  public FutureTask<Response> submit( @NonNull @NotNull  Callable<Response> task, int priority){
    FutureTask<Response> future = new FutureTask<>(task);
    resourceTaskQueue.addTask(future, priority);
    return future;
  }

  /**
   * Internal helper function that manages the publishing of events into the resource
   *
   * @param task to submit to the task queue
   * @return the number of times the message was delivered to subscribers
   * @throws IOException if, at any point, an exception was raised because of file I/O exceptions
   */
  public int handleTask(@NonNull @NotNull Callable<Response> task)throws IOException {
    Future<Response> future = submit(task, PUBLISH_PRIORITY);
    try {
      Response response = future.get(60, TimeUnit.SECONDS);
      if (response instanceof LongResponse) {
        return (int) ((LongResponse) response).getResponse();
      } else if (response instanceof FutureResponse) {
        response = ((FutureResponse) response).getResponse().get();
        if (response instanceof LongResponse) {
          return (int) ((LongResponse) response).getResponse();
        }
      }
      return 0;
    } catch (CancellationException exception){
      // We have a cancelled task..
      if(!isClosed()){
        throw exception;
      }
      return 0;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Thread interrupted");
    } catch (TimeoutException timeout) {
      throw new IOException(timeout.getCause());
    } catch (ExecutionException e) {
      throw new IOException(e.getMessage(), e);
    }
  }
  //</editor-fold>

  //<editor-fold desc="Resource operations">
  /**
   * Physically removes a message from the resource, should not be used directly
   *
   * @param messageId The message id to remove from the resource
   * @throws IOException If any File I/O exceptions raised during the operation
   */
  public void removeMessage(long messageId) throws IOException {
    long nano = System.nanoTime();
    resource.remove(messageId);
    nano = (System.nanoTime() - nano)/1000; // Make it micro seconds
    getStats().getDeleteTimeAverages().add(nano);
  }

  /**
   * Adds a message to the resource, should not be used directly
   *
   * @param message The message to add to the resource
   * @throws IOException If any File I/O exceptions raised during the operation
   */
  public void addMessage(Message message) throws IOException {
    long nano = System.nanoTime();
    resource.add(message);
    nano = (System.nanoTime() - nano)/1000;
    getStats().getWriteTimeAverages().add(nano);
  }

  public DestinationSubscription getSubscription(String subscriptionName) {
    Subscribable subscribable = subscriptionManager.getSubscription(subscriptionName);
    if(subscribable instanceof DestinationSubscription){
      return (DestinationSubscription)subscribable;
    }
    return null;
  }

  //</editor-fold>

}
