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
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import io.mapsmessaging.utilities.threads.tasks.SingleConcurrentTaskScheduler;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

//
// This class locks the specific hashed pipeline for Session creation and deletion
// Anything that needs to be thread safe needs to be in here and accessed within
// the lock structure
//
public class SessionManagerPipeLine {

  private final Logger logger = LoggerFactory.getLogger(SessionManagerPipeLine.class);
  private final Map<String, SubscriptionController> subscriptionManagerFactory;
  private final Map<String, SessionImpl> sessions;
  private final DestinationManager destinationManager;
  private final PersistentSessionManager storeLookup;

  private final ExecutorService taskScheduler = new SingleConcurrentTaskScheduler("SessionManagerPipeLine");

  private SecurityManager securityManager;

  private final LongAdder connectedSessions;
  private final LongAdder disconnectedSessions;
  private final LongAdder expiredSessions;
  private final WillTaskManager willTaskManager;

  SessionManagerPipeLine(DestinationManager destinationManager, PersistentSessionManager lookup, SecurityManager security, LongAdder connected, LongAdder disconnected,
      LongAdder expired) {
    subscriptionManagerFactory = new LinkedHashMap<>();
    sessions = new LinkedHashMap<>();
    this.destinationManager = destinationManager;
    connectedSessions = connected;
    disconnectedSessions = disconnected;
    expiredSessions = expired;
    securityManager = security;
    storeLookup = lookup;
    willTaskManager = WillTaskManager.getInstance();
  }

  public void stop() {
    for (SessionImpl session : sessions.values()) {
      session.close();
    }
    for(SubscriptionController controller:subscriptionManagerFactory.values()){
      controller.shutdown();
    }
  }

  public boolean hasSessions() {
    return !sessions.isEmpty();
  }

  public List<SessionImpl> getSessions() {
    return new ArrayList<>(sessions.values());
  }

  public boolean hasSubscriptions() {
    return subscriptionManagerFactory.isEmpty();
  }

  public Set<String> getSessionIds() {
    return subscriptionManagerFactory.keySet();
  }

  protected void setSecurityManager(SecurityManager manager) {
    securityManager = manager;
  }

  @SuppressWarnings("java:S1452")
  public Future<?> submit(Callable<?> task) {
    return taskScheduler.submit(task);
  }

