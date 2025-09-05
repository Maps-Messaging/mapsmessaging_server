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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.admin.SubscriptionControllerJMX;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.dto.rest.session.SubscriptionContextDTO;
import io.mapsmessaging.dto.rest.session.SubscriptionInformationDTO;
import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.DestinationFilter;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManagerListener;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.modes.SubscriptionModeManager;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * This class simply manages the subscription to destination mapping. It also manages the wildcard subscriptions, the overlap between wildcard subscriptions and simple
 * subscriptions, the difference between queues, topics, shared subscriptions and filtered subscriptions.
 */
public class SubscriptionController implements DestinationManagerListener {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

  private final DestinationFactory destinationManager;
  @Getter
  private final String sessionId;
  private final String uniqueSessionId;
  @Getter
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
  private final Map<DestinationMode, SubscriptionModeManager> subscriptionModeManager;

  //
  // Session represents the remote client
  //
  private SessionImpl sessionImpl;
  private Future<?> schedule;

  public SubscriptionController(SessionContext sessionContext, DestinationFactory destinationManager, Map<String, SubscriptionContext> contextMap) {
    this.sessionId = sessionContext.getId();
    this.destinationManager = destinationManager;
    this.contextMap = contextMap;
    uniqueSessionId = sessionContext.getUniqueId();
    subscriptions = new ConcurrentHashMap<>();
    subscriptionModeManager = constructModeManagers();

    destinationManager.addListener(this);
    isPersistent = sessionContext.isPersistentSession();
    subscriptionControllerJMX = new SubscriptionControllerJMX(this);
  }

