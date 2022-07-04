/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.admin.SubscriptionControllerJMX;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.DestinationFilter;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManagerListener;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.engine.destination.subscription.tasks.MetricsSubscriptionTask;
import io.mapsmessaging.engine.destination.subscription.tasks.SchemaSubscriptionTask;
import io.mapsmessaging.engine.destination.subscription.tasks.SubscriptionTask;
import io.mapsmessaging.engine.destination.subscription.tasks.UnsubscribeTask;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.tasks.ListResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

/**
 * This class simply manages the subscription to destination mapping. It also manages the wildcard subscriptions, the overlap between wildcard subscriptions and simple
 * subscriptions, the difference between queues, topics, shared subscriptions and filtered subscriptions.
 */
public class SubscriptionController implements DestinationManagerListener {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

  private final DestinationFactory destinationManager;
  private final String sessionId;
  private final boolean isPersistent;

  private final SubscriptionControllerJMX subscriptionControllerJMX;

  private final Map<String, SubscriptionContext> contextMap;

  //
  // Structure of client based subscriptions mapped to a list of destinations
  //
  private final Map<String, DestinationSet> subscriptions;

  //
  // Flat list of subscriptions to destinations
  //
  private final Map<DestinationImpl, Subscription> activeSubscriptions;
  private final Map<DestinationImpl, Subscription> schemaSubscriptions;


  //
  // Session represents the remote client
  //
  private SessionImpl sessionImpl;
  private Future<?> schedule;

  public SubscriptionController(SessionContext sessionContext, DestinationFactory destinationManager, Map<String, SubscriptionContext> contextMap) {
    this.sessionId = sessionContext.getId();
    this.destinationManager = destinationManager;
    this.contextMap = contextMap;
    subscriptions = new ConcurrentHashMap<>();
    destinationManager.addListener(this);
    activeSubscriptions = new LinkedHashMap<>();
    schemaSubscriptions = new LinkedHashMap<>();
    isPersistent = sessionContext.isPersistentSession();
    subscriptionControllerJMX = new SubscriptionControllerJMX(this);
  }

  public SubscriptionController(String sessionId, DestinationFactory destinationManager, Map<String, SubscriptionContext> contextMap) {
    this.sessionId = sessionId;
    this.destinationManager = destinationManager;
    this.contextMap = contextMap;
    subscriptions = new LinkedHashMap<>();
    destinationManager.addListener(this);
    activeSubscriptions = new LinkedHashMap<>();
    schemaSubscriptions = new LinkedHashMap<>();
    isPersistent = true;
    int counter = 0;
    int total = contextMap.size();
    for (SubscriptionContext context : contextMap.values()) {
      try {
        addSubscription(context, true);
        counter++;
        logger.log(ServerLogMessages.SUBSCRIPTION_MGR_RELOAD, sessionId, context.getFilter(), counter, total);
      } catch (IOException e) {
        logger.log(ServerLogMessages.SUBSCRIPTION_MGR_FAILED, sessionId, context.getFilter(), e);
      }
    }
    subscriptionControllerJMX = new SubscriptionControllerJMX(this);
  }

  public void close() {
    logger.log(ServerLogMessages.SUBSCRIPTION_MGR_CLOSE, sessionId);
    destinationManager.removeListener(this);
    List<Subscription> closeList = new ArrayList<>(activeSubscriptions.values());
    subscriptions.clear();
    activeSubscriptions.clear();
    for (Subscription subscription : closeList) {
      try {
        subscription.close();
      } catch (IOException e) {
        logger.log(ServerLogMessages.SUBSCRIPTION_MGR_CLOSE_SUB_ERROR, e);
      }
    }

    closeList = new ArrayList<>(schemaSubscriptions.values());
    for (Subscription subscription : closeList) {
      try {
        subscription.close();
      } catch (IOException e) {
        logger.log(ServerLogMessages.SUBSCRIPTION_MGR_CLOSE_SUB_ERROR, e);
      }
    }
    contextMap.clear();
    subscriptionControllerJMX.close();
  }

