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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.MissionAckPacket;
import io.mapsmessaging.state.mavlink.packet.MissionRequestIntPacket;
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.Acknowledgement;
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.Action;
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.TimeoutAction;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MavlinkMissionAcknowledgementHandlerTest {

  private static final int LOCAL_SYSTEM_ID = 103;
  private static final int LOCAL_COMPONENT_ID = 190;

  @Test
  void constructorRejectsInvalidMissionShape() {
    assertThrows(NullPointerException.class, () -> new MavlinkMissionAcknowledgementHandler(null, 3));
    assertThrows(IllegalArgumentException.class, () -> new MavlinkMissionAcknowledgementHandler(messages(4), -1, 3, LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID, 0));
    assertThrows(IllegalArgumentException.class, () -> new MavlinkMissionAcknowledgementHandler(messages(4), 1, -1, LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID, 0));
    assertThrows(IllegalArgumentException.class, () -> new MavlinkMissionAcknowledgementHandler(messages(3), 1, 3, LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID, 0));
  }

  @Test
  void requiresAcknowledgementOnlyForMissionMessages() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    assertTrue(handler.requiresAcknowledgement(messages.get(0)));
    assertTrue(handler.requiresAcknowledgement(messages.get(3)));
    assertFalse(handler.requiresAcknowledgement(mock(MavlinkMessage.class)));
  }

  @Test
  void requestZeroEstablishesTransferAndSendsFirstItem() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), request(0));

    assertEquals(Action.SEND_INDEX, acknowledgement.action());
    assertEquals(1, acknowledgement.index());
  }

  @Test
  void delayedNonzeroRequestBeforeRequestZeroIsIgnored() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    assertEquals(Action.NOT_RELATED, handler.acknowledge(messages.get(0), request(2)).action());
    assertEquals(Action.SEND_INDEX, handler.acknowledge(messages.get(0), request(0)).action());
  }

  @Test
  void duplicateRequestRetransmitsOnlyWhenVehicleRequestsIt() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    handler.acknowledge(messages.get(0), request(0));
    Acknowledgement duplicate = handler.acknowledge(messages.get(1), request(0));

    assertEquals(Action.SEND_INDEX, duplicate.action());
    assertEquals(1, duplicate.index());
  }

  @Test
  void nonMonotonicValidRequestsAreAuthoritativeAfterTransferStarts() {
    List<MavlinkMessage> messages = messages(5);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 4);

    handler.acknowledge(messages.get(0), request(0));
    Acknowledgement requestThree = handler.acknowledge(messages.get(1), request(3));
    Acknowledgement requestOne = handler.acknowledge(messages.get(4), request(1));
    Acknowledgement requestTwo = handler.acknowledge(messages.get(2), request(2));

    assertEquals(4, requestThree.index());
    assertEquals(2, requestOne.index());
    assertEquals(3, requestTwo.index());
  }

  @Test
  void requestOutsideRangeFails() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), request(3));

    assertEquals(Action.FAIL, acknowledgement.action());
    assertEquals("Mission requested sequence 3 outside range 0..2", acknowledgement.reason());
  }

  @Test
  void foreignGroundControlRequestAndAckAreIgnored() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    MissionRequestIntPacket foreignRequest = request(0, 250, 194);
    MissionAckPacket foreignAck = missionAck(MissionAckPacket.MAV_MISSION_ACCEPTED, "ACCEPTED", 250, 194);

    assertEquals(Action.NOT_RELATED, handler.acknowledge(messages.get(0), foreignRequest).action());
    assertEquals(Action.NOT_RELATED, handler.acknowledge(messages.get(0), foreignAck).action());
  }

  @Test
  void broadcastTargetIsAcceptedForCompatibility() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), request(0, 0, 0));

    assertEquals(Action.SEND_INDEX, acknowledgement.action());
  }

  @Test
  void wrongMissionTypeIsIgnored() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);
    MissionRequestIntPacket packet = request(0);
    when(packet.isMissionTypePresent()).thenReturn(true);
    when(packet.getMissionType()).thenReturn(1);

    assertEquals(Action.NOT_RELATED, handler.acknowledge(messages.get(0), packet).action());
  }

  @Test
  void acceptedAckRequiresEveryMissionItemToHaveBeenRequested() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    handler.acknowledge(messages.get(0), request(0));
    handler.acknowledge(messages.get(1), request(2));
    handler.acknowledge(messages.get(3), request(1));

    assertEquals(Action.COMPLETE, handler.acknowledge(messages.get(2), acceptedAck()).action());
  }

  @Test
  void acceptedAckBeforeAllItemsWereRequestedFails() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    handler.acknowledge(messages.get(0), request(0));
    Acknowledgement acknowledgement = handler.acknowledge(messages.get(1), acceptedAck());

    assertEquals(Action.FAIL, acknowledgement.action());
    assertEquals("Mission upload completed before all requested items were sent", acknowledgement.reason());
  }

  @Test
  void invalidSequenceDoesNotRestartArduPilotTransfer() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);
    handler.acknowledge(messages.get(0), request(0));

    Acknowledgement acknowledgement =
        handler.acknowledge(messages.get(1), missionAck(MissionAckPacket.MAV_MISSION_INVALID_SEQUENCE, "INVALID_SEQUENCE", LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID));

    assertEquals(Action.WAIT, acknowledgement.action());
    assertEquals(Action.SEND_INDEX, handler.acknowledge(messages.get(1), request(1)).action());
  }

  @Test
  void transientMissionErrorsRequestCleanOuterRetry() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    assertEquals(
        Action.RETRY_TRANSACTION,
        handler.acknowledge(messages.get(0), missionAck(MissionAckPacket.MAV_MISSION_ERROR, "ERROR", LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID)).action());
    assertEquals(
        Action.RETRY_TRANSACTION,
        handler.acknowledge(messages.get(0), missionAck(MissionAckPacket.MAV_MISSION_OPERATION_CANCELLED, "OPERATION_CANCELLED", LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID)).action());
  }

  @Test
  void permanentMissionErrorFailsImmediately() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    Acknowledgement acknowledgement =
        handler.acknowledge(messages.get(0), missionAck(MissionAckPacket.MAV_MISSION_UNSUPPORTED, "UNSUPPORTED", LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID));

    assertEquals(Action.FAIL, acknowledgement.action());
  }

  @Test
  void missionCountCanRetryButMissionItemsAbortTransactionOnTimeout() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    assertEquals(TimeoutAction.RETRY_MESSAGE, handler.timeoutAction(messages.get(0)));
    assertEquals(TimeoutAction.RETRY_TRANSACTION, handler.timeoutAction(messages.get(1)));
    assertEquals(2_000L, handler.acknowledgementTimeoutMillis(messages.get(0), 2_000L));
    assertEquals(5_000L, handler.acknowledgementTimeoutMillis(messages.get(1), 2_000L));
  }

  @Test
  void unrelatedPacketIsIgnored() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = handler(messages, 3);

    assertEquals(Action.NOT_RELATED, handler.acknowledge(messages.get(0), mock(MavlinkPacket.class)).action());
  }

  private MavlinkMissionAcknowledgementHandler handler(List<MavlinkMessage> messages, int itemCount) {
    return new MavlinkMissionAcknowledgementHandler(
        messages,
        1,
        itemCount,
        LOCAL_SYSTEM_ID,
        LOCAL_COMPONENT_ID,
        MAV_MISSION_TYPE_MISSION);
  }

  private List<MavlinkMessage> messages(int count) {
    List<MavlinkMessage> messages = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      messages.add(mock(MavlinkMessage.class));
    }
    return messages;
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
    return missionAck(MissionAckPacket.MAV_MISSION_ACCEPTED, "ACCEPTED", LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID);
  }

  private MissionAckPacket missionAck(int type, String typeName, int targetSystem, int targetComponent) {
    MissionAckPacket packet = mock(MissionAckPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.isAccepted()).thenReturn(type == MissionAckPacket.MAV_MISSION_ACCEPTED);
    when(packet.getType()).thenReturn(type);
    when(packet.getTypeName()).thenReturn(typeName);
    when(packet.getTargetSystem()).thenReturn(targetSystem);
    when(packet.getTargetComponent()).thenReturn(targetComponent);
    when(packet.isMissionTypePresent()).thenReturn(true);
    when(packet.getMissionType()).thenReturn(MAV_MISSION_TYPE_MISSION);
    return packet;
  }
}
