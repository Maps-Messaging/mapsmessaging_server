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

import io.mapsmessaging.state.mavlink.messages.MavlinkCommandInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.packet.CommandAckPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;

public class MavlinkCommandAcknowledgementHandler implements MavlinkAcknowledgementHandler {

  private final int localSystemId;
  private final int localComponentId;

  public MavlinkCommandAcknowledgementHandler() {
    this(-1, -1);
  }

  public MavlinkCommandAcknowledgementHandler(int localSystemId, int localComponentId) {
    this.localSystemId = localSystemId;
    this.localComponentId = localComponentId;
  }

  @Override
  public boolean requiresAcknowledgement(MavlinkMessage sentMessage) {
    return sentMessage instanceof MavlinkCommandLong || sentMessage instanceof MavlinkCommandInt;
  }

  @Override
  public Acknowledgement acknowledge(MavlinkMessage sentMessage, MavlinkPacket receivedMessage) {
    if (!(receivedMessage instanceof CommandAckPacket commandAckPacket)) {
      return Acknowledgement.notRelated();
    }

    CommandDetails commandDetails = commandDetails(sentMessage);
    if (commandDetails == null) {
      return Acknowledgement.notRelated();
    }

    if (!commandAckPacket.isValid()) {
      return Acknowledgement.notRelated();
    }

    if (commandAckPacket.getCommand() != commandDetails.command()) {
      return Acknowledgement.notRelated();
    }

    if (!ackTargetSystemMatches(commandAckPacket)) {
      return Acknowledgement.notRelated();
    }

    if (!ackTargetComponentMatches(commandAckPacket)) {
      return Acknowledgement.notRelated();
    }

    if (commandAckPacket.isAccepted()) {
      return Acknowledgement.advance();
    }

    if (commandAckPacket.isInProgress()) {
      return Acknowledgement.waitForMore();
    }

    if (commandAckPacket.isRejected()) {
      return Acknowledgement.fail("MAVLink command " + commandAckPacket.getCommand() + " rejected with result " + commandAckPacket.getResultName());
    }

    return Acknowledgement.fail("MAVLink command " + commandAckPacket.getCommand() + " failed with unknown result " + commandAckPacket.getResult());
  }

  private boolean ackTargetSystemMatches(CommandAckPacket commandAckPacket) {
    if (localSystemId < 0 || !commandAckPacket.isTargetSystemPresent()) {
      return true;
    }

    return commandAckPacket.getTargetSystem() == localSystemId;
  }

  private boolean ackTargetComponentMatches(CommandAckPacket commandAckPacket) {
    if (localComponentId < 0 || !commandAckPacket.isTargetComponentPresent()) {
      return true;
    }

    return commandAckPacket.getTargetComponent() == localComponentId;
  }

  private CommandDetails commandDetails(MavlinkMessage sentMessage) {
    if (sentMessage instanceof MavlinkCommandLong commandLong) {
      return new CommandDetails(
          commandLong.getCommand(),
          commandLong.getTargetSystem(),
          commandLong.getTargetComponent()
      );
    }

    if (sentMessage instanceof MavlinkCommandInt commandInt) {
      return new CommandDetails(
          commandInt.getCommand(),
          commandInt.getTargetSystem(),
          commandInt.getTargetComponent()
      );
    }

    return null;
  }

  private record CommandDetails(int command, int targetSystem, int targetComponent) {
  }
}