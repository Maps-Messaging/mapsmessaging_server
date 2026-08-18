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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MqttKeepAliveManagerTest {

  private static final long KEEP_ALIVE = 60_000L;

  @Test
  void checkIntervals_allowClientPingBeforeExpiryAndServerTimeoutAtOneAndAHalfPeriods() {
    MqttKeepAliveManager manager = new MqttKeepAliveManager();

    assertEquals(12_000L, manager.getCheckInterval(KEEP_ALIVE, true));
    assertEquals(12_000L, manager.getCheckInterval(KEEP_ALIVE, false));
  }

  @Test
  void clientCheck_sendsPingBeforeKeepAliveExpires() {
    MqttKeepAliveManager manager = new MqttKeepAliveManager();

    assertEquals(MqttKeepAliveManager.Action.NONE, manager.checkClient(41_999L, 0L, KEEP_ALIVE));
    assertEquals(MqttKeepAliveManager.Action.SEND_PING, manager.checkClient(42_000L, 0L, KEEP_ALIVE));
  }

  @Test
  void clientCheck_waitsForPingResponseThenDisconnectsAfterKeepAlivePeriod() {
    MqttKeepAliveManager manager = new MqttKeepAliveManager();

    assertEquals(MqttKeepAliveManager.Action.SEND_PING, manager.checkClient(42_000L, 0L, KEEP_ALIVE));
    assertEquals(MqttKeepAliveManager.Action.NONE, manager.checkClient(101_999L, 42_000L, KEEP_ALIVE));
    assertEquals(MqttKeepAliveManager.Action.DISCONNECT, manager.checkClient(102_000L, 42_000L, KEEP_ALIVE));
  }

  @Test
  void pingResponse_clearsOutstandingPing() {
    MqttKeepAliveManager manager = new MqttKeepAliveManager();

    assertEquals(MqttKeepAliveManager.Action.SEND_PING, manager.checkClient(42_000L, 0L, KEEP_ALIVE));
    manager.pingResponseReceived();
    assertEquals(MqttKeepAliveManager.Action.NONE, manager.checkClient(42_001L, 42_000L, KEEP_ALIVE));
  }

  @Test
  void serverCheck_disconnectsAtOneAndAHalfKeepAlivePeriods() {
    MqttKeepAliveManager manager = new MqttKeepAliveManager();

    assertEquals(MqttKeepAliveManager.Action.NONE, manager.checkServer(89_999L, 0L, KEEP_ALIVE));
    assertEquals(MqttKeepAliveManager.Action.DISCONNECT, manager.checkServer(90_000L, 0L, KEEP_ALIVE));
  }

  @Test
  void negotiateServerKeepAlive_honoursRequestedValueAndConfiguredBounds() {
    assertEquals(60, MqttKeepAliveManager.negotiateServerKeepAlive(60, 0, 120));
    assertEquals(30, MqttKeepAliveManager.negotiateServerKeepAlive(10, 30, 120));
    assertEquals(60, MqttKeepAliveManager.negotiateServerKeepAlive(120, 0, 60));
    assertEquals(120, MqttKeepAliveManager.negotiateServerKeepAlive(120, 0, 0));
    assertEquals(0, MqttKeepAliveManager.negotiateServerKeepAlive(0, 0, 60));
    assertEquals(60, MqttKeepAliveManager.negotiateServerKeepAlive(0, 30, 60));
    assertEquals(30, MqttKeepAliveManager.negotiateServerKeepAlive(0, 30, 0));
  }
}
