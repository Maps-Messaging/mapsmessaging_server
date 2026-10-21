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

package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt.RemoteSubscriptionTracker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import io.mapsmessaging.network.route.link.LinkState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.concurrent.TimeoutException;
import static org.mockito.Mockito.*;

class RemoteSubscriptionEstablishmentTest {
  private final EndPointConnection connection = mock(EndPointConnection.class);
  private final MQTTProtocol protocol = mock(MQTTProtocol.class);
  private final RemoteSubscriptionTracker tracker = new RemoteSubscriptionTracker(new PacketIdManager());
  private final Establishing establishing = new Establishing(connection);
  private int packetId;
  private boolean cancelDuringSubscribe;

  private void start() throws Exception {
    EndPointConnectionServerConfigDTO config = new EndPointConnectionServerConfigDTO();
    LinkConfigDTO link = new LinkConfigDTO();
    link.setDirection("pull");
    link.setRemoteNamespace("remote/topic");
    link.setLocalNamespace("local/topic");
    config.setLinkConfigs(List.of(link));
    when(connection.getProperties()).thenReturn(config);
    when(connection.getProtocol()).thenReturn(protocol);
    when(connection.getLogger()).thenReturn(mock(Logger.class));
    when(connection.getState()).thenReturn(establishing);
    when(protocol.getRemoteSubscriptionTracker()).thenReturn(tracker);
    doAnswer(invocation -> {
      packetId = tracker.register(List.of(invocation.<String>getArgument(0)));
      if (cancelDuringSubscribe) {
        establishing.cancel();
      }
      return null;
    }).when(protocol).subscribeRemote(anyString(), anyString(), any(), any(), any(), any(), any());
    establishing.execute();
  }

  @Test
  void reconnect_creates_fresh_establishing_attempt() {
    when(connection.getEstablishingState()).thenReturn(establishing);
    establishing.cancel();
    Connected connected = new Connected(connection);
    connected.execute();
    connected.execute();
    ArgumentCaptor<State> states = ArgumentCaptor.forClass(State.class);
    verify(connection, times(2)).scheduleState(states.capture());
    org.junit.jupiter.api.Assertions.assertNotSame(establishing, states.getAllValues().get(0));
    org.junit.jupiter.api.Assertions.assertNotSame(states.getAllValues().get(0), states.getAllValues().get(1));
  }

  @Test
  void pending_suback_does_not_advertise_connected_link() throws Exception {
    start();
    assertEquals(LinkState.CONNECTING, establishing.getLinkState());
    establishing.cancel();
  }

  @Test
  void missing_suback_does_not_establish_connection() throws Exception {
    start();
    verify(connection, never()).scheduleState(isA(Established.class));
    establishing.cancel();
  }

  @Test
  void successful_suback_establishes_connection() throws Exception {
    start();
    tracker.acknowledge(packetId, new int[] {1});
    verify(connection).scheduleState(isA(Established.class));
    verify(protocol, never()).close();
  }

  @Test
  void rejected_suback_closes_and_reconnects() throws Exception {
    start();
    tracker.acknowledge(packetId, new int[] {0x80});
    verify(protocol).close();
    verify(connection).scheduleState(isA(Disconnected.class));
    verify(connection, never()).scheduleState(isA(Established.class));
  }

  @Test
  void repeated_failure_and_teardown_do_not_schedule_duplicate_reconnects() throws Exception {
    start();
    doAnswer(invocation -> { tracker.close(); return null; }).when(protocol).close();
    tracker.acknowledge(packetId, new int[] {0x80});
    tracker.acknowledge(packetId, new int[] {0x80});
    tracker.close();
    verify(protocol).close();
    verify(connection).scheduleState(isA(Disconnected.class));
    verify(connection, never()).scheduleState(isA(Established.class));
  }

  @Test
  void timeout_closes_and_reconnects_without_establishing() throws Exception {
    start();
    tracker.awaitAcknowledgements().completeExceptionally(new TimeoutException("Missing SUBACK"));
    verify(protocol).close();
    verify(connection).scheduleState(isA(Disconnected.class));
    verify(connection, never()).scheduleState(isA(Established.class));
  }

  @Test
  void old_acknowledgement_cannot_establish_reconnected_protocol() throws Exception {
    start();
    when(connection.getProtocol()).thenReturn(mock(MQTTProtocol.class));
    tracker.acknowledge(packetId, new int[] {0});
    verify(connection, never()).scheduleState(isA(Established.class));
  }

  @Test
  void cancel_during_subscribe_blocks_late_success() throws Exception {
    cancelDuringSubscribe = true;
    start();
    tracker.acknowledge(packetId, new int[] {0});
    verify(connection, never()).scheduleState(any());
  }

  @Test
  void paused_connection_cannot_establish_from_late_acknowledgement() throws Exception {
    start();
    when(connection.isPaused()).thenReturn(true);
    tracker.acknowledge(packetId, new int[] {0});
    verify(connection, never()).scheduleState(any());
  }

  @Test
  void cancelling_attempt_blocks_late_success() throws Exception {
    start();
    establishing.cancel();
    tracker.acknowledge(packetId, new int[] {0});
    verify(connection, never()).scheduleState(any());
  }
}
