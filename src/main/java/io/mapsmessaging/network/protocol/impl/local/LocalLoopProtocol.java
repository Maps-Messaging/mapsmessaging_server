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

package io.mapsmessaging.network.protocol.impl.local;

import io.mapsmessaging.analytics.Analyser;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.LocalProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LocalLoopProtocol extends Protocol {

  private final Map<String, String> nameMapping;
  private final Logger logger;
  private Session session;
  private boolean closed;
  private String sessionId;


  public LocalLoopProtocol(@NonNull @NotNull EndPoint endPoint) {
    super(endPoint, new ProtocolConfigDTO());
    logger = LoggerFactory.getLogger(LocalLoopProtocol.class);
    closed = false;
    nameMapping = new ConcurrentHashMap<>();
    logger.log(ServerLogMessages.LOOP_CREATED);
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      SessionManager.getInstance().close(session, false);
      super.close();
      logger.log(ServerLogMessages.LOOP_CLOSED);
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    String lookup = nameMapping.get(messageEvent.getDestinationName());
    if (lookup != null) {
      CompletableFuture<Destination> future = session.findDestination(lookup, DestinationType.TOPIC);
      future.thenApply(destination -> {
        try {
          if (destination != null) {
            MessageBuilder messageBuilder = new MessageBuilder(messageEvent.getMessage());
            messageBuilder = messageBuilder.setDestinationTransformer(destinationTransformationLookup(messageEvent.getDestinationName()));
            destination.storeMessage(messageBuilder.build());
          }
          messageEvent.getCompletionTask().run();
          logger.log(ServerLogMessages.LOOP_SENT_MESSAGE);
        } catch (IOException ioException) {
          logger.log(ServerLogMessages.LOOP_SEND_MESSAGE_FAILED, ioException);
        }
        return destination;
      });
    }
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    SessionContextBuilder scb = new SessionContextBuilder(sessionId, new ProtocolClientConnection(this));
    scb.setUsername(username);
    scb.setPassword(password.toCharArray());
    scb.setPersistentSession(false);
    try {
      session = SessionManager.getInstance().create(scb.build(), this);
    } catch (LoginException e) {
      logger.log(ServerLogMessages.LOOP_SEND_CONNECT_FAILED, e);
      IOException ioException = new IOException();
      e.initCause(e);
      throw ioException;
    }
    setConnected(true);
    this.sessionId = sessionId;
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable ParserExecutor executor, @Nullable Transformer transformer, StatisticsConfigDTO statistics) throws IOException {
    subscribeLocal(resource, mappedResource, qos, null, transformer, new NamespaceFilters(null), statistics);
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource,@NonNull @NotNull QualityOfService qos, String selector, @Nullable Transformer transformer, @Nullable NamespaceFilters namespaceFilters, StatisticsConfigDTO statistics) throws IOException {
    if (transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }
    nameMapping.put(resource, mappedResource);
    SubscriptionContextBuilder builder = createSubscriptionContextBuilder(resource, selector, QualityOfService.AT_LEAST_ONCE, 1024);
    session.addSubscription(builder.build());
    session.resumeState();
    logger.log(ServerLogMessages.LOOP_SUBSCRIBED, resource, mappedResource);
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
  public ProtocolInformationDTO getInformation() {
    LocalProtocolInformation information = new LocalProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

}
