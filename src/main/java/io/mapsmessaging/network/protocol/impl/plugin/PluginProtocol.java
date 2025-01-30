/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.plugin;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class PluginProtocol extends Protocol implements ClientConnection {

  private Session session;
  private boolean closed;
  protected String sessionId;
  protected final EndPointURL endPointURL;

  public PluginProtocol(@NonNull @NotNull EndPoint endPoint) {
    super(endPoint);
    endPointURL = new EndPointURL(endPoint.getConfig().getUrl());
    closed = false;
  }

  @Override
  public Subject getSubject() {
    return (session != null) ? session.getSecurityContext().getSubject() : null;
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      SessionManager.getInstance().close(session, true);
      super.close();
    }
  }

  public abstract void forwardMessage(String destinationName, byte[] data, Map<String, Object> map);

  protected void saveMessage(String destinationName, byte[] data, Map<String, Object> map) throws IOException, ExecutionException, InterruptedException {
    Map<String, TypedData> dataMap = new LinkedHashMap<>();
    for(Map.Entry<String, Object> entry:map.entrySet()) {
      dataMap.put(entry.getKey(), new TypedData(entry.getValue()));
    }

    // Create a MapsMessage
    MessageBuilder mb = new MessageBuilder();
    mb.setOpaqueData(data)
        .setDataMap(dataMap)
        .setCreation(System.currentTimeMillis());
    Destination destination = session.findDestination(destinationName, DestinationType.TOPIC).get();
    if (destination != null) {
      destination.storeMessage(mb.build());
    }
  }

  @Override
  public void sendMessage(@org.jetbrains.annotations.NotNull @NonNull MessageEvent messageEvent) {
    Map<String, Object> map = new LinkedHashMap<>();
    for(Map.Entry<String, TypedData> entry: messageEvent.getMessage().getDataMap().entrySet()) {
      map.put(entry.getKey(), entry.getValue().getData());
    }
    forwardMessage(messageEvent.getDestinationName(), messageEvent.getMessage().getOpaqueData(),map);
  }

  protected void doConnect(String sessionId, String username, String password) throws IOException, LoginException {
    SessionContextBuilder scb = new SessionContextBuilder(sessionId, this);
    scb.setUsername(username);
    scb.setPassword(password.toCharArray());
    scb.setPersistentSession(true);
    session = SessionManager.getInstance().create(scb.build(), this);
    setConnected(true);
    this.sessionId = sessionId;
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, String selector, @Nullable Transformer transformer) throws IOException {
    if(transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }
    SubscriptionContextBuilder scb = new SubscriptionContextBuilder(resource, ClientAcknowledgement.AUTO);
    scb.setAlias(resource);
    ClientAcknowledgement ackManger = QualityOfService.AT_MOST_ONCE.getClientAcknowledgement();
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(resource, ackManger);
    builder.setQos(QualityOfService.AT_MOST_ONCE);
    builder.setAllowOverlap(true);
    builder.setReceiveMaximum(1024);
    if(selector != null && !selector.isEmpty()) {
      builder.setSelector(selector);
    }
    session.addSubscription(builder.build());
    session.resumeState();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    ProtocolInformationDTO dto = new ProtocolInformationDTO();
    dto.setSessionId(sessionId);
    dto.setMessageTransformationName("");
    dto.setType("pulsar");
    return null;
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    return false;
  }

  @Override
  public String getName() {
    return "LocalLoop";
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public Principal getPrincipal() {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return endPoint.getAuthenticationConfig();
  }

  @Override
  public String getUniqueName() {
    return endPointURL.toString();
  }

}
