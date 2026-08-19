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

package io.mapsmessaging.network.protocol.impl.mqtt;

public class MqttKeepAliveManager {

  public enum Action {
    NONE,
    SEND_PING,
    DISCONNECT
  }

  private static final long CLIENT_CHECK_DIVISOR = 5;
  private static final long SERVER_CHECK_DIVISOR = 5;
  private static final long PING_IDLE_NUMERATOR = 7;
  private static final long PING_IDLE_DIVISOR = 10;

  private long outstandingPingTime;

  public long getCheckInterval(long keepAlive, boolean client) {
    long divisor = client ? CLIENT_CHECK_DIVISOR : SERVER_CHECK_DIVISOR;
    return Math.max(1L, keepAlive / divisor);
  }

  public synchronized Action checkClient(long now, long lastWrite, long keepAlive) {
    if (outstandingPingTime != 0) {
      return now - outstandingPingTime >= keepAlive ? Action.DISCONNECT : Action.NONE;
    }

    long pingIdleTime = keepAlive * PING_IDLE_NUMERATOR / PING_IDLE_DIVISOR;
    if (now - lastWrite >= pingIdleTime) {
      outstandingPingTime = now;
      return Action.SEND_PING;
    }
    return Action.NONE;
  }

  public Action checkServer(long now, long lastRead, long keepAlive) {
    long disconnectTime = keepAlive * 2L;
    return now - lastRead >= disconnectTime ? Action.DISCONNECT : Action.NONE;
  }

  public synchronized void pingResponseReceived() {
    outstandingPingTime = 0;
  }

  public static int negotiateServerKeepAlive(int requestedKeepAlive, int minimumKeepAlive, int maximumKeepAlive) {
    if (requestedKeepAlive == 0) {
      return minimumKeepAlive == 0 ? 0 : (maximumKeepAlive == 0 ? minimumKeepAlive : maximumKeepAlive);
    }
    if (requestedKeepAlive < minimumKeepAlive) {
      return minimumKeepAlive;
    }
    if (maximumKeepAlive != 0 && requestedKeepAlive > maximumKeepAlive) {
      return maximumKeepAlive;
    }
    return requestedKeepAlive;
  }
}
