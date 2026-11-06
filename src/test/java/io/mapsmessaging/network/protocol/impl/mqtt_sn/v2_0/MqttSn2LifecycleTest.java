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
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2LifecycleTest {

  @Test
  void clearConnectionOnlyBecomesUsableAfterSessionEstablishment() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.checkAllowed(MqttSn2PacketType.CONNECT);
    lifecycle.begin(false, false);
    assertEquals(MqttSn2Lifecycle.State.ESTABLISHING, lifecycle.state());
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH));
    lifecycle.connected();
    lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH);
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.CONNECT));
  }

  @Test
  /** CSD01 MQTT-SN 4.11 AUTH completes before 3.1.2 Will/session setup. */
  void saslMustCompleteBeforeWillAndSessionEstablishment() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.begin(true, true);
    assertEquals(MqttSn2Lifecycle.State.AUTHENTICATING, lifecycle.state());
    lifecycle.checkAllowed(MqttSn2PacketType.AUTH);
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.SUBSCRIBE));
    lifecycle.authenticated(true);
    assertEquals(MqttSn2Lifecycle.State.NEGOTIATING_WILL, lifecycle.state());
    assertThrows(IOException.class, () -> lifecycle.connected());
    lifecycle.willAccepted();
    lifecycle.connected();
    lifecycle.checkAllowed(MqttSn2PacketType.SUBSCRIBE);
  }

  @Test
  /** CSD01 MQTT-SN 4.14.2 uses PINGREQ to wake a sleeping session. */
  void sleepingClientWakesWithPingRequestAndOnlyExchangesAllowedPackets() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.begin(false, false);
    lifecycle.connected();
    lifecycle.sleep();
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH));
    lifecycle.checkAllowed(MqttSn2PacketType.PINGREQ);
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.CONNECT));
    lifecycle.wake();
    lifecycle.checkAllowed(MqttSn2PacketType.PUBACK);
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.SUBSCRIBE));
    lifecycle.sleepAgain();
    assertEquals(MqttSn2Lifecycle.State.ASLEEP, lifecycle.state());
  }

  @Test
  void closeIsTerminalAndInvalidTransitionsAreRejected() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    assertThrows(IOException.class, lifecycle::connected);
    lifecycle.close();
    assertThrows(IOException.class, () -> lifecycle.begin(false, false));
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.CONNECT));
  }
  @Test
  void repeated_sleep_accepts_new_cycle_and_ignores_old_expiry() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.begin(false, false);
    lifecycle.connected();
    lifecycle.sleep();
    long firstCycle = lifecycle.sleepGeneration();
    lifecycle.checkAllowed(MqttSn2PacketType.SLEEPREQ);
    lifecycle.sleep();
    assertFalse(lifecycle.expireSleep(firstCycle));
    assertEquals(MqttSn2Lifecycle.State.ASLEEP, lifecycle.state());
    assertTrue(lifecycle.expireSleep(lifecycle.sleepGeneration()));
    assertEquals(MqttSn2Lifecycle.State.CLOSED, lifecycle.state());
  }

  @Test
  void wake_and_return_to_sleep_invalidate_previous_expiry() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.begin(false, false);
    lifecycle.connected();
    lifecycle.sleep();
    long firstCycle = lifecycle.sleepGeneration();
    lifecycle.wake();
    assertFalse(lifecycle.expireSleep(firstCycle));
    lifecycle.sleepAgain();
    assertFalse(lifecycle.expireSleep(firstCycle));
    assertTrue(lifecycle.expireSleep(lifecycle.sleepGeneration()));
  }

  @Test
  void renewal_pauses_other_packets_and_preserves_connected_state() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.begin(false, false);
    lifecycle.connected();
    lifecycle.beginReauthentication();
    lifecycle.checkAllowed(MqttSn2PacketType.AUTH);
    lifecycle.checkAllowed(MqttSn2PacketType.DISCONNECT);
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH));
    assertThrows(IOException.class, lifecycle::beginReauthentication);
    lifecycle.reauthenticated();
    assertEquals(MqttSn2Lifecycle.State.CONNECTED, lifecycle.state());
    lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH);
  }

}