  public SubscriptionController(String sessionId, String uniqueSessionId, DestinationFactory destinationManager, Map<String, SubscriptionContext> contextMap) {
    this.sessionId = sessionId;
    this.uniqueSessionId = uniqueSessionId;
    this.destinationManager = destinationManager;
    this.contextMap = contextMap;
    subscriptions = new LinkedHashMap<>();
    subscriptionModeManager = constructModeManagers();

    destinationManager.addListener(this);
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

  private Map<DestinationMode, SubscriptionModeManager> constructModeManagers(){
    Map<DestinationMode, SubscriptionModeManager> result = new LinkedHashMap<>();
    for(DestinationMode mode: DestinationMode.values()){
      result.put(mode, mode.getSubscriptionModeManager() );
    }
    return result;
  }

  public void shutdown(){
    logger.log(ServerLogMessages.SUBSCRIPTION_MGR_CLOSE, sessionId);
    destinationManager.removeListener(this);
    for(SubscriptionModeManager managers: subscriptionModeManager.values()){
      managers.shutdown();
    }
    subscriptions.clear();
    if (subscriptionControllerJMX != null) {
      subscriptionControllerJMX.close();
    }
  }

  public void close() {
    logger.log(ServerLogMessages.SUBSCRIPTION_MGR_CLOSE, sessionId);
    destinationManager.removeListener(this);
    for(SubscriptionModeManager managers: subscriptionModeManager.values()){
      managers.close();
    }
    subscriptions.clear();
    contextMap.clear();
    if (subscriptionControllerJMX != null) {
      subscriptionControllerJMX.close();
    }
  }

  public boolean isHibernating() {
    return sessionImpl == null;
  }

  /**
   * Simply add an incoming subscription, this will create a new subscription or an update
   *
   * @param context new subscription context to add
   * @return SubscribedEventManager  with the new event manager
   * @throws IOException if the add subscription failed, typically this is fatal since it means the server can not allocate resources
   */
  public SubscribedEventManager addSubscription(SubscriptionContext context) throws IOException {
    return addSubscription(context, false);
  }

  /**
   * Now scan the destinations that this subscription had and remove any context, if its the last context then we need to delete and close the subscription and all data
   *
   * @param id the specific subscription id used to create the subscription, could be the destination name or an alias
   * @return true if there was such a subscription to start with else false if unable to locate the subscription
   */
  public boolean delSubscription(String id) {
    boolean found = false;
    for(SubscriptionModeManager managers: subscriptionModeManager.values()){
      if(managers.delSubscription(id, subscriptions, this)){
        found = true;
        contextMap.remove(managers.getMode().getNamespace()+id);
      }
    }
    return found;
  }

  /**
   * Called when a new destination is created, we see if any wildcards match the new destination
   */
  @Override
  public void created(DestinationImpl destinationImpl) {
    for(SubscriptionModeManager managers: subscriptionModeManager.values()){
      managers.created(this, destinationImpl, subscriptions.values());
    }
  }

  /**
   * It is assumed that the deletion of the destination and its resource that ALL persistent data is deleted We simply clean up our structure here
   */
  @Override
  public void deleted(DestinationImpl destinationImpl) {
    for(SubscriptionModeManager managers: subscriptionModeManager.values()){
      managers.deleted(destinationImpl, subscriptions);
    }
  }

  public void hibernateAll() {
    if (sessionImpl != null) {
      sessionImpl = null;
      for(SubscriptionModeManager managers: subscriptionModeManager.values()){
        managers.hibernate();
      }
    }
  }

  public void wake(SessionImpl sessionImpl) {
    if (schedule != null) {
      schedule.cancel(false);
    }
    if (this.sessionImpl == null) {
      this.sessionImpl = sessionImpl;
    }
  }

  public SubscribedEventManager wake(SessionImpl sessionImpl, DestinationImpl destination) {
    for(SubscriptionModeManager managers: subscriptionModeManager.values()){
      SubscribedEventManager manager = managers.wake(sessionImpl, destination);
      if (manager != null) {
        return manager;
      }
    }
    return null;
  }

  public void wakeAll(SessionImpl sessionImpl) {
    wake(sessionImpl);
    if (this.sessionImpl != null) {
      for (SubscriptionModeManager managers : subscriptionModeManager.values()) {
        managers.wakeAll(sessionImpl);
      }
    }
  }

  private void queueRetainedMessage(DestinationImpl destinationImpl, Subscription subscription) {
    if (subscription.getContext() != null && !subscription.getContext().getRetainHandler().equals(RetainHandler.DO_NOT_SEND)) {
      long retainedId = destinationImpl.getRetainedIdentifier();
      if (retainedId >= 0) {
        subscription.register(retainedId);
      }
    }
  }

  public Subscription createSchemaSubscription(@NonNull @NotNull SubscriptionContext context, @NonNull @NotNull DestinationImpl destinationImpl) throws IOException {
    SubscriptionBuilder builder = SubscriptionFactory.getInstance().getBuilder(destinationImpl, context, false); // Schema subscriptions have no persistence
    Subscription subscription = builder.construct(sessionImpl, sessionId, uniqueSessionId, context.getAllocatedId());
    updateSubscriptionManager(context, destinationImpl, subscription);
    return subscription;
  }

  public Subscription createSubscription(@NonNull @NotNull SubscriptionContext context, @NonNull @NotNull DestinationImpl destinationImpl) throws IOException {
    SubscriptionBuilder builder = SubscriptionFactory.getInstance().getBuilder(destinationImpl, context, isPersistent);
    Subscription subscription = builder.construct(sessionImpl, sessionId, uniqueSessionId, context.getAllocatedId());
    if (!context.isReplaced() || context.getRetainHandler().equals(RetainHandler.SEND_ALWAYS)) {
      queueRetainedMessage(destinationImpl, subscription);
    }
    updateSubscriptionManager(context, destinationImpl, subscription);
    return subscription;
  }

  public Subscription createBrowserSubscription(@NonNull @NotNull SubscriptionContext context, @NonNull @NotNull DestinationSubscription parent, @NonNull @NotNull DestinationImpl destinationImpl) throws IOException {
    SubscriptionBuilder builder = SubscriptionFactory.getInstance().getBrowserBuilder(destinationImpl, context, parent);
    Subscription subscription = builder.construct(sessionImpl, sessionId, uniqueSessionId, context.getAllocatedId());
    updateSubscriptionManager(context, destinationImpl, subscription);
    return subscription;
  }

  protected void updateSubscriptionManager(@NonNull @NotNull SubscriptionContext context, DestinationImpl destinationImpl,  Subscription subscription){
    subscriptionModeManager.get(context.getDestinationMode()).add(subscription, destinationImpl);
  }

  @SuppressWarnings("java:S1452")
  public Future<?> getTimeout() {
    return schedule;
  }

  public void setTimeout(Future<?> newSchedule) {
    if (schedule != null && !schedule.isCancelled()) {
      schedule.cancel(true);
    }
    schedule = newSchedule;
  }

  public Map<String, SubscriptionContext> getSubscriptions() {
    return Collections.unmodifiableMap(contextMap);
  }

  public Subscription get(DestinationImpl destination) {
    return subscriptionModeManager.get(DestinationMode.NORMAL).get(destination);
  }

  public Subscription remove(DestinationImpl destination) {
    return  subscriptionModeManager.get(DestinationMode.NORMAL).remove(destination);
  }

  public Subscription getSchema(DestinationImpl destination) {
    return  subscriptionModeManager.get(DestinationMode.SCHEMA).get(destination);
  }

  public Subscription removeSchema(DestinationImpl destination) {
    return subscriptionModeManager.get(DestinationMode.SCHEMA).remove(destination);
  }

  public void hibernateSubscription(String subscriptionId) {
    for (SubscriptionModeManager managers : subscriptionModeManager.values()) {
      managers.hibernateSubscription(subscriptionId);
    }
  }

  @SneakyThrows
  private SubscribedEventManager addSubscription(SubscriptionContext context, boolean isReload) throws IOException {
    SubscribedEventManager subscription = null;
    String filter = context.getFilter();
    if (filter.equals("test/nosubscribe")) {
      throw new IOException("Not authorised to access topic");
    }
    if (!subscriptions.containsKey(context.getKey())) {
      if (!context.containsWildcard()) {
        CompletableFuture<DestinationImpl> future = destinationManager.findOrCreate(filter);
        if (future == null && filter.toLowerCase().startsWith("$sys")) {
          future = destinationManager.find("$SYS/notImplemented");
        }
        if (future != null) {
          future.get();
        }
      }
      DestinationFilter destinationFilter = name -> DestinationSet.matches(context, name);
      DestinationSet destinationSet = new DestinationSet(context, destinationManager.get(destinationFilter));
      subscriptions.put(context.getKey(), destinationSet);
      //
      // Now compare the active subscription destinations with the ones in this new subscriptionSet
      //
      if (!destinationSet.isEmpty()) {
        SubscriptionModeManager modeManager = subscriptionModeManager.get(context.getDestinationMode());
        try {
          subscription = modeManager.processSubscriptions(this, context, destinationSet, isReload);
        } catch (IOException e) {
          e.printStackTrace();
          //ToDo, log this and report error
        }
      }
    } else {
      // We have received a subscription for an already registered subscription!!!
      delSubscription(context.getKey());
      context.setReplaced(true);
      return addSubscription(context);
    }
    if (!isReload) {
      contextMap.put(context.getKey(), context);
    }
    return subscription;
  }

  public SubscriptionInformationDTO getSubscriptionInformation() {
    SubscriptionInformationDTO dto = new SubscriptionInformationDTO();
    dto.setHibernated(isHibernating());
    dto.setPersistent(isPersistent);
    dto.setSessionId(sessionId);
    dto.setUniqueId(uniqueSessionId);
    List<SubscriptionContextDTO> subscriptionList = new ArrayList<>();
    for(SubscriptionContext context : contextMap.values()) {
      subscriptionList.add(context.getDetails());
    }
    dto.setSubscriptionContextList(subscriptionList);

    List<SubscriptionStateDTO> stateList = new ArrayList<>();
    for(SubscriptionModeManager managers: subscriptionModeManager.values()){
      stateList.addAll(managers.getDetails());
    }
    dto.setSubscriptionStateList(stateList);
    return dto;
  }

}
