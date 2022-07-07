/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.UserProperty;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
public class SessionContextBuilder {

  @Getter
  private final String id;
  @Getter
  private final ProtocolImpl protocol;
  @Getter
  private final Map<String, String> userData;
  @Getter
  private String authenticationMethod;
  @Getter
  private byte[] authenticationData;
  @Getter
  private String username;
  @Getter
  private char[] password;
  @Getter
  private boolean resetState;
  @Getter
  private String willTopic;
  @Getter
  private Message willMessage;
  @Getter
  private long willDelay;
  @Getter
  private boolean persistentSession;
  @Getter
  private long sessionExpiry;
  @Getter
  private int receiveMaximum;
  @Getter
  private int duration;

  public SessionContextBuilder(@NonNull @NotNull String id, @NonNull @NotNull ProtocolImpl protocol) {
    this.id = id;
    this.protocol = protocol;
    userData = new LinkedHashMap<>();

    username = null;
    password = null;
    resetState = false;
    willMessage = null;
    willTopic = null;
    persistentSession = false;
    willDelay = 0;
    sessionExpiry = -1;
    receiveMaximum = (1 << 16) - 1;
    duration = -1;
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

  public @NonNull @NotNull SessionContextBuilder setAuthenticationMethod(@NonNull @NotNull String authenticationMethod) {
    this.authenticationMethod = authenticationMethod;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setAuthenticationData(@NonNull byte[] authenticationData) {
    this.authenticationData = authenticationData;
    return this;
  }

  public @NonNull @NotNull SessionContextBuilder setKeepAlive(int duration) {
    this.duration = duration;
    return this;
  }

  public SessionContext build() {
    SessionContext sc = new SessionContext(id, protocol);
    sc.setUsername(username);
    sc.setPassword(password);
    sc.setWillTopic(willTopic);
    sc.setWillMessage(willMessage);
    sc.setResetState(resetState);
    sc.setPersistentSession(persistentSession);
    sc.setWillDelay(willDelay);
    sc.setSessionExpiry(sessionExpiry);
    sc.setReceiveMaximum(receiveMaximum);
    sc.setDuration(duration);
    sc.setAuthenticationMethod(authenticationMethod);
    sc.setAuthenticationData(authenticationData);
    return sc;
  }

}
