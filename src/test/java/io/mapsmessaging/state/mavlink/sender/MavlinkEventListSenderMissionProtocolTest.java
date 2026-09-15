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

package io.mapsmessaging.state.mavlink.sender;

import static io.mapsmessaging.state.mavlink.sender.MavlinkMissionAcknowledgementHandler.MAV_MISSION_TYPE_MISSION;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.SUCCESS;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.packet.MissionAckPacket;
import io.mapsmessaging.state.mavlink.packet.MissionRequestIntPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class MavlinkEventListSenderMissionProtocolTest {

  @Test
  void missionProtocolIsVehicleRequestDrivenAndCompletes() throws Exception {
    Fixture fixture = fixture(3);

    try (MavlinkEventListSender sender = fixture.newSender()) {
      sender.start();
      sender.onMavlinkMessage(fixture.request(0));
      sender.onMavlinkMessage(fixture.request(1));
      sender.onMavlinkMessage(fixture.request(2));
      sender.onMavlinkMessage(fixture.acceptedAck());

      InOrder inOrder = inOrder(fixture.sender);
      inOrder.verify(fixture.sender).send(fixture.missionCount);
      inOrder.verify(fixture.sender).send(fixture.items.get(0));
      inOrder.verify(fixture.sender).send(fixture.items.get(1));
      inOrder.verify(fixture.sender).send(fixture.items.get(2));
      assertEquals(1, fixture.results.size());
      assertEquals(SUCCESS, fixture.results.getFirst().status());
    }
  }

  @Test
  void missionCountRetriesWhileWaitingForFirstVehicleRequest() throws Exception {
    Fixture fixture = fixture(2);

    try (MavlinkEventListSender sender = fixture.newSender()) {
      sender.start();
      sender.timeout();

      verify(fixture.sender, times(2)).send(fixture.missionCount);
      verify(fixture.sender, never()).send(fixture.items.get(0));
      assertTrue(fixture.results.isEmpty());
    }
  }

  @Test
  void timeoutAfterMissionItemDoesNotRetransmitPreviousItem() throws Exception {
    Fixture fixture = fixture(2);

    try (MavlinkEventListSender sender = fixture.newSender()) {
      sender.start();
      sender.onMavlinkMessage(fixture.request(0));
      sender.timeout();

      verify(fixture.sender).send(fixture.items.get(0));
      assertEquals(1, fixture.results.size());
      assertEquals(TIMEOUT, fixture.results.getFirst().status());
    }
  }

  @Test
  void duplicateVehicleRequestRetransmitsRequestedItem() throws Exception {
    Fixture fixture = fixture(2);

    try (MavlinkEventListSender sender = fixture.newSender()) {
      sender.start();
      sender.onMavlinkMessage(fixture.request(0));
      sender.onMavlinkMessage(fixture.request(0));
      sender.onMavlinkMessage(fixture.request(1));
      sender.onMavlinkMessage(fixture.acceptedAck());

      verify(fixture.sender, times(2)).send(fixture.items.get(0));
      verify(fixture.sender).send(fixture.items.get(1));
      assertEquals(SUCCESS, fixture.results.getFirst().status());
    }
  }

  @Test
  void nonMonotonicVehicleRequestsRemainAuthoritative() throws Exception {
    Fixture fixture = fixture(3);

    try (MavlinkEventListSender sender = fixture.newSender()) {
      sender.start();
      sender.onMavlinkMessage(fixture.request(0));
      sender.onMavlinkMessage(fixture.request(2));
      sender.onMavlinkMessage(fixture.request(1));
      sender.onMavlinkMessage(fixture.acceptedAck());

      verify(fixture.sender).send(fixture.items.get(0));
      verify(fixture.sender).send(fixture.items.get(1));
      verify(fixture.sender).send(fixture.items.get(2));
      assertEquals(SUCCESS, fixture.results.getFirst().status());
    }
  }

  @Test
  void foreignGroundControlRequestDoesNotAdvanceMissionSender() throws Exception {
    Fixture fixture = fixture(2);

    try (MavlinkEventListSender sender = fixture.newSender()) {
      sender.start();
      sender.onMavlinkMessage(fixture.request(0, 250, 194));

      verify(fixture.sender).send(fixture.missionCount);
      verify(fixture.sender, never()).send(fixture.items.get(0));
      assertTrue(fixture.results.isEmpty());
    }
  }

  @Test
  void invalidSequenceDoesNotRestartOrResendItem() throws Exception {
    Fixture fixture = fixture(2);

    try (MavlinkEventListSender sender = fixture.newSender()) {
      sender.start();
      sender.onMavlinkMessage(fixture.request(0));
      sender.onMavlinkMessage(fixture.ack(MissionAckPacket.MAV_MISSION_INVALID_SEQUENCE, "INVALID_SEQUENCE"));
      sender.onMavlinkMessage(fixture.request(1));
      sender.onMavlinkMessage(fixture.acceptedAck());

      verify(fixture.sender).send(fixture.missionCount);
      verify(fixture.sender).send(fixture.items.get(0));
      verify(fixture.sender).send(fixture.items.get(1));
      assertEquals(SUCCESS, fixture.results.getFirst().status());
    }
  }

  @Test
  void transientMissionErrorRequestsFreshOuterTransaction() throws Exception {
    Fixture fixture = fixture(2);

    try (MavlinkEventListSender sender = fixture.newSender()) {
      sender.start();
      sender.onMavlinkMessage(fixture.request(0));
      sender.onMavlinkMessage(fixture.ack(MissionAckPacket.MAV_MISSION_ERROR, "ERROR"));

      assertEquals(1, fixture.results.size());
      assertEquals(TIMEOUT, fixture.results.getFirst().status());
      verify(fixture.sender).send(fixture.items.get(0));
    }
  }

  @Test
  void lostFinalAckAbortsTransactionWithoutResendingFinalItem() throws Exception {
    Fixture fixture = fixture(2);

    try (MavlinkEventListSender sender = fixture.newSender()) {
      sender.start();
      sender.onMavlinkMessage(fixture.request(0));
      sender.onMavlinkMessage(fixture.request(1));
      sender.timeout();

      verify(fixture.sender).send(fixture.items.get(0));
      verify(fixture.sender).send(fixture.items.get(1));
      assertEquals(TIMEOUT, fixture.results.getFirst().status());
    }
  }

  private Fixture fixture(int itemCount) {
    return new Fixture(itemCount);
  }

  private static UxvModelCommandSet commandSet(List<MavlinkMessage> messages) {
    UxvModelCommandSet commandSet = mock(UxvModelCommandSet.class);
    when(commandSet.operation()).thenReturn(UxvOperation.BUILD_MISSION);
    when(commandSet.modelName()).thenReturn("mission");
    when(commandSet.messages()).thenReturn(messages);
    return commandSet;
  }

  private static final class Fixture {

    private static final int LOCAL_SYSTEM_ID = 103;
    private static final int LOCAL_COMPONENT_ID = 190;

    private final MavlinkMessage missionCount = mock(MavlinkMessage.class);
    private final List<MavlinkMessage> items = new ArrayList<>();
    private final List<MavlinkMessage> messages = new ArrayList<>();
    private final List<MavlinkSendResult> results = new ArrayList<>();
    private final MavlinkEventSender sender = mock(MavlinkEventSender.class);
    private final MavlinkMissionAcknowledgementHandler acknowledgementHandler;

    private Fixture(int itemCount) {
      messages.add(missionCount);
      for (int index = 0; index < itemCount; index++) {
        MavlinkMessage item = mock(MavlinkMessage.class);
        items.add(item);
        messages.add(item);
      }
      acknowledgementHandler =
          new MavlinkMissionAcknowledgementHandler(
              messages,
              1,
              itemCount,
              LOCAL_SYSTEM_ID,
              LOCAL_COMPONENT_ID,
              MAV_MISSION_TYPE_MISSION);
    }

    private MavlinkEventListSender newSender() {
      return new MavlinkEventListSender(commandSet(messages), sender, acknowledgementHandler, results::add);
    }

    private MissionRequestIntPacket request(int sequence) {
      return request(sequence, LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID);
    }

    private MissionRequestIntPacket request(int sequence, int targetSystem, int targetComponent) {
      MissionRequestIntPacket packet = mock(MissionRequestIntPacket.class);
      when(packet.isValid()).thenReturn(true);
      when(packet.getSequence()).thenReturn(sequence);
      when(packet.getTargetSystem()).thenReturn(targetSystem);
      when(packet.getTargetComponent()).thenReturn(targetComponent);
      when(packet.isMissionTypePresent()).thenReturn(true);
      when(packet.getMissionType()).thenReturn(MAV_MISSION_TYPE_MISSION);
      return packet;
    }

    private MissionAckPacket acceptedAck() {
      return ack(MissionAckPacket.MAV_MISSION_ACCEPTED, "ACCEPTED");
    }

    private MissionAckPacket ack(int type, String name) {
      MissionAckPacket packet = mock(MissionAckPacket.class);
      when(packet.isValid()).thenReturn(true);
      when(packet.getType()).thenReturn(type);
      when(packet.getTypeName()).thenReturn(name);
      when(packet.isAccepted()).thenReturn(type == MissionAckPacket.MAV_MISSION_ACCEPTED);
      when(packet.getTargetSystem()).thenReturn(LOCAL_SYSTEM_ID);
      when(packet.getTargetComponent()).thenReturn(LOCAL_COMPONENT_ID);
      when(packet.isMissionTypePresent()).thenReturn(true);
      when(packet.getMissionType()).thenReturn(MAV_MISSION_TYPE_MISSION);
      return packet;
    }
  }
}
