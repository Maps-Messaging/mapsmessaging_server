/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.session.SessionContext;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt5.packet.properties.UserProperty;

public class SessionContextBuilder {

  private final String id;
  private final ProtocolImpl protocol;
  private final Map<String, String> userData;
  private String authenticationMethod;
  private byte[] authenticationData;
  private String username;
  private char[] password;
  private boolean resetState;
  private String willTopic;
  private Message willMessage;
  private long willDelay;
  private boolean persistentSession;
  private long sessionExpiry;
  private int receiveMaximum;
  private int duration;

  public SessionContextBuilder(@NotNull String id, @NotNull ProtocolImpl protocol) {
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

  public @NotNull SessionContextBuilder setUsername(@NotNull String username) {
    this.username = username;
    return this;
  }

  public @NotNull SessionContextBuilder setPassword(@NotNull char[] password) {
    this.password = password;
    return this;
  }

  public @NotNull SessionContextBuilder setResetState(boolean resetState) {
    this.resetState = resetState;
    return this;
  }

  public @NotNull SessionContextBuilder setWillTopic(@NotNull String willTopic) {
    this.willTopic = willTopic;
    return this;
  }

  public @NotNull SessionContextBuilder setWillMessage(@NotNull Message willMessage) {
    this.willMessage = willMessage;
    return this;
  }

  public @NotNull SessionContextBuilder setPersistentSession(boolean persistentSession) {
    this.persistentSession = persistentSession;
    return this;
  }

  public @NotNull SessionContextBuilder setSessionExpiry(long sessionExpiry) {
    this.sessionExpiry = sessionExpiry;
    return this;
  }

  public @NotNull SessionContextBuilder setReceiveMaximum(int receiveMaximum) {
    this.receiveMaximum = receiveMaximum;
    return this;
  }

  public @NotNull SessionContextBuilder setUserProperty(@NotNull UserProperty property) {
    userData.put(property.getUserPropertyName(), property.getUserPropertyValue());
    return this;
  }

  public @NotNull SessionContextBuilder setWillDelayInterval(long willDelayInterval) {
    willDelay = willDelayInterval;
    return this;
  }

  public @NotNull SessionContextBuilder setAuthenticationMethod(@NotNull String authenticationMethod) {
    this.authenticationMethod = authenticationMethod;
    return this;
  }

  public @NotNull SessionContextBuilder setAuthenticationData(@NotNull byte[] authenticationData) {
    this.authenticationData = authenticationData;
    return this;
  }

  public String getAuthenticationMethod() {
    return authenticationMethod;
  }

  public byte[] getAuthenticationData() {
    return authenticationData;
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

  public @NotNull SessionContextBuilder setKeepAlive(int duration) {
    this.duration = duration;
    return this;
  }
}
