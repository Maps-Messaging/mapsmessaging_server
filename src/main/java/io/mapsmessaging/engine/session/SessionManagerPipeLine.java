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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.engine.session.will.WillTaskImpl;
import io.mapsmessaging.engine.session.will.WillTaskManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.utilities.threads.tasks.SingleConcurrentTaskScheduler;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

//
// One SessionManagerPipeLine owns the serial execution order for a hash partition
// of session IDs. Session creation, close, reconnect and expiry cleanup for a given
// session ID must execute through this pipeline's taskScheduler.
//
public class SessionManagerPipeLine {

  private final Logger logger = LoggerFactory.getLogger(SessionManagerPipeLine.class);
  private final Map<String, SubscriptionController> persistentControllers;
  private final Set<SubscriptionController> disconnectedControllers;
  private final Map<String, SessionImpl> sessions;
  private final DestinationManager destinationManager;
  private final PersistentSessionManager storeLookup;

  private final ExecutorService taskScheduler;
  private final SessionExpiryScheduler expiryScheduler;
  private final SessionStateFileStore stateFileStore;
  private final SubscriptionControllerFactory subscriptionControllerFactory;
  private final SessionFactory sessionFactory;

  private final LongAdder connectedSessions;
  private final LongAdder disconnectedSessions;
  private final LongAdder expiredSessions;
  private final WillTaskManager willTaskManager;

  SessionManagerPipeLine(DestinationManager destinationManager, PersistentSessionManager lookup, LongAdder connected, LongAdder disconnected,
      LongAdder expired) {
    this(destinationManager, lookup, connected, disconnected, expired,
        new SingleConcurrentTaskScheduler("SessionManagerPipeLine"),
        (task, delay, unit) -> io.mapsmessaging.utilities.threads.SimpleTaskScheduler.getInstance().schedule(task, delay, unit),
        new SessionStateFileStore(), new SubscriptionControllerFactory(), new SessionFactory(), WillTaskManager.getInstance());
  }

  SessionManagerPipeLine(DestinationManager destinationManager, PersistentSessionManager lookup, LongAdder connected, LongAdder disconnected,
      LongAdder expired, ExecutorService taskScheduler, SessionExpiryScheduler expiryScheduler, SessionStateFileStore stateFileStore,
      SubscriptionControllerFactory subscriptionControllerFactory, SessionFactory sessionFactory, WillTaskManager willTaskManager) {
    persistentControllers = new ConcurrentHashMap<>();
    disconnectedControllers = ConcurrentHashMap.newKeySet();
    sessions = new ConcurrentHashMap<>();
    this.destinationManager = destinationManager;
    this.taskScheduler = taskScheduler;
    this.expiryScheduler = expiryScheduler;
    this.stateFileStore = stateFileStore;
    this.subscriptionControllerFactory = subscriptionControllerFactory;
    this.sessionFactory = sessionFactory;
    connectedSessions = connected;
    disconnectedSessions = disconnected;
    expiredSessions = expired;
    storeLookup = lookup;
    this.willTaskManager = willTaskManager;
  }

  public void stop() {
    for (SessionImpl session : sessions.values()) {
      session.close();
    }
    for (SubscriptionController controller : persistentControllers.values()) {
      Future<?> timeout = controller.getTimeout();
      if (timeout != null) {
        timeout.cancel(false);
        controller.setTimeout(null);
      }
      controller.shutdown();
    }
    taskScheduler.shutdown();
  }

  public boolean hasSessions() {
    return !sessions.isEmpty();
  }

  public List<SessionImpl> getSessions() {
    return new ArrayList<>(sessions.values());
  }

  public boolean hasSubscriptions() {
    return !disconnectedControllers.isEmpty();
  }

  public Set<String> getSessionIds() {
    Set<String> sessionIds = new HashSet<>();
    for (SubscriptionController controller : disconnectedControllers) {
      sessionIds.add(controller.getSessionId());
    }
    return Set.copyOf(sessionIds);
  }

  @SuppressWarnings("java:S1452")
  public Future<?> submit(Callable<?> task) {
    return taskScheduler.submit(task);
  }

