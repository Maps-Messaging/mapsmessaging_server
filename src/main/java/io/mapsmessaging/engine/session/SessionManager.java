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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.admin.SessionManagerJMX;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.serializer.MapDBSerializer;
import io.mapsmessaging.engine.session.will.WillDetails;
import io.mapsmessaging.engine.session.will.WillTaskImpl;
import io.mapsmessaging.engine.session.will.WillTaskManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import javax.security.auth.login.LoginException;
import org.apache.logging.log4j.ThreadContext;
import org.mapdb.DB;
import org.mapdb.Serializer;

public class SessionManager {

  private static final String SUBSCRIPTION = "subscription_";
  private static final String WILLTASKS = "WILL_TASKS";

  private final Logger logger = LoggerFactory.getLogger(SessionManager.class);

  private final PipeLineSubscriptionFactory[] lockPipeline;
  private final Map<String, SessionImpl> sessions;
  private final DestinationManager destinationManager;
  private final DB dataStore;
  private final WillTaskManager willTaskManager;
  private final SessionManagerJMX sessionManagerJMX;

  private SecurityManager securityManager;

  private final LongAdder disconnectedSessions;
  private final LongAdder connectedSessions;
  private final LongAdder expiredSessions;


  public SessionManager(SecurityManager security, DestinationManager destinationManager, DB dataStore, int pipelineSize) {
    this.dataStore = dataStore;
    this.destinationManager = destinationManager;
    disconnectedSessions =  new LongAdder();
    connectedSessions = new LongAdder();
    expiredSessions = new LongAdder();
    lockPipeline = new PipeLineSubscriptionFactory[pipelineSize];
    Arrays.setAll(lockPipeline, x -> new PipeLineSubscriptionFactory());
    securityManager = security;
    sessions = new ConcurrentHashMap<>();
    willTaskManager = WillTaskManager.getInstance();
    willTaskManager.setMap(dataStore.hashMap(WILLTASKS, Serializer.STRING, new MapDBSerializer<>(WillDetails.class)).createOrOpen());
    sessionManagerJMX = new SessionManagerJMX(this);
  }

  //<editor-fold desc="Manager startup and shutdown functions">
  public void start() {
    if (logger.isInfoEnabled()) {
      logger.log(ServerLogMessages.SESSION_MANAGER_STARTUP);
    }
    //
    // Reload any persistent subscriptions from the file backing
    //
    for (String name : dataStore.getAllNames()) {
      if (name.startsWith(SUBSCRIPTION)) {
        String sessionId = name.substring(SUBSCRIPTION.length());
        Map<String, SubscriptionContext> map = getSubscriptionContextMap(sessionId, true);
        if (logger.isInfoEnabled()) {
          logger.log(ServerLogMessages.SESSION_MANAGER_LOADING_SESSION, sessionId, map.size());
        }
        //
        // Register the subscription info with the specific pipeline
        //
        if (!map.isEmpty()) {
          disconnectedSessions.increment();
          SubscriptionController subscriptionManager = new SubscriptionController(sessionId, destinationManager, map);
          int index = getPipeLineIndex(sessionId);
          lockPipeline[index].subscriptionManagerFactory.put(sessionId, subscriptionManager);
        }
      }
    }
    willTaskManager.start();
  }

  public void stop() {
    if (logger.isInfoEnabled()) {
      logger.log(ServerLogMessages.SESSION_MANAGER_STOPPING);
    }
    for (SessionImpl sessionImpl : sessions.values()) {
      sessionImpl.close();
    }
    willTaskManager.stop();
    sessionManagerJMX.close();
  }

  int getPipeLineIndex(String name) {
    int hashCode = name.hashCode() % lockPipeline.length; // Reduce its size before we get the ABS
    return Math.abs(hashCode);
  }
  //</editor-fold>

  protected SecurityManager getSecurityManager() {
    return securityManager;
  }

