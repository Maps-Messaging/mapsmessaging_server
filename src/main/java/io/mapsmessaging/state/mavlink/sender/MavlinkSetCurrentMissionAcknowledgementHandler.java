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

import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.packet.CommandAckPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.MissionCurrentPacket;

public final class MavlinkSetCurrentMissionAcknowledgementHandler
    implements MavlinkAcknowledgementHandler {

  private final int expectedMissionSequence;

  public MavlinkSetCurrentMissionAcknowledgementHandler(int expectedMissionSequence) {
    if (expectedMissionSequence < 0) {
      throw new IllegalArgumentException("expectedMissionSequence must not be negative");
    }
    this.expectedMissionSequence = expectedMissionSequence;
  }

  @Override
  public boolean requiresAcknowledgement(MavlinkMessage sentMessage) {
    return sentMessage instanceof MavlinkCommandLong command
        && command.getCommand()
            == MavlinkCommandLongFactory.MAV_CMD_DO_SET_MISSION_CURRENT;
  }

  @Override
  public synchronized Acknowledgement acknowledge(
      MavlinkMessage sentMessage, MavlinkPacket receivedMessage) {
    if (!requiresAcknowledgement(sentMessage)) {
      return Acknowledgement.notRelated();
    }

    if (receivedMessage instanceof CommandAckPacket commandAck) {
      if (!commandAck.isValid()
          || commandAck.getCommand()
              != MavlinkCommandLongFactory.MAV_CMD_DO_SET_MISSION_CURRENT) {
        return Acknowledgement.notRelated();
      }
      if (commandAck.isAccepted()) {
        return Acknowledgement.waitForMore();
      }
      if (commandAck.isInProgress()) {
        return Acknowledgement.waitForMore();
      }
      return Acknowledgement.fail(
          "MAVLink set-current-mission command rejected with result "
              + commandAck.getResultName());
    }

    if (receivedMessage instanceof MissionCurrentPacket missionCurrent) {
      if (!missionCurrent.isValid()
          || missionCurrent.getSequence() != expectedMissionSequence) {
        return Acknowledgement.notRelated();
      }
      return Acknowledgement.complete();
    }

    return Acknowledgement.notRelated();
  }
}