  SessionImpl create(SessionContext sessionContext) throws LoginException {
    SessionImpl sessionImpl;
    logger.log(ServerLogMessages.SESSION_MANAGER_CREATE_SECURITY_CONTEXT);
    SecurityContext securityContext = securityManager.getSecurityContext(sessionContext);

    //
    // Force close the older session if duplicates are not allowed
    //
    if (sessions.containsKey(sessionContext.getId())) {
      SessionImpl oldSessionImpl = sessions.get(sessionContext.getId());
      if (oldSessionImpl != null) {
        oldSessionImpl.close();
        logger.log(ServerLogMessages.SESSION_MANAGER_FOUND_CLOSED, sessionContext.getId());
      }
    }

    //
    // Create the session
    //
    SubscriptionController subscriptionManager = loadSubscriptionManager(sessionContext);
    SessionDestinationManager sessionDestinationManager = new SessionDestinationManager(destinationManager);
    if(sessionContext.isPersistentSession()) {
      sessionImpl = new PersistentSession(sessionContext, securityContext, sessionDestinationManager, subscriptionManager, storeLookup);
    }
    else{
      sessionImpl = new SessionImpl(sessionContext, securityContext, sessionDestinationManager, subscriptionManager);
    }

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
    SubscriptionController subscriptionController;
    long expiry = sessionImpl.getExpiry();
    sessions.remove(sessionImpl.getName());
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
      subscriptionController.hibernateAll();
      Future<?> sched = SimpleTaskScheduler.getInstance().schedule(() -> closeAndDeleteSubscriptionController(storeName, subscriptionController), expiry, TimeUnit.SECONDS);
      subscriptionController.setTimeout(sched);
      disconnectedSessions.increment();
    } else {
      closeAndDeleteSubscriptionController(storeName, subscriptionController);
    }
    connectedSessions.decrement();
  }

  void addDisconnectedSession(String sessionId, String uniqueSessionId, Map<String, SubscriptionContext> map) {
    SubscriptionController subscriptionManager = new SubscriptionController(sessionId, uniqueSessionId, destinationManager, map);
    subscriptionManagerFactory.put(sessionId, subscriptionManager);
    disconnectedSessions.increment();
  }

  void closeAndDeleteSubscriptionController(String sessionStateFile, SubscriptionController subscriptionController) {
    closeSubscriptionController(subscriptionController);
    try {
      Files.deleteIfExists(new File(sessionStateFile).toPath());
    } catch (IOException e) {
      // ignore
    }
  }

  void closeSubscriptionController(SubscriptionController subscriptionController) {
    if (subscriptionController.getTimeout() != null) {
      expiredSessions.increment();
    }
    subscriptionManagerFactory.remove(subscriptionController.getSessionId());
    WillTaskImpl willTaskImpl = willTaskManager.remove(subscriptionController.getSessionId());
    if (willTaskImpl != null) {
      willTaskImpl.cancel();
      willTaskImpl.run(); // Will Task MUST run on session close regardless of the will timeout
    }
    subscriptionController.close();
    disconnectedSessions.decrement();
  }

  SubscriptionController getIdleSubscriptions(String sessionId) {
    return subscriptionManagerFactory.get(sessionId);
  }

  //
  // Find any existing Subscription Manager for the incoming session ID
  //
  private SubscriptionController loadSubscriptionManager(SessionContext context) {
    context.setRestored(false);
    SessionDetails sessionDetails = storeLookup.getSessionDetails(context);
    context.setUniqueId(sessionDetails.getUniqueId());
    context.setInternalSessionId(sessionDetails.getInternalUnqueId());
    SubscriptionController subscriptionManager = subscriptionManagerFactory.get(context.getId());
    if (subscriptionManager == null) {
      logger.log(ServerLogMessages.SESSION_MANAGER_NO_EXISTING, context.getId());
      Map<String, SubscriptionContext> contextMap = sessionDetails.getSubscriptionContextMap();
      subscriptionManager = new SubscriptionController(context, destinationManager, contextMap);
      if (context.isPersistentSession()) {
        logger.log(ServerLogMessages.SESSION_MANAGER_ADDING_SUBSCRIPTION, context.getId());
        subscriptionManagerFactory.put(context.getId(), subscriptionManager);
      }
    } else {
      if (clearAndIsTimeOut(subscriptionManager)) {
        return loadSubscriptionManager(context);
      }

      if (context.isResetState()) {
        disconnectedSessions.decrement(); // No longer stored
        logger.log(ServerLogMessages.SESSION_MANAGER_FOUND_EXISTING, context.getId(), context.isResetState());
        subscriptionManagerFactory.remove(context.getId());
        subscriptionManager.close();
        sessionDetails.clearSubscriptions();
        subscriptionManager = new SubscriptionController(context, destinationManager, new LinkedHashMap<>());
        if (context.isPersistentSession()) {
          subscriptionManagerFactory.put(context.getId(), subscriptionManager);
        }
      } else {
        context.setRestored(true);
        disconnectedSessions.decrement(); // Restored
      }
    }
    return subscriptionManager;
  }

  private boolean clearAndIsTimeOut(SubscriptionController subscriptionManager) {
    Future<?> timeout = subscriptionManager.getTimeout();
    if (timeout != null) {
      boolean fired = timeout.isDone();
      timeout.cancel(true);
      if (fired) {
        closeSubscriptionController(subscriptionManager); // Timeout has been executed
        return true;
      }
    }
    subscriptionManager.setTimeout(null);
    return false;
  }
}
