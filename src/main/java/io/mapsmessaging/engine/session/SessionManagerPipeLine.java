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

  SessionManagerPipeLine(DestinationManager destinationManager, PersistentSessionManager lookup, LongAdder connected, LongAdder disconnected, LongAdder expired) {
    this(destinationManager, lookup, connected, disconnected, expired, new SingleConcurrentTaskScheduler("SessionManagerPipeLine"), (task, delay, unit) -> io.mapsmessaging.utilities.threads.SimpleTaskScheduler.getInstance().schedule(task, delay, unit), new SessionStateFileStore(), new SubscriptionControllerFactory(), new SessionFactory(), WillTaskManager.getInstance());
  }

  SessionManagerPipeLine(DestinationManager destinationManager, PersistentSessionManager lookup, LongAdder connected, LongAdder disconnected, LongAdder expired, ExecutorService taskScheduler, SessionExpiryScheduler expiryScheduler, SessionStateFileStore stateFileStore, SubscriptionControllerFactory subscriptionControllerFactory, SessionFactory sessionFactory, WillTaskManager willTaskManager) {
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
    for (String sessionId : Set.copyOf(sessions.keySet())) {
      close(sessionId, true);
    }
    for (String sessionId : getSessionIds()) {
      close(sessionId, true);
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
    replaceActiveSession(sessionContext.getId());

    SecurityContext securityContext = sessionContext.getSecurityContext();
    SubscriptionController controller = loadSubscriptionManager(sessionContext);
    SessionImpl session = sessionFactory.create(sessionContext, securityContext, destinationManager, controller, storeLookup);

    ThreadContext.put("session", sessionContext.getId());
    logger.log(ServerLogMessages.SESSION_MANAGER_LOADED_SUBSCRIPTION, sessionContext.getId(), controller.toString());

    sessions.put(session.getName(), session);
    connectedSessions.increment();
    return session;
  }

  private void replaceActiveSession(String sessionId) {
    SessionImpl previous = sessions.get(sessionId);
    if (previous == null) {
      return;
    }
    close(previous, true);
    logger.log(ServerLogMessages.SESSION_MANAGER_FOUND_CLOSED, sessionId);
  }

  void close(SessionImpl sessionImpl, boolean clearWillTask) {
    if (!sessions.remove(sessionImpl.getName(), sessionImpl)) {
      return;
    }
    SubscriptionController subscriptionController;
    long expiry = sessionImpl.getExpiry();
    String storeName = (sessionImpl instanceof PersistentSession) ? ((PersistentSession) sessionImpl).getStoreName() : "";
    sessionImpl.close();
    handleWill(sessionImpl.getName(), clearWillTask);

    subscriptionController = sessionImpl.getSubscriptionController();
    if (sessionImpl instanceof PersistentSession persistentSession && expiry > 0) {
      disconnectPersistentSession(persistentSession, storeName, subscriptionController, expiry);
    } else {
      closeAndDeleteSubscriptionController(storeName, subscriptionController);
    }
    connectedSessions.decrement();
  }

  private void handleWill(String sessionId, boolean clearWillTask) {
    if (clearWillTask) {
      willTaskManager.remove(sessionId);
      return;
    }
    WillTaskImpl task = willTaskManager.get(sessionId);
    if (task != null) {
      task.schedule();
    }
  }

  private void disconnectPersistentSession(PersistentSession session, String storeName, SubscriptionController controller, long expirySeconds) {
    session.setExpiryTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expirySeconds));
    controller.hibernateAll();
    scheduleExpiry(storeName, controller, expirySeconds, TimeUnit.SECONDS);
  }

  void addDisconnectedSession(String sessionId, String storeName, SessionDetails sessionDetails, Map<String, SubscriptionContext> subscriptions) {
    SubscriptionController controller = subscriptionControllerFactory.create(sessionId, sessionDetails, destinationManager, subscriptions);
    persistentControllers.put(sessionId, controller);

    long remainingMillis = sessionDetails.getExpiryTime() - System.currentTimeMillis();
    if (remainingMillis > 0) {
      scheduleExpiry(storeName, controller, remainingMillis, TimeUnit.MILLISECONDS);
    } else {
      markDisconnected(controller);
      expireAndDeleteSubscriptionController(storeName, controller);
    }
  }

  private void scheduleExpiry(String stateFile, SubscriptionController controller, long delay, TimeUnit unit) {
    Future<?> expiryTask = SessionExpiryTask.schedule(expiryScheduler, taskScheduler, () -> expireAndDeleteSubscriptionController(stateFile, controller), delay, unit);
    controller.setTimeout(expiryTask);
    markDisconnected(controller);
  }

  void closeAndDeleteSubscriptionController(String sessionStateFile, SubscriptionController subscriptionController) {
    if (finaliseController(subscriptionController, false)) {
      deleteStateFile(sessionStateFile);
    }
  }

  private void expireAndDeleteSubscriptionController(String sessionStateFile, SubscriptionController subscriptionController) {
    if (finaliseController(subscriptionController, true)) {
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

  void close(String sessionId, boolean clearWillTask) {
    SessionImpl active = sessions.get(sessionId);
    if (active != null) {
      close(active, clearWillTask);
    }

    SubscriptionController controller = getIdleSubscriptions(sessionId);
    if (controller != null) {
      closeDisconnectedController(controller);
    }
  }

  private void closeDisconnectedController(SubscriptionController controller) {
    Future<?> timeout = controller.getTimeout();
    if (timeout != null) {
      timeout.cancel(false);
      controller.setTimeout(null);
    }
    closeAndDeleteSubscriptionController(resolveStateFile(controller), controller);
  }

  private String resolveStateFile(SubscriptionController controller) {
    SessionDetails details = storeLookup.getSessionDetails(controller.getSessionId());
    if (details == null || details.getUniqueId() == null || details.getUniqueId().isBlank()) {
      return "";
    }
    return storeLookup.getDataPath() + "/" + details.getUniqueId() + ".bin";
  }

  private boolean finaliseController(SubscriptionController controller, boolean expired) {
    return finaliseController(controller, expired, true);
  }

  private boolean finaliseController(SubscriptionController controller, boolean expired, boolean finaliseWill) {
    String sessionId = controller.getSessionId();
    if (sessions.containsKey(sessionId)) {
      return false;
    }
    if (controller.isPersistent() && !persistentControllers.remove(sessionId, controller)) {
      return false;
    }

    clearDisconnected(controller);
    if (expired) {
      expiredSessions.increment();
    }
    if (finaliseWill) {
      finaliseWill(sessionId);
    }
    controller.close(false);
    return true;
  }

  private void finaliseWill(String sessionId) {
    WillTaskImpl willTask = willTaskManager.remove(sessionId);
    if (willTask == null) {
      return;
    }
    willTask.cancel();
    willTask.run(); // Will Task MUST run on final session close regardless of the will timeout
  }

  private void markDisconnected(SubscriptionController controller) {
    if (disconnectedControllers.add(controller)) {
      disconnectedSessions.increment();
    }
  }

  private void clearDisconnected(SubscriptionController controller) {
    if (disconnectedControllers.remove(controller)) {
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
        finaliseController(controller, false);
      }
      return createSubscriptionController(context, sessionDetails, sessionDetails.getSubscriptionContextMap());
    }

    if (context.isResetState()) {
      return resetSubscriptionController(context, sessionDetails, controller);
    }

    context.setRestored(true);
    clearDisconnected(controller);
    return controller;
  }

  private SubscriptionController createSubscriptionController(SessionContext context, SessionDetails sessionDetails, Map<String, SubscriptionContext> subscriptions) {
    logger.log(ServerLogMessages.SESSION_MANAGER_NO_EXISTING, context.getId());
    SubscriptionController controller = subscriptionControllerFactory.create(context, destinationManager, subscriptions);
    if (context.isPersistentSession()) {
      logger.log(ServerLogMessages.SESSION_MANAGER_ADDING_SUBSCRIPTION, context.getId());
      persistentControllers.put(context.getId(), controller);
    }
    return controller;
  }

  private SubscriptionController resetSubscriptionController(SessionContext context, SessionDetails sessionDetails, SubscriptionController controller) {
    logger.log(ServerLogMessages.SESSION_MANAGER_FOUND_EXISTING, context.getId(), true);
    finaliseController(controller, false, false);
    sessionDetails.clearSubscriptions();

    SubscriptionController replacement = subscriptionControllerFactory.create(context, destinationManager, new LinkedHashMap<>());
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
