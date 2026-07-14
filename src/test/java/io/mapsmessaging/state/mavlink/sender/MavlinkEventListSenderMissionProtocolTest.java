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
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.MissionAckPacket;
import io.mapsmessaging.state.mavlink.packet.MissionRequestIntPacket;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.state.mavlink.sender.MavlinkMissionAcknowledgementHandler.MAV_MISSION_TYPE_MISSION;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.FAILED;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.SUCCESS;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MavlinkEventListSenderMissionProtocolTest {

  @Test
  void missionProtocolRequestSequenceSendsRequestedItemsAndCompletes() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.requestInt(1));
    sender.onMavlinkMessage(fixture.requestInt(2));
    sender.onMavlinkMessage(fixture.acceptedMissionAck());

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(fixture.missionCountMessage);
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(0));
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(1));
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
    assertEquals(3, fixture.results.get(0).index());
    assertEquals(4, fixture.results.get(0).total());
    assertSame(fixture.itemMessages.get(2), fixture.results.get(0).sentMessage());
  }

  @Test
  void missionProtocolFailsWhenVehicleSkipsFirstItemRequest() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestInt(1));

    verify(fixture.sender).send(fixture.missionCountMessage);
    verify(fixture.sender, never()).send(fixture.itemMessages.get(0));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(1));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals("Mission requested sequence 1 but expected 0", fixture.results.get(0).reason());
  }

  @Test
  void missionProtocolRetransmitsMostRecentlyRequestedItem() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.requestInt(1));
    sender.onMavlinkMessage(fixture.requestInt(2));
    sender.onMavlinkMessage(fixture.acceptedMissionAck());

    verify(fixture.sender).send(fixture.missionCountMessage);
    verify(fixture.sender, times(2)).send(fixture.itemMessages.get(0));
    verify(fixture.sender).send(fixture.itemMessages.get(1));
    verify(fixture.sender).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
  }

  @Test
  void missionProtocolIgnoresStaleRequestAndContinuesUpload() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.requestInt(1));
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.requestInt(2));
    sender.onMavlinkMessage(fixture.acceptedMissionAck());

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(fixture.missionCountMessage);
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(0));
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(1));
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
  }

  @Test
  void missionProtocolRepeatedFinalRequestRetransmitsLostFinalItemAndCompletes() throws Exception {
    Fixture fixture = fixture(2);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.requestInt(1));

    verify(fixture.sender).send(fixture.missionCountMessage);
    verify(fixture.sender).send(fixture.itemMessages.get(0));
    verify(fixture.sender).send(fixture.itemMessages.get(1));
    assertTrue(fixture.results.isEmpty());

    sender.onMavlinkMessage(fixture.requestInt(1));

    verify(fixture.sender, times(2)).send(fixture.itemMessages.get(1));
    assertTrue(fixture.results.isEmpty());

    sender.onMavlinkMessage(fixture.acceptedMissionAck());

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
    assertSame(fixture.itemMessages.get(1), fixture.results.get(0).sentMessage());
  }

  @Test
  void missionProtocolTimeoutRetransmitsFinalItemWhenMissionAckIsLostAndThenCompletes() throws Exception {
    Fixture fixture = fixture(2);

    MavlinkEventListSender sender = fixture.newSender(2);
    sender.start();
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.requestInt(1));

    verify(fixture.sender).send(fixture.missionCountMessage);
    verify(fixture.sender).send(fixture.itemMessages.get(0));
    verify(fixture.sender).send(fixture.itemMessages.get(1));
    assertTrue(fixture.results.isEmpty());

    sender.timeout();

    verify(fixture.sender, times(2)).send(fixture.itemMessages.get(1));
    assertEquals(1, sender.getRetryCount());
    assertTrue(fixture.results.isEmpty());

    sender.onMavlinkMessage(fixture.acceptedMissionAck());

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
    assertSame(fixture.itemMessages.get(1), fixture.results.get(0).sentMessage());

    sender.timeout();

    verify(fixture.sender, times(2)).send(fixture.itemMessages.get(1));
    assertEquals(1, fixture.results.size());
  }

  @Test
  void missionProtocolRetriesMissionCountAfterTimeoutThenContinues() throws Exception {
    Fixture fixture = fixture(2);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.timeout();
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.requestInt(1));
    sender.onMavlinkMessage(fixture.acceptedMissionAck());

    verify(fixture.sender, times(2)).send(fixture.missionCountMessage);
    verify(fixture.sender).send(fixture.itemMessages.get(0));
    verify(fixture.sender).send(fixture.itemMessages.get(1));

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
  }

  @Test
  void missionProtocolIgnoresRequestForAnotherLocalComponentAndAcceptsCorrectRequestAfterwards() throws Exception {
    Fixture fixture = fixture(2);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestInt(0, Fixture.LOCAL_SYSTEM_ID, Fixture.LOCAL_COMPONENT_ID + 1, true, MAV_MISSION_TYPE_MISSION));
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.requestInt(1));
    sender.onMavlinkMessage(fixture.acceptedMissionAck());

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(fixture.missionCountMessage);
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(0));
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(1));

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
  }

  @Test
  void missionProtocolFailsWhenMissionAckArrivesBeforeAllItemsWereRequested() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestInt(0));
    sender.onMavlinkMessage(fixture.acceptedMissionAck());

    verify(fixture.sender).send(fixture.missionCountMessage);
    verify(fixture.sender).send(fixture.itemMessages.get(0));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(1));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals("Mission upload completed before all requested items were sent", fixture.results.get(0).reason());
  }

  @Test
  void missionProtocolFailsWhenMissionAckIsRejected() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.rejectedMissionAck("INVALID_SEQUENCE"));

    verify(fixture.sender).send(fixture.missionCountMessage);
    verify(fixture.sender, never()).send(fixture.itemMessages.get(0));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(1));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals("Mission upload failed with result INVALID_SEQUENCE", fixture.results.get(0).reason());
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

  private static class Fixture {

    private static final int LOCAL_SYSTEM_ID = 255;
    private static final int LOCAL_COMPONENT_ID = 190;

    private final MavlinkMessage missionCountMessage;
    private final List<MavlinkMessage> itemMessages;
    private final List<MavlinkMessage> messages;
    private final List<MavlinkSendResult> results;
    private final MavlinkEventSender sender;
    private final MavlinkMissionAcknowledgementHandler acknowledgementHandler;

    private Fixture(int itemCount) {
      this.missionCountMessage = mock(MavlinkMessage.class);
      this.itemMessages = new ArrayList<>();
      this.messages = new ArrayList<>();
      this.results = new ArrayList<>();
      this.sender = mock(MavlinkEventSender.class);

      messages.add(missionCountMessage);
      for (int i = 0; i < itemCount; i++) {
        MavlinkMessage itemMessage = mock(MavlinkMessage.class);
        itemMessages.add(itemMessage);
        messages.add(itemMessage);
      }

      this.acknowledgementHandler = new MavlinkMissionAcknowledgementHandler(messages, 1, itemCount, LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID, MAV_MISSION_TYPE_MISSION);
    }

    private MavlinkEventListSender newSender() {
      return new MavlinkEventListSender(commandSet(messages), sender, acknowledgementHandler, results::add);
    }

    private MavlinkEventListSender newSender(int maxRetries) {
      return new MavlinkEventListSender(commandSet(messages), sender, acknowledgementHandler, results::add, maxRetries);
    }

    private MissionRequestIntPacket requestInt(int sequence) {
      return requestInt(sequence, LOCAL_SYSTEM_ID, LOCAL_COMPONENT_ID, true, MAV_MISSION_TYPE_MISSION);
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
      MissionAckPacket packet = mock(MissionAckPacket.class);
      when(packet.isValid()).thenReturn(true);
      when(packet.isAccepted()).thenReturn(true);
      when(packet.getTargetSystem()).thenReturn(LOCAL_SYSTEM_ID);
      when(packet.getTargetComponent()).thenReturn(LOCAL_COMPONENT_ID);
      when(packet.isMissionTypePresent()).thenReturn(true);
      when(packet.getMissionType()).thenReturn(MAV_MISSION_TYPE_MISSION);
      return packet;
    }

    private MissionAckPacket rejectedMissionAck(String resultName) {
      MissionAckPacket packet = mock(MissionAckPacket.class);
      when(packet.isValid()).thenReturn(true);
      when(packet.isAccepted()).thenReturn(false);
      when(packet.getTypeName()).thenReturn(resultName);
      when(packet.getTargetSystem()).thenReturn(LOCAL_SYSTEM_ID);
      when(packet.getTargetComponent()).thenReturn(LOCAL_COMPONENT_ID);
      when(packet.isMissionTypePresent()).thenReturn(true);
      when(packet.getMissionType()).thenReturn(MAV_MISSION_TYPE_MISSION);
      return packet;
    }
  }
}