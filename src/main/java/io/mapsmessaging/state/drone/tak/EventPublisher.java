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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.ClientConnection;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.*;

public class EventPublisher implements ClientConnection, MessageListener {

  private final Session session;
  private final Destination destination;

  public EventPublisher(String topic) throws ExecutionException, InterruptedException, TimeoutException {
    session = createSession();
    destination = session.findDestination(topic, DestinationType.TOPIC).get(1, TimeUnit.SECONDS);
  }

  public void close() throws IOException {
    SessionManager.getInstance().close(session, true);
  }

  public void publish(String xml) throws IOException {
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(xml.getBytes())
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setContentType("text/xml")
        .setSchemaId(SchemaManager.DEFAULT_XML_SCHEMA.toString())
        .setRetain(false);
    destination.storeMessage(messageBuilder.build());
  }

  private Session createSession() throws ExecutionException, InterruptedException, TimeoutException {
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("tak_publisher", this);
    sessionContextBuilder.setResetState(true)
        .setSessionExpiry(0)
        .isInternal(true)
        .setPersistentSession(false)
        .setReceiveMaximum(100);

    CompletableFuture<Session> sessionFuture = SessionManager.getInstance().createAsync(sessionContextBuilder.build(), this);
    return sessionFuture.get(5, TimeUnit.SECONDS);
  }

  @Override
  public long getTimeOut() {
    return 0;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public String getVersion() {
    return "";
  }

  @Override
  public void sendKeepAlive() {

  }

  @Override
  public Principal getPrincipal() {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

  @Override
  public String getUniqueName() {
    return "";
  }

  @Override
  public String getProtocolName() {
    return "";
  }

  @Override
  public String getRemoteIp() {
    return "";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {

  }
}
