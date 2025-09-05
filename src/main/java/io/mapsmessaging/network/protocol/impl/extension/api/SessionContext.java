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

package io.mapsmessaging.network.protocol.impl.extension.api;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.protocol.impl.extension.ExtensionProtocol;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SessionContext {

  private final Session session;
  private final Map<String, DestinationContext> destinations;

  protected SessionContext(@NonNull @NotNull ExtensionProtocol extensionProtocol, @NonNull @NotNull String sessionId, @Nullable String username, @Nullable String password) throws LoginException, IOException {
    SessionContextBuilder scb = new SessionContextBuilder(sessionId, extensionProtocol);
    scb.setUsername(username != null ? username : "");
    scb.setPassword(password != null ? password.toCharArray() : new char[0]);
    scb.setPersistentSession(false);
    session = SessionManager.getInstance().create(scb.build(), extensionProtocol);
    destinations = new ConcurrentHashMap<>();
  }

  protected void close() throws IOException {
    SessionManager.getInstance().close(session, true);
  }

  public String getSessionId() {
    return session.getName();
  }

  public Subject getSubject(){
    return session.getSecurityContext().getSubject();
  }

  public DestinationContext getDestination(@NonNull @NotNull String destinationName, @NonNull @NotNull DestinationType destinationType) throws  ExecutionException, InterruptedException, TimeoutException {
    if(destinations.containsKey(destinationName)) {
      return destinations.get(destinationName);
    }
    DestinationContext context = new DestinationContext(session.findDestination(destinationName, destinationType).get(60_000L, TimeUnit.MILLISECONDS));
    destinations.put(destinationName, context);
    return context;
  }

  public void subscribe(@NonNull @NotNull String resource,@Nullable String selector) throws IOException {
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

}