  //<editor-fold desc="Security Manager API">
  protected void setSecurityManager(SecurityManager manager) {
    securityManager = manager;
  }
  //</editor-fold>

  //<editor-fold desc="Session status API">
  public boolean hasSessions() {
    return !sessions.isEmpty();
  }

  public List<SessionImpl> getSessions() {
    return new ArrayList<>(sessions.values());
  }
  //</editor-fold>

  //<editor-fold desc="Session life cycle API">

  //
  // We lock here so that the change to the sessions map and the construction of this session
  // is an atomic operation. If we had 2 or more competing sessions with the same ID we would
  // get corruption as to which will or session was what.
  //
  public SessionImpl create(SessionContext sessionContext) throws LoginException, IOException {
    logger.log(ServerLogMessages.SESSION_MANAGER_CREATE, sessionContext.toString());

    //
    // Check to see if they can actually log in first
    //
    SecurityContext securityContext = securityManager.getSecurityContext(sessionContext);
    logger.log(ServerLogMessages.SESSION_MANAGER_CREATE_SECURITY_CONTEXT);
    SessionImpl sessionImpl;
    int lockIndex = getPipeLineIndex(sessionContext.getId());
    synchronized (lockPipeline[lockIndex]) {
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
      SubscriptionController subscriptionManager = loadSubscriptionManager(sessionContext, lockPipeline[lockIndex].subscriptionManagerFactory);
      SessionDestinationManager sessionDestinationManager = new SessionDestinationManager(destinationManager);
      sessionImpl = new SessionImpl(sessionContext, securityContext, sessionDestinationManager, subscriptionManager);

      //
      // Either reload or create a new subscription manager
      //
      ThreadContext.put("session", sessionContext.getId());
      logger.log(ServerLogMessages.SESSION_MANAGER_LOADED_SUBSCRIPTION, sessionContext.getId(), subscriptionManager.toString());

      //
      // Now record the session
      //
      sessions.put(sessionImpl.getName(), sessionImpl);
    }
    connectedSessions.increment();
    return sessionImpl;
  }

  public void close(SessionImpl sessionImpl) {
    close(sessionImpl, false);
  }