  public boolean isPersistent() {
    return isPersistent;
  }

  public boolean isHibernating() {
    return sessionImpl == null;
  }


  public String getSessionId() {
    return sessionId;
  }

  /**
   * Simply add an incoming subscription, this will create a new subscription or an update
   *
   * @param context new subscription context to add
   * @return true if a change to subscription occurred
   *
   * @throws IOException if the add subscription failed, typically this is fatal since it means the server can not allocate resources
   */
  public SubscribedEventManager addSubscription(SubscriptionContext context) throws IOException {
    return addSubscription(context, false);
  }

  @SneakyThrows
  private SubscribedEventManager addSubscription(SubscriptionContext context, boolean isReload) throws IOException {
    SubscribedEventManager subscription = null;
    String filter = context.getFilter();
    if (filter.equals("test/nosubscribe")) {
      throw new IOException("Not authorised to access topic");
    }
    if (!subscriptions.containsKey(filter)) {
      if (!context.containsWildcard()) {
        CompletableFuture<DestinationImpl> future = destinationManager.findOrCreate(filter);
        if(future == null && filter.toLowerCase().startsWith("$sys")){
          future = destinationManager.find("$SYS/notImplemented");
        }
        if(future != null) future.get();
      }
      DestinationFilter destinationFilter = name -> DestinationSet.matches(context, name);
      DestinationSet destinationSet = new DestinationSet(context, destinationManager.get(destinationFilter));
      subscriptions.put(context.getAlias(), destinationSet);
      //
      // Now compare the active subscription destinations with the ones in this new subscriptionSet
      //
      if(!destinationSet.isEmpty()) {
        subscription = processSubscriptions(context, destinationSet, isReload);
      }
    } else {

      // We have received a subscription for an already registered subscription!!!
      delSubscription(filter);
      context.setReplaced(true);
      return addSubscription(context);
    }
    if (!isReload) {
      contextMap.put(context.getAlias(), context);
    }
    return subscription;
  }

