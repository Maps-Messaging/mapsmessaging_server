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
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.Acknowledgement;
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.Action;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MavlinkCommandAcknowledgementHandlerTest {

  private final MavlinkCommandAcknowledgementHandler handler = new MavlinkCommandAcknowledgementHandler();

  @Test
  void requiresAcknowledgementReturnsTrueForCommandLong() {
    assertTrue(handler.requiresAcknowledgement(mock(MavlinkCommandLong.class)));
  }

  @Test
  void requiresAcknowledgementReturnsTrueForCommandInt() {
    assertTrue(handler.requiresAcknowledgement(mock(MavlinkCommandInt.class)));
  }

  @Test
  void requiresAcknowledgementReturnsFalseForOtherMessages() {
    assertFalse(handler.requiresAcknowledgement(mock(MavlinkMessage.class)));
  }

  @Test
  void acknowledgeReturnsNotRelatedWhenPacketIsNotCommandAck() {
    Acknowledgement acknowledgement = handler.acknowledge(commandLong(192, 2, 1), mock(MavlinkPacket.class));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void acknowledgeReturnsNotRelatedWhenSentMessageIsNotACommand() {
    Acknowledgement acknowledgement = handler.acknowledge(mock(MavlinkMessage.class), acceptedAck(192));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void acknowledgeReturnsNotRelatedWhenAckIsInvalid() {
    CommandAckPacket ackPacket = mock(CommandAckPacket.class);
    when(ackPacket.isValid()).thenReturn(false);

    Acknowledgement acknowledgement = handler.acknowledge(commandLong(192, 2, 1), ackPacket);

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void acknowledgeReturnsNotRelatedWhenCommandDoesNotMatch() {
    Acknowledgement acknowledgement = handler.acknowledge(commandLong(192, 2, 1), acceptedAck(193));

    assertEquals(Action.NOT_RELATED, acknowledgement.action());
  }

  @Test
  void acknowledgeReturnsAdvanceWhenCommandLongAckIsAccepted() {
    Acknowledgement acknowledgement = handler.acknowledge(commandLong(192, 2, 1), acceptedAck(192));

    assertEquals(Action.ADVANCE, acknowledgement.action());
  }

  @Test
  void acknowledgeReturnsAdvanceWhenCommandIntAckIsAccepted() {
    Acknowledgement acknowledgement = handler.acknowledge(commandInt(192, 2, 1), acceptedAck(192));

    assertEquals(Action.ADVANCE, acknowledgement.action());
  }

  @Test
  void acknowledgeReturnsWaitWhenCommandIsStillInProgress() {
    CommandAckPacket ackPacket = validAck(192);
    when(ackPacket.isInProgress()).thenReturn(true);

    Acknowledgement acknowledgement = handler.acknowledge(commandLong(192, 2, 1), ackPacket);

    assertEquals(Action.WAIT, acknowledgement.action());
  }

  @Test
  void acknowledgeReturnsFailedWhenCommandIsRejected() {
    CommandAckPacket ackPacket = validAck(192);
    when(ackPacket.isRejected()).thenReturn(true);
    when(ackPacket.getResultName()).thenReturn("DENIED");

    Acknowledgement acknowledgement = handler.acknowledge(commandLong(192, 2, 1), ackPacket);

    assertEquals(Action.FAIL, acknowledgement.action());
    assertEquals("MAVLink command 192 rejected with result DENIED", acknowledgement.reason());
  }

  @Test
  void acknowledgeReturnsFailedForUnknownResult() {
    CommandAckPacket ackPacket = validAck(192);
    when(ackPacket.getResult()).thenReturn(99);

    Acknowledgement acknowledgement = handler.acknowledge(commandLong(192, 2, 1), ackPacket);

    assertEquals(Action.FAIL, acknowledgement.action());
    assertEquals("MAVLink command 192 failed with unknown result 99", acknowledgement.reason());
  }

  private MavlinkCommandLong commandLong(int command, int targetSystem, int targetComponent) {
    MavlinkCommandLong commandLong = mock(MavlinkCommandLong.class);
    when(commandLong.getCommand()).thenReturn(command);
    when(commandLong.getTargetSystem()).thenReturn(targetSystem);
    when(commandLong.getTargetComponent()).thenReturn(targetComponent);
    return commandLong;
  }

  private MavlinkCommandInt commandInt(int command, int targetSystem, int targetComponent) {
    MavlinkCommandInt commandInt = mock(MavlinkCommandInt.class);
    when(commandInt.getCommand()).thenReturn(command);
    when(commandInt.getTargetSystem()).thenReturn(targetSystem);
    when(commandInt.getTargetComponent()).thenReturn(targetComponent);
    return commandInt;
  }

  private CommandAckPacket acceptedAck(int command) {
    CommandAckPacket ackPacket = validAck(command);
    when(ackPacket.isAccepted()).thenReturn(true);
    return ackPacket;
  }

  private CommandAckPacket validAck(int command) {
    CommandAckPacket ackPacket = mock(CommandAckPacket.class);
    when(ackPacket.isValid()).thenReturn(true);
    when(ackPacket.getCommand()).thenReturn(command);
    when(ackPacket.isTargetComponentPresent()).thenReturn(false);
    return ackPacket;
  }
}
