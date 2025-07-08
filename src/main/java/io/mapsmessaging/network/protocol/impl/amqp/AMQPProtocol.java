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

package io.mapsmessaging.network.protocol.impl.amqp;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.AmqpProtocolInformation;
import io.mapsmessaging.dto.rest.session.SessionInformationDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AMQPProtocol extends Protocol {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final ProtonEngine protonEngine;
  private final Map<String, SessionManager> activeSessions;
  private boolean isJms;

  @Getter
  private final String version;

  @Getter
  @Setter
  private volatile boolean closed;

  @Getter
  @Setter
  private String sessionId;

  public AMQPProtocol(EndPoint endPoint, Packet packet) throws IOException {
    super(endPoint,  endPoint.getConfig().getProtocolConfig("amqp"));
    version = "1.0";
    logger = LoggerFactory.getLogger("AMQP Protocol on " + endPoint.getName());
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("version", getVersion());
    activeSessions = new LinkedHashMap<>();
    closed = false;
    isJms = false;
    sessionId = "";
    protonEngine = new ProtonEngine(this);
    protonEngine.processPacket(packet);
    registerRead();
  }

  @Override
  public Subject getSubject() {
    if(!activeSessions.isEmpty()){
      SessionManager sessionManager = activeSessions.values().iterator().next();
      return sessionManager.getSession().getSecurityContext().getSubject();
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    closed = true;
    protonEngine.close();
    selectorTask.close();
    super.close();
    for (SessionManager sessionManager : activeSessions.values()) {
      sessionManager.close();
    }
    deregisterRead();
  }

  public SessionManager getSession(String sessionId) {
    return activeSessions.get(sessionId);
  }

  public SessionManager addSession(String sessionId, Session session) {
    completedConnection();
    SessionManager sessionManager = new SessionManager(session);
    activeSessions.put(sessionId, sessionManager);
    return sessionManager;
  }

  public void delSession(String sessionId) throws IOException {
    SessionManager manager = activeSessions.get(sessionId);
    if (manager != null && manager.decrement() == 0) {
      activeSessions.remove(sessionId);
      manager.close();
    }
  }

  public void registerRead() throws IOException {
    if (selectorTask != null) {
      selectorTask.register(SelectionKey.OP_READ);
    }
  }

  public void deregisterRead() throws IOException {
    selectorTask.cancel(SelectionKey.OP_READ);
  }

  public String getName() {
    return "AMQP";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    protonEngine.sendMessage(messageEvent.getMessage(), messageEvent.getSubscription());
    messageEvent.getCompletionTask().run();
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    protonEngine.processPacket(packet);
    return true;
  }

  public @NonNull @NotNull Logger getLogger() {
    return logger;
  }

  public boolean isJMS() {
    return isJms;
  }

  public void setJMS(boolean isJms) {
    this.isJms = isJms;
  }

  @Override
  public void sendKeepAlive() {
    // This needs to be done
  }

  public String getUsername() {
    String username = "";
    if (protonEngine.getSaslManager() != null) {
      username = protonEngine.getSaslManager().getUsername();
      if (username == null) {
        username = "anonymous";
      }
    }
    return username;
  }

  public boolean isAuthorised() {
    if (protonEngine.getSaslManager() != null) {
      return (protonEngine.getSaslManager().isDone());
    }
    return false;
  }


  @Override
  public ProtocolInformationDTO getInformation() {
    AmqpProtocolInformation information = new AmqpProtocolInformation();
    updateInformation(information);
    List<SessionInformationDTO> sessions = new ArrayList<>();
    for(SessionManager sessionManager : activeSessions.values()) {
      sessions.add(sessionManager.getSession().getSessionInformation());
    }
    information.setSessionInfoList(sessions);
    return information;
  }
}