  SessionImpl create(SessionContext sessionContext) throws LoginException {
    SessionImpl sessionImpl;
    SecurityContext securityContext = sessionContext.getSecurityContext();
    //
    // Force close the older session if duplicates are not allowed
    //
    SessionImpl oldSessionImpl = sessions.remove(sessionContext.getId());
    if (oldSessionImpl != null) {
      oldSessionImpl.close();
      connectedSessions.decrement();
      logger.log(ServerLogMessages.SESSION_MANAGER_FOUND_CLOSED, sessionContext.getId());
    }
    SubscriptionController subscriptionManager = loadSubscriptionManager(sessionContext);
    sessionImpl = sessionFactory.create(sessionContext, securityContext, destinationManager, subscriptionManager, storeLookup);

    //
    // Either reload or create a new subscription manager
    //
    ThreadContext.put("session", sessionContext.getId());
    logger.log(ServerLogMessages.SESSION_MANAGER_LOADED_SUBSCRIPTION, sessionContext.getId(), subscriptionManager.toString());

    //
    // Now record the session
    //
    sessions.put(sessionImpl.getName(), sessionImpl);
    connectedSessions.increment();
    return sessionImpl;
  }

  void close(SessionImpl sessionImpl, boolean clearWillTask) {
    if (!sessions.remove(sessionImpl.getName(), sessionImpl)) {
      return;
    }
    SubscriptionController subscriptionController;
    long expiry = sessionImpl.getExpiry();
    String storeName = (sessionImpl instanceof PersistentSession) ?  ((PersistentSession)sessionImpl).getStoreName(): "";
    sessionImpl.close();

    if (!clearWillTask) {
      WillTaskImpl task = willTaskManager.get(sessionImpl.getName());
      if (task != null) {
        task.schedule();
      }
    } else {
      willTaskManager.remove(sessionImpl.getName());
    }

    //
    // Now lets check the expiry on the session
    //
    subscriptionController = sessionImpl.getSubscriptionController();
    if (expiry > 0) {
      if(sessionImpl instanceof PersistentSession persistentSession) {
        persistentSession.setExpiryTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiry));
      }
      subscriptionController.hibernateAll();
      Future<?> sched = SessionExpiryTask.schedule(expiryScheduler, taskScheduler,
          () -> expireAndDeleteSubscriptionController(storeName, subscriptionController), expiry, TimeUnit.SECONDS);
      subscriptionController.setTimeout(sched);
      markDisconnected(subscriptionController);
    } else {
      closeAndDeleteSubscriptionController(storeName, subscriptionController);
    }
    connectedSessions.decrement();
  }

  void addDisconnectedSession(String sessionId, String storeName, SessionDetails sessionDetails, Map<String, SubscriptionContext> map) {
    SubscriptionController subscriptionManager = subscriptionControllerFactory.create(sessionId, sessionDetails, destinationManager, map);
    persistentControllers.put(sessionId, subscriptionManager);
    markDisconnected(subscriptionManager);
    long timeout =  sessionDetails.getExpiryTime() - System.currentTimeMillis();
    if(timeout > 0) {
      Future<?> sched = SessionExpiryTask.schedule(expiryScheduler, taskScheduler,
          () -> expireAndDeleteSubscriptionController(storeName, subscriptionManager), timeout, TimeUnit.MILLISECONDS);
      subscriptionManager.setTimeout(sched);
    }
    else{
      expireAndDeleteSubscriptionController(storeName, subscriptionManager);
    }
  }

  void closeAndDeleteSubscriptionController(String sessionStateFile, SubscriptionController subscriptionController) {
    if (closeSubscriptionController(subscriptionController, false)) {
      deleteStateFile(sessionStateFile);
    }
  }

  private void expireAndDeleteSubscriptionController(String sessionStateFile, SubscriptionController subscriptionController) {
    if (closeSubscriptionController(subscriptionController, true)) {
      deleteStateFile(sessionStateFile);
    }
  }

  private void deleteStateFile(String sessionStateFile) {
    if (sessionStateFile == null || sessionStateFile.isBlank()) {
      return;
    }
    try {
      stateFileStore.delete(sessionStateFile);
    } catch (IOException e) {
      // ignore
    }
  }

  void closeSubscriptionController(SubscriptionController subscriptionController) {
    closeSubscriptionController(subscriptionController, false);
  }

  private boolean closeSubscriptionController(SubscriptionController subscriptionController, boolean expired) {
    if (subscriptionController.isPersistent()
        && !persistentControllers.remove(subscriptionController.getSessionId(), subscriptionController)) {
      return false;
    }
    if (expired) {
      expiredSessions.increment();
    }
    markConnected(subscriptionController);
    WillTaskImpl willTaskImpl = willTaskManager.remove(subscriptionController.getSessionId());
    if (willTaskImpl != null) {
      willTaskImpl.cancel();
      willTaskImpl.run(); // Will Task MUST run on session close regardless of the will timeout
    }
    subscriptionController.close(false);
    return true;
  }

  private void markDisconnected(SubscriptionController subscriptionController) {
    if (disconnectedControllers.add(subscriptionController)) {
      disconnectedSessions.increment();
    }
  }

  private void markConnected(SubscriptionController subscriptionController) {
    if (disconnectedControllers.remove(subscriptionController)) {
      disconnectedSessions.decrement();
    }
  }

  SubscriptionController getIdleSubscriptions(String sessionId) {
    SubscriptionController controller = persistentControllers.get(sessionId);
    return disconnectedControllers.contains(controller) ? controller : null;
  }

  //
  // Find any existing Subscription Manager for the incoming session ID
  //
  private SubscriptionController loadSubscriptionManager(SessionContext context) {
    context.setRestored(false);
    SessionDetails sessionDetails = storeLookup.getSessionDetails(context);
    context.setUniqueId(sessionDetails.getUniqueId());
    context.setInternalSessionId(sessionDetails.getInternalUnqueId());

    SubscriptionController controller = persistentControllers.get(context.getId());
    if (controller == null) {
      return createSubscriptionController(context, sessionDetails, sessionDetails.getSubscriptionContextMap());
    }

    if (!cancelPendingExpiry(controller)) {
      SubscriptionController current = persistentControllers.get(context.getId());
      if (current == controller) {
        closeSubscriptionController(controller, false);
      }
      return createSubscriptionController(context, sessionDetails, sessionDetails.getSubscriptionContextMap());
    }

    if (context.isResetState()) {
      return resetSubscriptionController(context, sessionDetails, controller);
    }

    context.setRestored(true);
    markConnected(controller);
    return controller;
  }

  private SubscriptionController createSubscriptionController(SessionContext context, SessionDetails sessionDetails,
      Map<String, SubscriptionContext> subscriptions) {
    logger.log(ServerLogMessages.SESSION_MANAGER_NO_EXISTING, context.getId());
    SubscriptionController controller = subscriptionControllerFactory.create(context, destinationManager, subscriptions);
    if (context.isPersistentSession()) {
      logger.log(ServerLogMessages.SESSION_MANAGER_ADDING_SUBSCRIPTION, context.getId());
      persistentControllers.put(context.getId(), controller);
    }
    return controller;
  }

  private SubscriptionController resetSubscriptionController(SessionContext context, SessionDetails sessionDetails,
      SubscriptionController controller) {
    markConnected(controller);
    logger.log(ServerLogMessages.SESSION_MANAGER_FOUND_EXISTING, context.getId(), true);
    persistentControllers.remove(context.getId(), controller);
    controller.close(false);
    sessionDetails.clearSubscriptions();

    SubscriptionController replacement =
        subscriptionControllerFactory.create(context, destinationManager, new LinkedHashMap<>());
    if (context.isPersistentSession()) {
      persistentControllers.put(context.getId(), replacement);
    }
    return replacement;
  }

  private boolean cancelPendingExpiry(SubscriptionController subscriptionManager) {
    Future<?> timeout = subscriptionManager.getTimeout();
    if (timeout == null) {
      return true;
    }
    if (!timeout.cancel(false)) {
      return false;
    }
    subscriptionManager.setTimeout(null);
    return true;
  }
}
