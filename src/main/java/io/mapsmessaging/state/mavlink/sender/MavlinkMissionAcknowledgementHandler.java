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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MavlinkMissionAcknowledgementHandler implements MavlinkAcknowledgementHandler {

  public static final int MAV_MISSION_TYPE_MISSION = 0;
  public static final int ANY_LOCAL_ID = -1;
  public static final int DEFAULT_MISSION_ITEM_OFFSET = 1;

  private final Map<MavlinkMessage, Boolean> missionMessages;
  private final int missionItemOffset;
  private final int missionItemCount;
  private final int localSystemId;
  private final int localComponentId;
  private final int missionType;
  private int expectedSequence;

  public MavlinkMissionAcknowledgementHandler(List<MavlinkMessage> missionMessages, int missionItemCount) {
    this(missionMessages, DEFAULT_MISSION_ITEM_OFFSET, missionItemCount, ANY_LOCAL_ID, ANY_LOCAL_ID, MAV_MISSION_TYPE_MISSION);
  }

  public MavlinkMissionAcknowledgementHandler(List<MavlinkMessage> missionMessages, int missionItemOffset, int missionItemCount, int localSystemId, int localComponentId, int missionType) {
    if (missionItemOffset < 0) {
      throw new IllegalArgumentException("missionItemOffset must not be negative");
    }
    if (missionItemCount < 0) {
      throw new IllegalArgumentException("missionItemCount must not be negative");
    }

    List<MavlinkMessage> messages = Objects.requireNonNull(missionMessages, "missionMessages must not be null");
    int requiredMessages = missionItemOffset + missionItemCount;
    if (messages.size() < requiredMessages) {
      throw new IllegalArgumentException("missionMessages must contain the mission prefix and all mission items");
    }

    this.missionMessages = new IdentityHashMap<>();
    for (MavlinkMessage message : messages) {
      this.missionMessages.put(Objects.requireNonNull(message, "missionMessages must not contain null messages"), Boolean.TRUE);
    }

    this.missionItemOffset = missionItemOffset;
    this.missionItemCount = missionItemCount;
    this.localSystemId = localSystemId;
    this.localComponentId = localComponentId;
    this.missionType = missionType;
    this.expectedSequence = 0;
  }

  @Override
  public boolean requiresAcknowledgement(MavlinkMessage sentMessage) {
    return missionMessages.containsKey(sentMessage);
  }

  @Override
  public Acknowledgement acknowledge(MavlinkMessage sentMessage, MavlinkPacket receivedMessage) {
    if (!requiresAcknowledgement(sentMessage)) {
      return Acknowledgement.notRelated();
    }

    if (receivedMessage instanceof MissionRequestIntPacket missionRequestIntPacket) {
      return acknowledgeRequest(missionRequestIntPacket.isValid(), missionRequestIntPacket.getTargetSystem(), missionRequestIntPacket.getTargetComponent(), missionRequestIntPacket.isMissionTypePresent(), missionRequestIntPacket.getMissionType(), missionRequestIntPacket.getSequence());
    }

    if (receivedMessage instanceof MissionRequestPacket missionRequestPacket) {
      return acknowledgeRequest(missionRequestPacket.isValid(), missionRequestPacket.getTargetSystem(), missionRequestPacket.getTargetComponent(), missionRequestPacket.isMissionTypePresent(), missionRequestPacket.getMissionType(), missionRequestPacket.getSequence());
    }

    if (receivedMessage instanceof MissionAckPacket missionAckPacket) {
      return acknowledgeMissionAck(sentMessage, missionAckPacket);
    }

    return Acknowledgement.notRelated();
  }

  private Acknowledgement acknowledgeRequest(boolean valid, int targetSystem, int targetComponent, boolean missionTypePresent, int packetMissionType, int sequence) {
    if (!valid) {
      return Acknowledgement.notRelated();
    }

    if (!targetMatches(targetSystem, targetComponent)) {
      return Acknowledgement.notRelated();
    }

    if (!missionTypeMatches(missionTypePresent, packetMissionType)) {
      return Acknowledgement.notRelated();
    }

    if (sequence < 0 || sequence >= missionItemCount) {
      return Acknowledgement.fail("Mission requested sequence " + sequence + " outside range 0.." + Math.max(0, missionItemCount - 1));
    }

    if (sequence != expectedSequence) {
      return Acknowledgement.fail("Mission requested sequence " + sequence + " but expected " + expectedSequence);
    }

    expectedSequence++;
    return Acknowledgement.sendIndex(missionItemOffset + sequence);
  }

  private Acknowledgement acknowledgeMissionAck(MavlinkMessage sentMessage, MissionAckPacket missionAckPacket) {
    if (!missionAckPacket.isValid()) {
      return Acknowledgement.notRelated();
    }

    if (!targetMatches(missionAckPacket.getTargetSystem(), missionAckPacket.getTargetComponent())) {
      return Acknowledgement.notRelated();
    }

    if (!missionTypeMatches(missionAckPacket.isMissionTypePresent(), missionAckPacket.getMissionType())) {
      return Acknowledgement.notRelated();
    }

    if (!missionAckPacket.isAccepted()) {
      return Acknowledgement.fail("Mission upload failed with result " + missionAckPacket.getTypeName());
    }

    if (expectedSequence != missionItemCount) {
      return Acknowledgement.fail("Mission upload completed before all requested items were sent");
    }

    return Acknowledgement.complete();
  }

  private boolean targetMatches(int targetSystem, int targetComponent) {
    return systemMatches(targetSystem) && componentMatches(targetComponent);
  }

  private boolean systemMatches(int targetSystem) {
    return localSystemId == ANY_LOCAL_ID || targetSystem == 0 || targetSystem == localSystemId;
  }

  private boolean componentMatches(int targetComponent) {
    return localComponentId == ANY_LOCAL_ID || targetComponent == 0 || targetComponent == localComponentId;
  }

  private boolean missionTypeMatches(boolean missionTypePresent, int packetMissionType) {
    return !missionTypePresent || packetMissionType == missionType;
  }
}
