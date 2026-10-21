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

package io.mapsmessaging.state.mavlink.bootstrap;

import static io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory.MAV_CMD_REQUEST_MESSAGE;
import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.AUTOPILOT_VERSION;
import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.BATTERY_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MavlinkBootstrapRequestPublisherTest {

  @Test
  void autopilot_version_request_is_published_to_the_recorded_mavlink_route() throws Exception {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = routedTwin();
    twinManager.registerTwin(droneTwin, null);

    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    when(protocol.getSession()).thenReturn(session);
    when(session.findDestination("/mavlink/out", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(destination));

    new MavlinkBootstrapRequestPublisher(twinManager, protocol).publish(
        MavlinkBootstrapEvent.requestMessage(droneTwin.getTwinId(), 3, 1, DroneTwinMissingState.MISSING_AUTOPILOT_VERSION, AUTOPILOT_VERSION)
    );

    ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
    verify(destination).storeMessage(messageCaptor.capture());
    Message message = messageCaptor.getValue();
    JsonObject command = JsonParser.parseString(new String(message.getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();

    assertEquals("ID#42#/127.0.0.1:14550", new String(message.getCorrelationData(), StandardCharsets.UTF_8));
    assertEquals(76, command.getAsJsonObject("header").get("messageId").getAsInt());
    assertEquals(3, command.getAsJsonObject("payload").get("target_system").getAsInt());
    assertEquals(1, command.getAsJsonObject("payload").get("target_component").getAsInt());
    assertEquals(MAV_CMD_REQUEST_MESSAGE, command.getAsJsonObject("payload").get("command").getAsInt());
    assertEquals(AUTOPILOT_VERSION, command.getAsJsonObject("payload").get("param1").getAsInt());
  }

  @Test
  void non_autopilot_bootstrap_requests_remain_dormant() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = routedTwin();
    twinManager.registerTwin(droneTwin, null);
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);

    new MavlinkBootstrapRequestPublisher(twinManager, protocol).publish(
        MavlinkBootstrapEvent.requestMessage(droneTwin.getTwinId(), 3, 1, DroneTwinMissingState.MISSING_BATTERY_STATE, BATTERY_STATUS)
    );

    verify(protocol, never()).getSession();
  }

  @Test
  void autopilot_version_request_without_a_response_route_is_not_published() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("drone-3");
    twinManager.registerTwin(droneTwin, null);
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);

    new MavlinkBootstrapRequestPublisher(twinManager, protocol).publish(
        MavlinkBootstrapEvent.requestMessage(droneTwin.getTwinId(), 3, 1, DroneTwinMissingState.MISSING_AUTOPILOT_VERSION, AUTOPILOT_VERSION)
    );

    verify(protocol, never()).getSession();
  }

  private DroneTwin routedTwin() {
    DroneTwin droneTwin = new DroneTwin("drone-3");
    droneTwin.setResponseTopicName("/mavlink/out");
    droneTwin.setUniqueOutboundIdentifier("ID#42#/127.0.0.1:14550");
    return droneTwin;
  }
}
