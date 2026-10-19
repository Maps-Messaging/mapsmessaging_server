/*
 *
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import java.io.IOException;

/**
 * Connection lifecycle gate for MQTT-SN 2.0 CSD01.
 *
 * <p>State transitions are independent of the MQTT-SN 1.2 protocol. Session
 * creation must not occur until all required negotiation has succeeded.</p>
 */
public final class MqttSn2Lifecycle {

  public enum State { NEW, AUTHENTICATING, NEGOTIATING_WILL, ESTABLISHING, CONNECTED, ASLEEP, AWAKE, CLOSED }

  private State state = State.NEW;
  private boolean reauthenticating;

  public synchronized boolean isReauthenticating() {
    return reauthenticating;
  }

  public synchronized void beginReauthentication() throws IOException {
    require(State.CONNECTED);
    if (reauthenticating) {
      throw new IOException("MQTT-SN 2.0 re-authentication is already active");
    }
    reauthenticating = true;
  }

  public synchronized void reauthenticated() throws IOException {
    require(State.CONNECTED);
    if (!reauthenticating) {
      throw new IOException("MQTT-SN 2.0 re-authentication is not active");
    }
    reauthenticating = false;
  }





  public synchronized State state() {
    return state;
  }

  public synchronized void begin(boolean authentication, boolean will) throws IOException {
    require(State.NEW);
    if (authentication) {
      state = State.AUTHENTICATING;
    } else if (will) {
      state = State.NEGOTIATING_WILL;
    } else {
      state = State.ESTABLISHING;
    }
  }

  public synchronized void authenticated(boolean will) throws IOException {
    require(State.AUTHENTICATING);
    state = will ? State.NEGOTIATING_WILL : State.ESTABLISHING;
  }

  public synchronized void willAccepted() throws IOException {
    require(State.NEGOTIATING_WILL);
    state = State.ESTABLISHING;
  }

  public synchronized void connected() throws IOException {
    require(State.ESTABLISHING);
    state = State.CONNECTED;
  }

  public synchronized void sleep() throws IOException {
    require(State.CONNECTED);
    state = State.ASLEEP;
  }

  public synchronized void wake() throws IOException {
    require(State.ASLEEP);
    state = State.AWAKE;
  }

  public synchronized void sleepAgain() throws IOException {
    require(State.AWAKE);
    state = State.ASLEEP;
  }

  public synchronized void checkAllowed(MqttSn2PacketType type) throws IOException {
    if (state == State.CLOSED) {
      throw new IOException("MQTT-SN 2.0 connection is closed");
    }
    if (reauthenticating) {
      if (type != MqttSn2PacketType.AUTH && type != MqttSn2PacketType.DISCONNECT) {
        throw new IOException("MQTT-SN 2.0 traffic paused during re-authentication");
      }
      return;
    }
    boolean allowed = switch (state) {
      case NEW -> type == MqttSn2PacketType.CONNECT;
      case AUTHENTICATING -> type == MqttSn2PacketType.AUTH;
      case NEGOTIATING_WILL, ESTABLISHING -> false;
      case CONNECTED -> type != MqttSn2PacketType.CONNECT && type != MqttSn2PacketType.CONNACK
          && type != MqttSn2PacketType.SLEEPRESP;
      case ASLEEP -> type == MqttSn2PacketType.PINGREQ || type == MqttSn2PacketType.DISCONNECT;
      case AWAKE -> type == MqttSn2PacketType.PUBACK || type == MqttSn2PacketType.PUBREC
          || type == MqttSn2PacketType.PUBCOMP || type == MqttSn2PacketType.REGACK
          || type == MqttSn2PacketType.DISCONNECT;
      case CLOSED -> false;
    };
    if (!allowed) {
      throw new IOException("MQTT-SN 2.0 packet " + type + " not permitted in " + state);
    }
  }

  public synchronized void close() {
    state = State.CLOSED;
    reauthenticating = false;
  }

  private void require(State expected) throws IOException {
    if (state != expected) {
      throw new IOException("MQTT-SN 2.0 expected " + expected + " but was " + state);
    }
  }
}
