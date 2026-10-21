/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.state.mavlink.sender;

import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionCount;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionCountFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MavlinkCommandSetPreparer {

  private MavlinkCommandSetPreparer() {
  }

  public static PreparedMavlinkCommandSet prepare(UxvModelCommandSet commandSet) {
    Objects.requireNonNull(commandSet, "commandSet must not be null");
    if (commandSet.operation() == UxvOperation.BUILD_MISSION) {
      return prepareMission(commandSet);
    }
    if (commandSet.operation() == UxvOperation.SET_CURRENT_MISSION) {
      return prepareSetCurrentMission(commandSet);
    }
    return new PreparedMavlinkCommandSet(commandSet, new MavlinkCommandAcknowledgementHandler());
  }

  private static PreparedMavlinkCommandSet prepareSetCurrentMission(
      UxvModelCommandSet commandSet) {
    if (commandSet.messages().size() != 1
        || !(commandSet.messages().getFirst() instanceof MavlinkCommandLong command)
        || command.getCommand()
            != MavlinkCommandLongFactory.MAV_CMD_DO_SET_MISSION_CURRENT) {
      throw new IllegalArgumentException(
          "SET_CURRENT_MISSION must contain one MAV_CMD_DO_SET_MISSION_CURRENT command");
    }
    int missionSequence = Math.round(command.getParam1());
    if (missionSequence < 0 || command.getParam1() != missionSequence) {
      throw new IllegalArgumentException(
          "SET_CURRENT_MISSION sequence must be a non-negative integer");
    }
    return new PreparedMavlinkCommandSet(
        commandSet,
        new MavlinkSetCurrentMissionAcknowledgementHandler(missionSequence));
  }

  private static PreparedMavlinkCommandSet prepareMission(UxvModelCommandSet commandSet) {
    List<MavlinkMessage> messages = commandSet.messages();
    if (messages.isEmpty()) {
      throw new IllegalArgumentException("Mission command set contains no messages");
    }

    List<MavlinkMessage> uploadMessages;
    int itemCount;
    if (messages.get(0) instanceof MavlinkMissionCount missionCount) {
      itemCount = missionCount.getCount();
      if (itemCount != messages.size() - 1) {
        throw new IllegalArgumentException("MISSION_COUNT does not match the number of mission items");
      }
      validateMissionItems(messages.subList(1, messages.size()), missionCount.getTargetSystem(), missionCount.getTargetComponent());
      uploadMessages = List.copyOf(messages);
    } else {
      MavlinkMissionItemInt firstItem = requireMissionItem(messages.get(0), 0);
      validateMissionItems(messages, firstItem.getTargetSystem(), firstItem.getTargetComponent());
      itemCount = messages.size();
      uploadMessages = new ArrayList<>(itemCount + 1);
      uploadMessages.add(MavlinkMissionCountFactory.mission(firstItem.getTargetSystem(), firstItem.getTargetComponent(), itemCount));
      uploadMessages.addAll(messages);
      uploadMessages = List.copyOf(uploadMessages);
    }

    UxvModelCommandSet prepared = UxvModelCommandSet.of(commandSet.operation(), commandSet.modelName(), uploadMessages);
    return new PreparedMavlinkCommandSet(prepared, new MavlinkMissionAcknowledgementHandler(uploadMessages, itemCount));
  }

  private static void validateMissionItems(List<MavlinkMessage> messages, int targetSystem, int targetComponent) {
    for (int index = 0; index < messages.size(); index++) {
      MavlinkMissionItemInt item = requireMissionItem(messages.get(index), index);
      if (item.getTargetSystem() != targetSystem || item.getTargetComponent() != targetComponent) {
        throw new IllegalArgumentException("Mission item " + index + " targets a different vehicle or component");
      }
      if (item.getMissionSequence() != index) {
        throw new IllegalArgumentException("Mission item " + index + " has sequence " + item.getMissionSequence());
      }
    }
  }

  private static MavlinkMissionItemInt requireMissionItem(MavlinkMessage message, int index) {
    if (!(message instanceof MavlinkMissionItemInt missionItem)) {
      throw new IllegalArgumentException("Mission message " + index + " is not MISSION_ITEM_INT");
    }
    return missionItem;
  }
}
