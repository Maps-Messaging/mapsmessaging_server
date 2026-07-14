/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at:
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

import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.MissionAckPacket;
import io.mapsmessaging.state.mavlink.packet.MissionRequestIntPacket;
import io.mapsmessaging.state.mavlink.packet.MissionRequestPacket;
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.Acknowledgement;
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.Action;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MavlinkMissionAcknowledgementHandlerTest {

  @Test
  void constructorRejectsNullMessageList() {
    assertThrows(NullPointerException.class, () -> new MavlinkMissionAcknowledgementHandler(null, 3));
  }

  @Test
  void constructorRejectsNegativeMissionItemOffset() {
    assertThrows(IllegalArgumentException.class, () -> new MavlinkMissionAcknowledgementHandler(messages(4), -1, 3, 255, 190, 0));
  }

  @Test
  void constructorRejectsNegativeMissionItemCount() {
    assertThrows(IllegalArgumentException.class, () -> new MavlinkMissionAcknowledgementHandler(messages(4), 1, -1, 255, 190, 0));
  }

  @Test
  void constructorRejectsTooFewMessagesForMissionItems() {
    assertThrows(IllegalArgumentException.class, () -> new MavlinkMissionAcknowledgementHandler(messages(3), 1, 3, 255, 190, 0));
  }

  @Test
  void constructorRejectsNullMissionMessage() {
    List<MavlinkMessage> messages = new ArrayList<>();
    messages.add(mock(MavlinkMessage.class));
    messages.add(null);

    assertThrows(NullPointerException.class, () -> new MavlinkMissionAcknowledgementHandler(messages, 1));
  }

  @Test
  void requiresAcknowledgementReturnsTrueForMissionMessagesOnly() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);

    assertTrue(handler.requiresAcknowledgement(messages.get(0)));
    assertTrue(handler.requiresAcknowledgement(messages.get(3)));
    assertFalse(handler.requiresAcknowledgement(mock(MavlinkMessage.class)));
  }

  @Test
  void acknowledgeReturnsNotRelatedWhenSentMessageIsNotPartOfMission() {
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages(4), 3);

    Acknowledgement acknowledgement = handler.acknowledge(mock(MavlinkMessage.class), requestInt(0));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void requestIntForExpectedSequenceReturnsSendIndex() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestInt(0));

    assertEquals(Action.SEND_INDEX, acknowledgement.action());
    assertEquals(1, acknowledgement.index());
  }

  @Test
  void requestForExpectedSequenceReturnsSendIndex() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), request(0));

    assertEquals(Action.SEND_INDEX, acknowledgement.action());
    assertEquals(1, acknowledgement.index());
  }

  @Test
  void requestSequenceMapsThroughMissionItemOffset() {
    List<MavlinkMessage> messages = messages(5);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 2, 3, 255, 190, 0);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestInt(0));

    assertEquals(Action.SEND_INDEX, acknowledgement.action());
    assertEquals(2, acknowledgement.index());
  }

  @Test
  void requestSkippingSequenceFails() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestInt(1));

    assertEquals(Action.FAIL, acknowledgement.action());
    assertEquals("Mission requested sequence 1 but expected 0", acknowledgement.reason());
  }

  @Test
  void requestRepeatingMostRecentSequenceRequestsRetransmission() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler =
        new MavlinkMissionAcknowledgementHandler(messages, 3);

    handler.acknowledge(messages.get(0), requestInt(0));
    Acknowledgement acknowledgement =
        handler.acknowledge(messages.get(1), requestInt(0));

    assertEquals(Action.SEND_INDEX, acknowledgement.action());
    assertEquals(1, acknowledgement.index());
  }

  @Test
  void staleRequestOlderThanMostRecentSequenceIsIgnored() {
    List<MavlinkMessage> messages = messages(5);
    MavlinkMissionAcknowledgementHandler handler =
        new MavlinkMissionAcknowledgementHandler(messages, 4);

    handler.acknowledge(messages.get(0), requestInt(0));
    handler.acknowledge(messages.get(1), requestInt(1));
    Acknowledgement acknowledgement =
        handler.acknowledge(messages.get(2), requestInt(0));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void repeatedFinalItemRequestBeforeMissionAckRequestsRetransmission() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler =
        new MavlinkMissionAcknowledgementHandler(messages, 3);

    handler.acknowledge(messages.get(0), requestInt(0));
    handler.acknowledge(messages.get(1), requestInt(1));
    handler.acknowledge(messages.get(2), requestInt(2));

    Acknowledgement acknowledgement =
        handler.acknowledge(messages.get(3), requestInt(2));

    assertEquals(Action.SEND_INDEX, acknowledgement.action());
    assertEquals(3, acknowledgement.index());
  }

  @Test
  void requestOutsideRangeFails() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestInt(3));

    assertEquals(Action.FAIL, acknowledgement.action());
    assertEquals("Mission requested sequence 3 outside range 0..2", acknowledgement.reason());
  }

  @Test
  void invalidRequestReturnsNotRelated() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);
    MissionRequestIntPacket requestPacket = requestInt(0);
    when(requestPacket.isValid()).thenReturn(false);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestPacket);

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void requestWithWrongTargetSystemReturnsNotRelated() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 1, 3, 255, 190, 0);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestInt(0, 254, 190, false, 0));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void requestWithWrongTargetComponentReturnsNotRelated() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 1, 3, 255, 190, 0);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestInt(0, 255, 191, false, 0));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void requestWithBroadcastTargetSystemAndComponentMatches() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 1, 3, 255, 190, 0);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestInt(0, 0, 0, false, 0));

    assertEquals(Action.SEND_INDEX, acknowledgement.action());
    assertEquals(1, acknowledgement.index());
  }

  @Test
  void requestWithWrongMissionTypeReturnsNotRelated() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 1, 3, 255, 190, 0);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestInt(0, 255, 190, true, 1));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void requestWithoutMissionTypeMatchesForCompatibility() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 1, 3, 255, 190, 0);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), requestInt(0, 255, 190, false, 1));

    assertEquals(Action.SEND_INDEX, acknowledgement.action());
  }

  @Test
  void acceptedMissionAckCompletesAfterAllItemsRequested() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);

    handler.acknowledge(messages.get(0), requestInt(0));
    handler.acknowledge(messages.get(1), requestInt(1));
    handler.acknowledge(messages.get(2), requestInt(2));
    Acknowledgement acknowledgement = handler.acknowledge(messages.get(3), acceptedMissionAck());

    assertEquals(Action.COMPLETE, acknowledgement.action());
  }

  @Test
  void acceptedMissionAckFailsBeforeAllItemsRequested() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);

    handler.acknowledge(messages.get(0), requestInt(0));
    Acknowledgement acknowledgement = handler.acknowledge(messages.get(1), acceptedMissionAck());

    assertEquals(Action.FAIL, acknowledgement.action());
    assertEquals("Mission upload completed before all requested items were sent", acknowledgement.reason());
  }

  @Test
  void rejectedMissionAckFails() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);
    MissionAckPacket missionAckPacket = missionAck(false, "INVALID_SEQUENCE", 13, 0, 0, false, 0);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), missionAckPacket);

    assertEquals(Action.FAIL, acknowledgement.action());
    assertEquals("Mission upload failed with result INVALID_SEQUENCE", acknowledgement.reason());
  }

  @Test
  void missionAckWithWrongMissionTypeReturnsNotRelated() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 1, 3, 255, 190, 0);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), missionAck(true, "ACCEPTED", 0, 255, 190, true, 1));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void missionAckWithWrongTargetReturnsNotRelated() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 1, 3, 255, 190, 0);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), missionAck(true, "ACCEPTED", 0, 255, 191, false, 0));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void wrongPacketReturnsNotRelated() {
    List<MavlinkMessage> messages = messages(4);
    MavlinkMissionAcknowledgementHandler handler = new MavlinkMissionAcknowledgementHandler(messages, 3);

    Acknowledgement acknowledgement = handler.acknowledge(messages.get(0), mock(MavlinkPacket.class));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  private List<MavlinkMessage> messages(int count) {
    List<MavlinkMessage> messages = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      messages.add(mock(MavlinkMessage.class));
    }
    return messages;
  }

  private MissionRequestPacket request(int sequence) {
    MissionRequestPacket packet = mock(MissionRequestPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.getSequence()).thenReturn(sequence);
    when(packet.getTargetSystem()).thenReturn(0);
    when(packet.getTargetComponent()).thenReturn(0);
    when(packet.isMissionTypePresent()).thenReturn(false);
    return packet;
  }

  private MissionRequestIntPacket requestInt(int sequence) {
    return requestInt(sequence, 0, 0, false, 0);
  }

  private MissionRequestIntPacket requestInt(int sequence, int targetSystem, int targetComponent, boolean missionTypePresent, int missionType) {
    MissionRequestIntPacket packet = mock(MissionRequestIntPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.getSequence()).thenReturn(sequence);
    when(packet.getTargetSystem()).thenReturn(targetSystem);
    when(packet.getTargetComponent()).thenReturn(targetComponent);
    when(packet.isMissionTypePresent()).thenReturn(missionTypePresent);
    when(packet.getMissionType()).thenReturn(missionType);
    return packet;
  }

  private MissionAckPacket acceptedMissionAck() {
    return missionAck(true, "ACCEPTED", 0, 0, 0, false, 0);
  }

  private MissionAckPacket missionAck(boolean accepted, String typeName, int type, int targetSystem, int targetComponent, boolean missionTypePresent, int missionType) {
    MissionAckPacket packet = mock(MissionAckPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.isAccepted()).thenReturn(accepted);
    when(packet.getType()).thenReturn(type);
    when(packet.getTypeName()).thenReturn(typeName);
    when(packet.getTargetSystem()).thenReturn(targetSystem);
    when(packet.getTargetComponent()).thenReturn(targetComponent);
    when(packet.isMissionTypePresent()).thenReturn(missionTypePresent);
    when(packet.getMissionType()).thenReturn(missionType);
    return packet;
  }
}