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

package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.UserProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@Getter
public class SessionContextBuilder {

  private final String id;
  private final ClientConnection clientConnection;
  private final Map<String, String> userData;
  private String username;
  private char[] password;
  private boolean resetState;
  private String willTopic;
  private Message willMessage;
  private long willDelay;
  private boolean persistentSession;
  private long sessionExpiry;
  private int receiveMaximum;
  private boolean authorized;

  public SessionContextBuilder(@NonNull @NotNull String id, @NonNull @NotNull ClientConnection clientConnection) {
    this.id = id;
    this.clientConnection = clientConnection;
    userData = new LinkedHashMap<>();
    authorized = false;
    username = null;
    password = null;
    resetState = false;
    willMessage = null;
    willTopic = null;
    persistentSession = false;
    willDelay = 0;
    sessionExpiry = -1;
    receiveMaximum = (1 << 16) - 1;
  }

  public @NonNull @NotNull SessionContextBuilder setUsername(@NonNull @NotNull String username) {
    this.username = username;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setPassword(@NonNull char[] password) {
    this.password = password;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setResetState(boolean resetState) {
    this.resetState = resetState;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setWillTopic(@NonNull @NotNull String willTopic) {
    this.willTopic = willTopic;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setWillMessage(@NonNull @NotNull Message willMessage) {
    this.willMessage = willMessage;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setPersistentSession(boolean persistentSession) {
    this.persistentSession = persistentSession;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setSessionExpiry(long sessionExpiry) {
    this.sessionExpiry = sessionExpiry;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setReceiveMaximum(int receiveMaximum) {
    this.receiveMaximum = receiveMaximum;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setUserProperty(@NonNull @NotNull UserProperty property) {
    userData.put(property.getUserPropertyName(), property.getUserPropertyValue());
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setWillDelayInterval(long willDelayInterval) {
    willDelay = willDelayInterval;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder isAuthorized(boolean authorized) {
    this.authorized = authorized;
    return this;
  }

  public SessionContext build() {
    SessionContext sc = new SessionContext(id, clientConnection);
    sc.setUsername(username);
    sc.setPassword(password);
    sc.setWillTopic(willTopic);
    sc.setWillMessage(willMessage);
    sc.setResetState(resetState);
    sc.setPersistentSession(persistentSession);
    sc.setWillDelay(willDelay);
    sc.setExpiry(sessionExpiry);
    sc.setReceiveMaximum(receiveMaximum);
    sc.setAuthorized(authorized);
    return sc;
  }

}
