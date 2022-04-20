package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.will.WillTaskImpl;
import io.mapsmessaging.engine.session.will.WillTaskManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import io.mapsmessaging.utilities.threads.tasks.SingleConcurrentTaskScheduler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import javax.security.auth.login.LoginException;


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
  private final SubscriptionStoreLookup storeLookup;

  private final ExecutorService taskScheduler = new SingleConcurrentTaskScheduler("SessionManagerPipeLine");

  private SecurityManager securityManager;

  private final LongAdder connectedSessions;
  private final LongAdder disconnectedSessions;
  private final LongAdder expiredSessions;
  private final WillTaskManager willTaskManager;

  SessionManagerPipeLine(DestinationManager destinationManager,SubscriptionStoreLookup lookup,SecurityManager security, LongAdder connected, LongAdder disconnected, LongAdder expired) {
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

  public void stop(){
    for(SessionImpl session:sessions.values()){
      session.close();
    }
  }

  public boolean hasSessions(){
    return !sessions.isEmpty();
  }

  public List<SessionImpl> getSessions(){
    return new ArrayList<>(sessions.values());
  }

  public boolean hasSubscriptions(){
    return subscriptionManagerFactory.isEmpty();
  }

  public Set<String> getSessionIds(){
    return subscriptionManagerFactory.keySet();
  }

  protected void setSecurityManager(SecurityManager manager){
    securityManager = manager;
  }

  public Future<?> submit(Callable<?> task){
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
    SubscriptionController subscriptionManager = loadSubscriptionManager(sessionContext, subscriptionManagerFactory);
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
    connectedSessions.increment();
    return sessionImpl;
  }

  void close(SessionImpl sessionImpl, boolean clearWillTask) {
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
    connectedSessions.decrement();
  }

  void addDisconnectedSession(String sessionId, Map<String, SubscriptionContext> map){
    SubscriptionController subscriptionManager = new SubscriptionController(sessionId, destinationManager, map);
    subscriptionManagerFactory.put(sessionId, subscriptionManager);
    disconnectedSessions.increment();
  }

  void closeSubscriptionController(SubscriptionController subscriptionController){
    if(subscriptionController.getTimeout() != null){
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
  private SubscriptionController loadSubscriptionManager(SessionContext context, Map<String, SubscriptionController> subscriptionManagerFactory) {
    context.setRestored(false);
    SubscriptionController subscriptionManager = subscriptionManagerFactory.get(context.getId());
    if (subscriptionManager == null) {
      logger.log(ServerLogMessages.SESSION_MANAGER_NO_EXISTING, context.getId());
      Map<String, SubscriptionContext> contextMap = storeLookup.getSubscriptionContextMap(context.getId(), context.isPersistentSession());
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
        Map<String, SubscriptionContext> contextMap = storeLookup.getSubscriptionContextMap(context.getId(), context.isPersistentSession());
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
}