  //
  // This needs to be an atomic operation, if we are 1/2 way through hibernating a session and a client
  // reconnects or wants to restart we would have a race condition between the 2
  //
  public void close(SessionImpl sessionImpl, boolean clearWillTask) {
    if (sessionImpl != null && !sessionImpl.isClosed()) {
      int lockIndex = getPipeLineIndex(sessionImpl.getName());
      connectedSessions.decrement();
      synchronized (lockPipeline[lockIndex]) {
        SubscriptionController subscriptionController;
        long expiry = sessionImpl.getExpiry();
        sessions.remove(sessionImpl.getName());
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
          Future<?> schedule = SimpleTaskScheduler.getInstance().schedule(() -> closeSubscriptionController(subscriptionController), expiry, TimeUnit.SECONDS);
          subscriptionController.setTimeout(schedule);
          disconnectedSessions.increment();
        } else {
          closeSubscriptionController(subscriptionController);
        }
      }
    }
  }
  //</editor-fold>

  //<editor-fold desc="Internal subscription controller functions">
  public void closeSubscriptionController(SubscriptionController subscriptionController) {
    int index = getPipeLineIndex(subscriptionController.getSessionId());
    synchronized (lockPipeline[index]) {
      if(subscriptionController.getTimeout() != null){
        expiredSessions.increment();
      }
      lockPipeline[index].subscriptionManagerFactory.remove(subscriptionController.getSessionId());
      WillTaskImpl willTaskImpl = willTaskManager.remove(subscriptionController.getSessionId());
      if (willTaskImpl != null) {
        willTaskImpl.cancel();
        willTaskImpl.run(); // Will Task MUST run on session close regardless of the will timeout
      }
      subscriptionController.close();
      disconnectedSessions.decrement();
    }
  }

  private SubscriptionController loadSubscriptionManager(SessionContext context, Map<String, SubscriptionController> subscriptionManagerFactory) {
    //
    // Find any existing Subscription Manager for the incoming session ID
    //
    context.setRestored(false);
    SubscriptionController subscriptionManager = subscriptionManagerFactory.get(context.getId());
    if (subscriptionManager == null) {
      logger.log(ServerLogMessages.SESSION_MANAGER_NO_EXISTING, context.getId());
      Map<String, SubscriptionContext> contextMap = getSubscriptionContextMap(context.getId(), context.isPersistentSession());
      subscriptionManager = new SubscriptionController(context, destinationManager, contextMap);
      if (context.isPersistentSession()) {
        logger.log(ServerLogMessages.SESSION_MANAGER_ADDING_SUBSCRIPTION, context.getId());
        subscriptionManagerFactory.put(context.getId(), subscriptionManager);
      }
    } else {
      Future<?> timeout = subscriptionManager.getTimeout();
      if (timeout != null) {
        boolean fired = timeout.isDone();
        timeout.cancel(false);
        if (fired) {
          closeSubscriptionController(subscriptionManager); // Timeout has been executed
          return loadSubscriptionManager(context, subscriptionManagerFactory);
        }
      }
      if (context.isResetState()) {
        disconnectedSessions.decrement(); // No longer stored
        logger.log(ServerLogMessages.SESSION_MANAGER_FOUND_EXISTING, context.getId(), context.isResetState());
        subscriptionManagerFactory.remove(context.getId());
        subscriptionManager.close();
        Map<String, SubscriptionContext> contextMap = getSubscriptionContextMap(context.getId(), context.isPersistentSession());
        contextMap.clear();
        subscriptionManager = new SubscriptionController(context, destinationManager, contextMap);
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

  private Map<String, SubscriptionContext> getSubscriptionContextMap(String sessionId, boolean isPersistent) {
    Map<String, SubscriptionContext> map;
    if (isPersistent) {
      String mapName = SUBSCRIPTION + sessionId;
      map = dataStore.hashMap(mapName, Serializer.STRING, new MapDBSerializer<>(SubscriptionContext.class)).createOrOpen();
    } else {
      map = new LinkedHashMap<>();
    }
    return map;
  }
  //</editor-fold>

  //<editor-fold desc="Test Functions ONLY">
  //
  // TEST FUNCTIONS, SHOULD NOT BE PUBLIC
  //
  boolean hasIdleSessions() {
    for (PipeLineSubscriptionFactory pipeLineSubscriptionFactory : lockPipeline) {
      if (!pipeLineSubscriptionFactory.subscriptionManagerFactory.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  List<String> getIdleSessions() {
    ArrayList<String> retValue = new ArrayList<>();
    for (PipeLineSubscriptionFactory pipeLineSubscriptionFactory : lockPipeline) {
      retValue.addAll(pipeLineSubscriptionFactory.subscriptionManagerFactory.keySet());
    }
    return retValue;
  }

  SubscriptionController getIdleSubscriptions(String sessionId) {
    int index = getPipeLineIndex(sessionId);
    return lockPipeline[index].subscriptionManagerFactory.get(sessionId);
  }
  //</editor-fold>

  public long getConnected(){
    return connectedSessions.sum();
  }

  public long getDisconnected(){
    return disconnectedSessions.sum();
  }

  public long getTotalExpired(){
    return expiredSessions.sum();
  }


  //
  // This class locks the specific hashed pipeline for Session creation and deletion
  // Anything that needs to be thread safe needs to be in here and accessed within
  // the lock structure
  //
  private static final class PipeLineSubscriptionFactory {

    private final Map<String, SubscriptionController> subscriptionManagerFactory;

    PipeLineSubscriptionFactory() {
      subscriptionManagerFactory = new LinkedHashMap<>();
    }
  }
}
