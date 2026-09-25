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

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.admin.SessionManagerJMX;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import io.mapsmessaging.engine.session.will.WillTaskManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import lombok.Getter;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class SessionManager implements Agent {

  private final Logger logger = LoggerFactory.getLogger(SessionManager.class);
  private final SessionManagerPipeLine[] sessionPipeLines;
  private final WillTaskManager willTaskManager;
  private final SessionManagerJMX sessionManagerJMX;
  private final PersistentSessionManager storeLookup;

  @Getter
  private SecurityManager securityManager;

  private final LongAdder disconnectedSessions;
  private final LongAdder connectedSessions;
  private final LongAdder expiredSessions;

  private final AtomicLong uniqueSessionId = new AtomicLong(0);


  public SessionManager(SecurityManager security, DestinationManager destinationManager, String dataPath, int pipeLineSize) {
    disconnectedSessions = new LongAdder();
    connectedSessions = new LongAdder();
    expiredSessions = new LongAdder();
    sessionPipeLines = new SessionManagerPipeLine[pipeLineSize];
    storeLookup = new PersistentSessionManager(dataPath);
    Arrays.setAll(sessionPipeLines, x -> new SessionManagerPipeLine(destinationManager, storeLookup, connectedSessions, disconnectedSessions, expiredSessions));
    securityManager = security;
    willTaskManager = WillTaskManager.getInstance();
    sessionManagerJMX = new SessionManagerJMX(this);
  }

  @Override
  public String getName() {
    return "Session Manager";
  }

  @Override
  public String getDescription() {
    return "Session life cycle manager";
  }

  //<editor-fold desc="Manager startup and shutdown functions">
  public void start() {
    if (logger.isInfoEnabled()) {
      logger.log(ServerLogMessages.SESSION_MANAGER_STARTUP);
    }
    //
    // Reload any persistent subscriptions from the file backing
    //
    for (String sessionId : storeLookup.getSessionNames()) {
      SessionDetails sessionDetails = storeLookup.getSessionDetails(sessionId);
      if(sessionDetails.getInternalUnqueId() == 0){
        sessionDetails.setInternalUnqueId(uniqueSessionId.incrementAndGet());
      }
      else if(sessionDetails.getInternalUnqueId() > uniqueSessionId.longValue()) {
        uniqueSessionId.set(sessionDetails.getInternalUnqueId()+1);
      }
      Map<String, SubscriptionContext> map = sessionDetails.getSubscriptionContextMap();
      if (logger.isInfoEnabled()) {
        logger.log(ServerLogMessages.SESSION_MANAGER_LOADING_SESSION, sessionId, map.size());
      }
      //
      // Register the subscription info with the specific pipeline
      //
      if (!map.isEmpty()) {
        String path = storeLookup.getDataPath() + "/" + sessionDetails.getUniqueId() + ".bin";
        sessionPipeLines[getPipeLineIndex(sessionId)].addDisconnectedSession(sessionId, path, sessionDetails, map);
      }
    }
    willTaskManager.start();
    MessageDaemon.getInstance().getDestinationManager().cleanup();
  }

  public void stop() {
    if (logger.isInfoEnabled()) {
      logger.log(ServerLogMessages.SESSION_MANAGER_STOPPING);
    }
    for (SessionManagerPipeLine pipeLine : sessionPipeLines) {
      pipeLine.stop();
    }
    willTaskManager.stop();
    sessionManagerJMX.close();
  }

  @SuppressWarnings("java:S1452")
  public Future<?> submit(String sessionId, Callable<?> task) {
    return sessionPipeLines[getPipeLineIndex(sessionId)].submit(task);
  }

  int getPipeLineIndex(String name) {
    return Math.abs(name.hashCode() % sessionPipeLines.length); // Reduce its size before we get the ABS
  }
  //</editor-fold>

  //<editor-fold desc="Security Manager API">
  protected void setSecurityManager(SecurityManager manager) {
    securityManager = manager;
  }
  //</editor-fold>

  //<editor-fold desc="Session status API">
  public boolean hasSessions() {
    for (SessionManagerPipeLine pipeLine : sessionPipeLines) {
      if (pipeLine.hasSessions()) {
        return true;
      }
    }
    return false;
  }

  public List<SessionImpl> getSessions() {
    List<SessionImpl> response = new ArrayList<>();
    for (SessionManagerPipeLine pipeLine : sessionPipeLines) {
      response.addAll(pipeLine.getSessions());
    }
    return response;
  }
  //</editor-fold>

  //<editor-fold desc="Session life cycle API">

  //
  // The API submits creation to the session ID's pipeline executor. Once on that executor,
  // creation and replacement of a session are serial with close, reconnect and expiry cleanup.
  //
  public SessionImpl create(SessionContext sessionContext) throws LoginException {
    if(sessionContext.getInternalSessionId() == 0){
      sessionContext.setInternalSessionId(uniqueSessionId.incrementAndGet());
    }
    else{
      if(uniqueSessionId.get() < sessionContext.getInternalSessionId()){
        uniqueSessionId.set(sessionContext.getInternalSessionId()+1);
      }
    }
    logger.log(ServerLogMessages.SESSION_MANAGER_CREATE, sessionContext.toString());
    return sessionPipeLines[getPipeLineIndex(sessionContext.getId())].create(sessionContext);
  }

  //
  // The API submits close to the same session ID pipeline used by create(), keeping hibernation,
  // reconnect and expiry cleanup in one serial execution order.
  //
  public void close(SessionImpl sessionImpl, boolean clearWillTask) {
    logger.log(ServerLogMessages.SESSION_MANAGER_CLOSE, sessionImpl.getName());
    sessionPipeLines[getPipeLineIndex(sessionImpl.getName())].close(sessionImpl, clearWillTask);
  }
  //</editor-fold>

  //<editor-fold desc="Administrative session close">
  public void close(String sessionId, boolean clearWillTask) {
    SessionManagerPipeLine pipeLine = sessionPipeLines[getPipeLineIndex(sessionId)];
    pipeLine.submit((Callable<Void>) () -> {
      pipeLine.close(sessionId, clearWillTask);
      return null;
    });
  }
  //</editor-fold>

  //<editor-fold desc="Test Functions ONLY">
  //
  // TEST FUNCTIONS, SHOULD NOT BE PUBLIC
  //
  boolean hasIdleSessions() {
    for (SessionManagerPipeLine sessionManagerPipeLine : sessionPipeLines) {
      if (sessionManagerPipeLine.hasSubscriptions()) {
        return true;
      }
    }
    return false;
  }

  List<String> getIdleSessions() {
    ArrayList<String> retValue = new ArrayList<>();
    for (SessionManagerPipeLine sessionManagerPipeLine : sessionPipeLines) {
      retValue.addAll(sessionManagerPipeLine.getSessionIds());
    }
    return retValue;
  }

  SubscriptionController getIdleSubscriptions(String sessionId) {
    return sessionPipeLines[getPipeLineIndex(sessionId)].getIdleSubscriptions(sessionId);
  }
  //</editor-fold>

  public long getConnected() {
    return connectedSessions.sum();
  }

  public long getDisconnected() {
    return disconnectedSessions.sum();
  }

  public long getTotalExpired() {
    return expiredSessions.sum();
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("");
    status.setStatus(Status.OK);

    if(sessionPipeLines.length == 0) {
      status.setStatus(Status.ERROR);
      status.setComment("No Pipelines defined!!");
    }
    return status;
  }

}
