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

import static io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory.MAV_CMD_MISSION_START;
import static io.mapsmessaging.state.mavlink.sender.MavlinkMissionAcknowledgementHandler.MAV_MISSION_TYPE_MISSION;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.FAILED;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvNavigationPlan;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4FixedWingUavModel;
import io.mapsmessaging.state.mavlink.model.impl.ugv.GenericPx4UgvModel;
import io.mapsmessaging.state.mavlink.packet.CommandAckPacket;
import io.mapsmessaging.state.mavlink.packet.MissionAckPacket;
import io.mapsmessaging.state.mavlink.packet.MissionRequestIntPacket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class UxvNavigationSenderTest {

  private static final int TARGET_SYSTEM = 10;
  private static final int TARGET_COMPONENT = 1;
  private static final int LOCAL_SYSTEM = 255;
  private static final int LOCAL_COMPONENT = 190;
  private static final int SEQUENCE = 42;

  @Test
  void navigationPlanUploadsMissionThenStartsMission() throws Exception {
    GenericPx4UgvModel model = new GenericPx4UgvModel();
    UxvNavigationPlan plan =
        model.navigate(
            context(),
            List.of(
                new GeoPosition(-33.8688, 151.2093, null, null),
                new GeoPosition(-33.8695, 151.2102, null, null)),
            Duration.ofMinutes(3));

    MavlinkEventSender transport = mock(MavlinkEventSender.class);
    MavlinkMessage missionCount = mock(MavlinkMessage.class);
    UxvModelCommandSet uploadCommandSet = uploadCommandSet(plan.missionPhase().getFirst(), missionCount);
    List<MavlinkSendResult> missionResults = new ArrayList<>();
    List<MavlinkSendResult> startResults = new ArrayList<>();
    AtomicReference<MavlinkEventListSender> startSender = new AtomicReference<>();

    MavlinkMissionAcknowledgementHandler missionAcknowledgementHandler =
        new MavlinkMissionAcknowledgementHandler(
            uploadCommandSet.messages(),
            1,
            2,
            LOCAL_SYSTEM,
            LOCAL_COMPONENT,
            MAV_MISSION_TYPE_MISSION);

    MavlinkEventListSender missionSender =
        new MavlinkEventListSender(
            uploadCommandSet,
            transport,
            missionAcknowledgementHandler,
            result -> {
              missionResults.add(result);
              if (!result.isSuccess()) {
                return;
              }

              MavlinkEventListSender sender =
                  new MavlinkEventListSender(
                      plan.postMissionUploadPhase().getFirst(),
                      transport,
                      new MavlinkCommandAcknowledgementHandler(),
                      startResults::add);
              startSender.set(sender);
              sender.start();
            });

    missionSender.start();
    missionSender.onMavlinkMessage(requestInt(0));
    missionSender.onMavlinkMessage(requestInt(1));
    missionSender.onMavlinkMessage(acceptedMissionAck());

    MavlinkMessage firstWaypoint = uploadCommandSet.messages().get(1);
    MavlinkMessage secondWaypoint = uploadCommandSet.messages().get(2);
    MavlinkMessage startMission =
        plan.postMissionUploadPhase().getFirst().messages().getFirst();

    InOrder inOrder = inOrder(transport);
    inOrder.verify(transport).send(missionCount);
    inOrder.verify(transport).send(firstWaypoint);
    inOrder.verify(transport).send(secondWaypoint);
    inOrder.verify(transport).send(startMission);

    assertEquals(1, missionResults.size());
    assertEquals(SUCCESS, missionResults.getFirst().status());
    assertTrue(startResults.isEmpty());
    assertNotNull(startSender.get());

    startSender.get().onMavlinkMessage(acceptedCommandAck());

    assertEquals(1, startResults.size());
    assertEquals(SUCCESS, startResults.getFirst().status());
    assertEquals(1, startResults.getFirst().total());
    verifyNoMoreInteractions(transport);
  }

  @Test
  void generatedFixedWingRepeatMissionUploadsEveryExactItemBeforeMissionStart() throws Exception {
    GenericPx4FixedWingUavModel model =
        new GenericPx4FixedWingUavModel();

    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                waypoint(
                    new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                    Duration.ZERO,
                    null),
                waypoint(
                    new GeoPosition(-33.8695d, 151.2102d, 130.0d, null),
                    Duration.ofSeconds(10),
                    60.0d)),
            2);

    UxvModelCommandSet generatedMission =
        model.buildMission(context(), missionPlan);
    UxvModelCommandSet startMission =
        model.startMission(context());

    assertEquals(3, generatedMission.messages().size());

    MavlinkMissionItemInt first =
        (MavlinkMissionItemInt) generatedMission.messages().get(0);
    MavlinkMissionItemInt second =
        (MavlinkMissionItemInt) generatedMission.messages().get(1);
    MavlinkMissionItemInt jump =
        (MavlinkMissionItemInt) generatedMission.messages().get(2);

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, first.getCommand());
    assertEquals(0, first.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME, second.getCommand());
    assertEquals(1, second.getMissionSequence());
    assertEquals(10.0f, second.getParam1());
    assertEquals(60.0f, second.getParam3());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP, jump.getCommand());
    assertEquals(2, jump.getMissionSequence());
    assertEquals(0.0f, jump.getParam1());
    assertEquals(1.0f, jump.getParam2());

    MavlinkEventSender transport = mock(MavlinkEventSender.class);
    MavlinkMessage missionCount = mock(MavlinkMessage.class);
    UxvModelCommandSet uploadCommandSet =
        uploadCommandSet(generatedMission, missionCount);
    List<MavlinkSendResult> missionResults = new ArrayList<>();
    List<MavlinkSendResult> startResults = new ArrayList<>();
    AtomicReference<MavlinkEventListSender> startSender = new AtomicReference<>();

    MavlinkEventListSender missionSender =
        new MavlinkEventListSender(
            uploadCommandSet,
            transport,
            new MavlinkMissionAcknowledgementHandler(
                uploadCommandSet.messages(),
                1,
                generatedMission.messages().size(),
                LOCAL_SYSTEM,
                LOCAL_COMPONENT,
                MAV_MISSION_TYPE_MISSION),
            result -> {
              missionResults.add(result);
              if (!result.isSuccess()) {
                return;
              }

              MavlinkEventListSender sender =
                  new MavlinkEventListSender(
                      startMission,
                      transport,
                      new MavlinkCommandAcknowledgementHandler(),
                      startResults::add);
              startSender.set(sender);
              sender.start();
            });

    missionSender.start();
    missionSender.onMavlinkMessage(requestInt(0));
    missionSender.onMavlinkMessage(requestInt(1));
    missionSender.onMavlinkMessage(requestInt(2));
    missionSender.onMavlinkMessage(acceptedMissionAck());

    InOrder inOrder = inOrder(transport);
    inOrder.verify(transport).send(missionCount);
    inOrder.verify(transport).send(first);
    inOrder.verify(transport).send(second);
    inOrder.verify(transport).send(jump);
    inOrder.verify(transport).send(startMission.messages().getFirst());

    assertEquals(1, missionResults.size());
    assertEquals(SUCCESS, missionResults.getFirst().status());
    assertNotNull(startSender.get());
    assertTrue(startResults.isEmpty());

    startSender.get().onMavlinkMessage(acceptedCommandAck());

    assertEquals(1, startResults.size());
    assertEquals(SUCCESS, startResults.getFirst().status());
    verifyNoMoreInteractions(transport);
  }

  @Test
  void rejectedMissionUploadDoesNotStartMission() throws Exception {
    GenericPx4UgvModel model = new GenericPx4UgvModel();
    UxvNavigationPlan plan =
        model.navigate(
            context(),
            List.of(
                new GeoPosition(-33.8688, 151.2093, null, null),
                new GeoPosition(-33.8695, 151.2102, null, null)),
            Duration.ZERO);

    MavlinkEventSender transport = mock(MavlinkEventSender.class);
    MavlinkMessage missionCount = mock(MavlinkMessage.class);
    UxvModelCommandSet uploadCommandSet =
        uploadCommandSet(plan.missionPhase().getFirst(), missionCount);
    List<MavlinkSendResult> missionResults = new ArrayList<>();
    AtomicReference<MavlinkEventListSender> startSender = new AtomicReference<>();

    MavlinkEventListSender missionSender =
        new MavlinkEventListSender(
            uploadCommandSet,
            transport,
            new MavlinkMissionAcknowledgementHandler(
                uploadCommandSet.messages(),
                1,
                2,
                LOCAL_SYSTEM,
                LOCAL_COMPONENT,
                MAV_MISSION_TYPE_MISSION),
            result -> {
              missionResults.add(result);
              if (result.isSuccess()) {
                MavlinkEventListSender sender =
                    new MavlinkEventListSender(
                        plan.postMissionUploadPhase().getFirst(),
                        transport,
                        new MavlinkCommandAcknowledgementHandler(),
                        ignored -> {});
                startSender.set(sender);
                sender.start();
              }
            });

    missionSender.start();
    missionSender.onMavlinkMessage(rejectedMissionAck("INVALID_SEQUENCE"));

    MavlinkMessage startMission =
        plan.postMissionUploadPhase().getFirst().messages().getFirst();

    verify(transport).send(missionCount);
    verify(transport, never()).send(startMission);
    verifyNoMoreInteractions(transport);

    assertEquals(1, missionResults.size());
    assertEquals(FAILED, missionResults.getFirst().status());
    assertEquals(
        "Mission upload failed with result INVALID_SEQUENCE",
        missionResults.getFirst().reason());
    assertNull(startSender.get());
  }

  private static UxvModelCommandSet uploadCommandSet(
      UxvModelCommandSet missionCommandSet,
      MavlinkMessage missionCount) {
    List<MavlinkMessage> uploadMessages =
        new ArrayList<>(missionCommandSet.messages().size() + 1);

    uploadMessages.add(missionCount);
    uploadMessages.addAll(missionCommandSet.messages());

    return UxvModelCommandSet.of(
        UxvOperation.BUILD_MISSION,
        missionCommandSet.modelName(),
        uploadMessages);
  }

  private static PlanItem waypoint(
      GeoPosition position,
      Duration holdDuration,
      Double radiusMeters) {
    return new PlanItem(
        PlanItemType.WAYPOINT,
        position,
        holdDuration,
        radiusMeters,
        null,
        null,
        null,
        null);
  }

  private static UxvCommandContext context() {
    UxvCommandContext context = mock(UxvCommandContext.class);
    when(context.targetSystem()).thenReturn(TARGET_SYSTEM);
    when(context.targetComponent()).thenReturn(TARGET_COMPONENT);
    when(context.sequence()).thenReturn(SEQUENCE);
    return context;
  }

  private static MissionRequestIntPacket requestInt(int sequence) {
    MissionRequestIntPacket packet = mock(MissionRequestIntPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.getSequence()).thenReturn(sequence);
    when(packet.getTargetSystem()).thenReturn(LOCAL_SYSTEM);
    when(packet.getTargetComponent()).thenReturn(LOCAL_COMPONENT);
    when(packet.isMissionTypePresent()).thenReturn(true);
    when(packet.getMissionType()).thenReturn(MAV_MISSION_TYPE_MISSION);
    return packet;
  }

  private static MissionAckPacket acceptedMissionAck() {
    MissionAckPacket packet = mock(MissionAckPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.isAccepted()).thenReturn(true);
    when(packet.getTargetSystem()).thenReturn(LOCAL_SYSTEM);
    when(packet.getTargetComponent()).thenReturn(LOCAL_COMPONENT);
    when(packet.isMissionTypePresent()).thenReturn(true);
    when(packet.getMissionType()).thenReturn(MAV_MISSION_TYPE_MISSION);
    return packet;
  }

  private static MissionAckPacket rejectedMissionAck(String resultName) {
    MissionAckPacket packet = mock(MissionAckPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.isAccepted()).thenReturn(false);
    when(packet.getTypeName()).thenReturn(resultName);
    when(packet.getTargetSystem()).thenReturn(LOCAL_SYSTEM);
    when(packet.getTargetComponent()).thenReturn(LOCAL_COMPONENT);
    when(packet.isMissionTypePresent()).thenReturn(true);
    when(packet.getMissionType()).thenReturn(MAV_MISSION_TYPE_MISSION);
    return packet;
  }

  private static CommandAckPacket acceptedCommandAck() {
    CommandAckPacket packet = mock(CommandAckPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.getCommand()).thenReturn(MAV_CMD_MISSION_START);
    when(packet.isAccepted()).thenReturn(true);
    when(packet.isTargetComponentPresent()).thenReturn(false);
    return packet;
  }
}
