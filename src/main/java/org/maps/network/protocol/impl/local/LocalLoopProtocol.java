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

package org.maps.network.protocol.impl.local;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.messaging.api.SessionManager;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.SubscriptionContextBuilder;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.api.transformers.Transformer;
import org.maps.network.io.EndPoint;
import org.maps.network.io.Packet;
import org.maps.network.protocol.ProtocolImpl;

public class LocalLoopProtocol extends ProtocolImpl {

  private final Map<String, String> nameMapping;
  private final Logger logger;
  private Session session;
  private boolean closed;
  private String sessionId;


  public LocalLoopProtocol(@NonNull @NotNull EndPoint endPoint) {
    super(endPoint);
    logger = LoggerFactory.getLogger(LocalLoopProtocol.class);
    closed = false;
    nameMapping = new ConcurrentHashMap<>();
    logger.log(LogMessages.LOOP_CREATED);
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      SessionManager.getInstance().close(session);
      super.close();
      logger.log(LogMessages.LOOP_CLOSED);
    }
  }

  @Override
  public void sendMessage(@NonNull @NotNull Destination source, @NonNull @NotNull String normalisedName, @NonNull @NotNull SubscribedEventManager subscription, @NonNull @NotNull Message message,
      @NonNull @NotNull Runnable completionTask) {
    String lookup = nameMapping.get(normalisedName);
    if(lookup != null){
      try {
        Destination destination = session.findDestination(lookup);
        if(destination != null) {
          MessageBuilder messageBuilder = new MessageBuilder(message);
          messageBuilder.setDestinationTransformer(destinationTransformationLookup(normalisedName));
          destination.storeMessage(messageBuilder.build());
        }
        completionTask.run();
        logger.log(LogMessages.LOOP_SENT_MESSAGE);
      } catch (IOException ioException) {
        logger.log(LogMessages.LOOP_SEND_MESSAGE_FAILED, ioException);
      }
    }
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException{
    SessionContextBuilder scb = new SessionContextBuilder(sessionId, this);
    scb.setUsername(username);
    scb.setPassword(password.toCharArray());
    scb.setPersistentSession(true);
    try {
      session = SessionManager.getInstance().create(scb.build(), this);
    } catch (LoginException e) {
      logger.log(LogMessages.LOOP_SEND_CONNECT_FAILED, e);
      IOException ioException = new IOException();
      e.initCause(e);
      throw ioException;
    }
    setConnected(true);
    this.sessionId = sessionId;
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource,@Nullable Transformer transformer) throws IOException{
    subscribeLocal(resource, mappedResource, null, transformer);
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, String selector, @Nullable Transformer transformer) throws IOException {
    if(transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }
    nameMapping.put(resource, mappedResource);
    SubscriptionContextBuilder scb = new SubscriptionContextBuilder(resource, ClientAcknowledgement.AUTO);
    scb.setAlias(resource);
    ClientAcknowledgement ackManger = QualityOfService.AT_MOST_ONCE.getClientAcknowledgement();
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(resource, ackManger);
    builder.setQos(QualityOfService.AT_MOST_ONCE);
    builder.setAllowOverlap(true);
    builder.setReceiveMaximum(1024);
    if(selector != null && selector.length() > 0) {
      builder.setSelector(selector);
    }
    session.addSubscription(builder.build());
    session.resumeState();
    logger.log(LogMessages.LOOP_SUBSCRIBED, resource, mappedResource);
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
  public void sendKeepAlive() {

  }
}
