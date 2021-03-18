/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.amqp;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;

public class AMQPProtocol extends ProtocolImpl {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final ProtonEngine protonEngine;
  private final String version;

  private final Map<String, SessionManager> activeSessions;

  private volatile boolean closed;
  private String sessionId;
  private boolean isJms;

  public AMQPProtocol(EndPoint endPoint, Packet packet) throws IOException {
    super(endPoint);
    version = "1.0";
    logger = LoggerFactory.getLogger("AMQP Protocol on " + endPoint.getName());
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties());
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("version", getVersion());
    activeSessions = new LinkedHashMap<>();
    closed = false;
    isJms = false;
    protonEngine = new ProtonEngine(this);
    protonEngine.processPacket(packet);
    registerRead();
    sessionId = "";
  }

  @Override
  public void close() throws IOException {
    closed = true;
    protonEngine.close();
    endPoint.close();
    selectorTask.close();
    for(SessionManager sessionManager:activeSessions.values()){
      sessionManager.close();
    }
  }

  public SessionManager getSession(String sessionId){
    return activeSessions.get(sessionId);
  }

  public SessionManager addSession(String sessionId, Session session){
    SessionManager sessionManager = new SessionManager(session);
    activeSessions.put(sessionId, sessionManager);
    return sessionManager;
  }

  public boolean delSession(String sessionId){
    SessionManager manager = activeSessions.get(sessionId);
    if (manager != null && manager.decrement() == 0) {
      activeSessions.remove(sessionId);
      return true;
    }
    return false;
  }

  public void registerRead() throws IOException {
    if(selectorTask != null) {
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
  public void sendMessage(@NonNull @NotNull Destination destination, @NonNull @NotNull String normalisedName, @NonNull @NotNull SubscribedEventManager subscription,@NonNull @NotNull Message message,@NonNull @NotNull Runnable completionTask) {
    protonEngine.sendMessage(message, subscription);
    completionTask.run();
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    protonEngine.processPacket(packet);
    return true;
  }

  public @NonNull @NotNull Logger getLogger(){
    return logger;
  }

  public boolean isClosed() {
    return closed;
  }

  public void setClosed(boolean closed) {
    this.closed = closed;
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

  @Override
  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId){
    this.sessionId = sessionId;
  }

  @Override
  public String getVersion() {
    return version;
  }

}