  private SubscribedEventManager processSubscriptions(SubscriptionContext context, DestinationSet destinationSet, boolean isReload) throws IOException {
    AtomicLong counter = new AtomicLong(destinationSet.size());
    SubscribedEventManager subscription = null;
    ListResponse responses = new ListResponse();
    Future<Response> future;
    for (DestinationImpl destinationImpl : destinationSet) {
      // Lets queue the subscription requests for each destination
      future = scheduleSubscription(context, destinationImpl, counter);
      if (!isReload) {
        responses.addResponse(future);
      }
    }
    if (!isReload) {
      try {
        List<Response> response = responses.getResponse();
        if(!response.isEmpty()){
          subscription = ((SubscriptionResponse) response.get(0)).getSubscription();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
      } catch (ExecutionException e) {
        throw new IOException(e);
      }
    }
    return subscription;
  }

  /**
   * Now scan the destinations that this subscription had and remove any context, if its the last context then we need to delete and close the subscription and all data
   * @param id the specific subscription id used to create the subscription, could be the destination name or an alias
   * @return true if there was such a subscription to start with else false if unable to locate the subscription
   */
  public boolean delSubscription(String id) {
    if(id.startsWith("$schema")){
      String destinationName = id.substring("$schema".length()+1);
      for(Entry<DestinationImpl, Subscription> entry:schemaSubscriptions.entrySet()){
        if(entry.getKey().getFullyQualifiedNamespace().equals(destinationName)){
          schemaSubscriptions.remove(entry.getKey());
          return true;
        }
      }
    }
    DestinationSet destinationSet = subscriptions.remove(id);
    if (destinationSet != null) {
      handleUnsubscribeSet(id, destinationSet);
      return true;
    }

    return false;
  }


  private void handleUnsubscribeSet(String id, DestinationSet destinationSet){
    contextMap.remove(id);
    List<DestinationImpl> lostInterest = new ArrayList<>(destinationSet);
    AtomicLong counter = new AtomicLong(lostInterest.size());
    for (DestinationImpl destinationImpl : lostInterest) {
      UnsubscribeTask task = new UnsubscribeTask(this, destinationImpl,  destinationSet, counter);
      if(destinationImpl.submit(task).isCancelled()){
        counter.decrementAndGet();
      }
    }
    int timeout = 1000;
    while (counter.get() > 0 && timeout > 0) {
      LockSupport.parkNanos(10000000);
      timeout--;
    }
  }


  /**
   * Called when a new destination is created, we see if any wildcards match the new destination
   *
   */
  @Override
  public void created(DestinationImpl destinationImpl) {
    List<SubscriptionContext> interested = new ArrayList<>();
    List<DestinationSet> subscribeList = new ArrayList<>(subscriptions.values());
    for (DestinationSet subscription : subscribeList) {
      if (subscription.add(destinationImpl)) {
        interested.add(subscription.getContext());
      }
    }

    //
    // OK we have interest and maybe more than one, lucky that the SubscriptionContext is sorted
    // by QoS
    //
    if (!interested.isEmpty()) {
      try {
        createSubscription(interested.get(0), destinationImpl);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * It is assumed that the deletion of the destination and its resource that ALL persistent data is deleted We simply clean up our structure here
   */
  @Override
  public void deleted(DestinationImpl destinationImpl) {
    if (activeSubscriptions.containsKey(destinationImpl)) {
      //
      // Create a list of destination sets that have this destination in its mapping
      //
      List<DestinationSet> interested = new ArrayList<>();
      for (DestinationSet destinationSet : subscriptions.values()) {
        if (destinationSet.contains(destinationImpl)) {
          interested.add(destinationSet);
        }
      }

      //
      // Now lets see if there is now no interest and clear out any subscription
      //
      if (!interested.isEmpty()) {
        for (DestinationSet destinationSet : interested) {
          clearStructure(destinationImpl, destinationSet);
        }
      }
      activeSubscriptions.remove(destinationImpl);
    }
  }

  public void hibernateAll() {
    if (sessionImpl != null) {
      sessionImpl = null;
      List<Subscription> active = new ArrayList<>(activeSubscriptions.values());
      for (Subscription subscription : active) {
        subscription.hibernate();
      }
    }
  }

  public void wake(SessionImpl sessionImpl){
    if (schedule != null) {
      schedule.cancel(false);
    }
    if (this.sessionImpl == null) {
      this.sessionImpl = sessionImpl;
    }
  }

  public SubscribedEventManager wake(SessionImpl sessionImpl, DestinationImpl destination){
    for (Subscription subscription : activeSubscriptions.values()) {
      if(subscription.getContext() != null && subscription.getContext().getFilter().equals(destination.getFullyQualifiedNamespace())){
        subscription.wakeUp(sessionImpl);
        if (subscription instanceof DestinationSubscription && subscription.getContext().getRetainHandler().equals(RetainHandler.SEND_IF_NEW)) {
          queueRetainedMessage(((DestinationSubscription) subscription).getDestinationImpl(), subscription);
        }
        return subscription;
      }
    }
    return null;
  }


  public void wakeAll(SessionImpl sessionImpl) {
    wake(sessionImpl);
    if (this.sessionImpl != null) {
      for (Subscription subscription : activeSubscriptions.values()) {
        subscription.wakeUp(this.sessionImpl);
        if (subscription instanceof DestinationSubscription
            && subscription.getContext() != null && subscription.getContext().getRetainHandler().equals(RetainHandler.SEND_IF_NEW)) {
          queueRetainedMessage(((DestinationSubscription) subscription).getDestinationImpl(), subscription);
        }
      }
    }
  }

  public void queueRetainedMessage(DestinationImpl destinationImpl, Subscription subscription) {
    if (subscription.getContext() != null && !subscription.getContext().getRetainHandler().equals(RetainHandler.DO_NOT_SEND)) {
      long retainedId = destinationImpl.getRetainedIdentifier();
      if (retainedId >= 0) {
        subscription.register(retainedId);
      }
    }
  }

  private Future<Response> scheduleSubscription(SubscriptionContext context, DestinationImpl destinationImpl, AtomicLong counter){
    SubscriptionTask task = null;
    switch (context.getDestinationMode()){
      case SCHEMA:
        task = new SchemaSubscriptionTask(this, context, destinationImpl, counter);
        break;
      case METRICS:
        task = new MetricsSubscriptionTask(this, context, destinationImpl, counter);
        break;
      case NORMAL:
      default:
        task = new SubscriptionTask(this, context, destinationImpl, counter);
        break;
    }
    return destinationImpl.submit(task);
  }

  public Subscription createSchemaSubscription( @NonNull @NotNull SubscriptionContext context, @NonNull @NotNull DestinationImpl destinationImpl) throws IOException {
    SubscriptionBuilder builder = SubscriptionFactory.getInstance().getBuilder(destinationImpl, context, false); // Schema subscriptions have no persistence
    Subscription subscription = builder.construct(sessionImpl, sessionId);
    schemaSubscriptions.put(destinationImpl, subscription);
    return subscription;
  }

  public Subscription createSubscription( @NonNull @NotNull SubscriptionContext context, @NonNull @NotNull DestinationImpl destinationImpl) throws IOException {
    SubscriptionBuilder builder = SubscriptionFactory.getInstance().getBuilder(destinationImpl, context, isPersistent);
    Subscription subscription = builder.construct(sessionImpl, sessionId);

    if (!context.isReplaced() || context.getRetainHandler().equals(RetainHandler.SEND_ALWAYS)) {
      queueRetainedMessage(destinationImpl, subscription);
    }
    activeSubscriptions.put(destinationImpl, subscription);
    return subscription;
  }

  public Subscription createBrowserSubscription( @NonNull @NotNull SubscriptionContext context, @NonNull @NotNull DestinationSubscription parent, @NonNull @NotNull DestinationImpl destinationImpl) throws IOException {
    SubscriptionBuilder builder = SubscriptionFactory.getInstance().getBrowserBuilder(destinationImpl, context, parent);
    Subscription subscription = builder.construct(sessionImpl, sessionId);
    activeSubscriptions.put(destinationImpl, subscription);
    return subscription;
  }

  /**
   * Clean up the structure on a deletion of a destination, we keep the wildcard subscriptions since they may be needed in future
   */
  private void clearStructure(DestinationImpl destinationImpl, DestinationSet destinationSet) {
    if (destinationSet.remove(destinationImpl)
        && destinationSet.isEmpty()
        && !destinationSet.getContext().containsWildcard()) {
      subscriptions.remove(destinationSet.getContext().getFilter());
    }
  }

  public Future<?> getTimeout() {
    return schedule;
  }

  public void setTimeout(Future<?> schedule) {
    if(this.schedule != null && !this.schedule.isCancelled()) {
      this.schedule.cancel(true);
    }
    this.schedule = schedule;
  }

  public Map<String, SubscriptionContext> getSubscriptions() {
    return new LinkedHashMap<>(contextMap);
  }

  public Subscription get(DestinationImpl destination) {
    return activeSubscriptions.get(destination);
  }

  public Subscription getSchema(DestinationImpl destination) {
    return schemaSubscriptions.get(destination);
  }

  public Subscription remove(DestinationImpl destination) {
    return activeSubscriptions.remove(destination);
  }

  public void hibernateSubscription(String subscriptionId) {
    //ToDo this needs to be queued via the task queue
    for(Entry<DestinationImpl, Subscription> entry:activeSubscriptions.entrySet()){
      if(entry.getKey().getFullyQualifiedNamespace().equals(subscriptionId)){
        entry.getValue().hibernate();
      }
    }
  }

}
