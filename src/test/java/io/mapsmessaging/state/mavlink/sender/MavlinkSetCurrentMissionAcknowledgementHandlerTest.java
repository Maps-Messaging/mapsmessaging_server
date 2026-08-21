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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.packet.CommandAckPacket;
import io.mapsmessaging.state.mavlink.packet.MissionCurrentPacket;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MavlinkSetCurrentMissionAcknowledgementHandlerTest {

  private static final int EXPECTED_SEQUENCE = 3;
  private static final MavlinkCommandLong COMMAND =
      MavlinkCommandLongFactory.setMissionCurrent(10, 1, 7, EXPECTED_SEQUENCE, false);

  @Test
  void commandAckThenFreshMatchingMissionCurrentCompletes() {
    MavlinkSetCurrentMissionAcknowledgementHandler handler = handler();

    assertEquals(
        MavlinkAcknowledgementHandler.Action.WAIT,
        handler.acknowledge(COMMAND, commandAck(0)).action());
    assertEquals(
        MavlinkAcknowledgementHandler.Action.COMPLETE,
        handler.acknowledge(COMMAND, missionCurrent(EXPECTED_SEQUENCE)).action());
  }

  @Test
  void missionCurrentBeforeCommandAckStillRequiresBothConfirmations() {
    MavlinkSetCurrentMissionAcknowledgementHandler handler = handler();

    assertEquals(
        MavlinkAcknowledgementHandler.Action.WAIT,
        handler.acknowledge(COMMAND, missionCurrent(EXPECTED_SEQUENCE)).action());
    assertEquals(
        MavlinkAcknowledgementHandler.Action.COMPLETE,
        handler.acknowledge(COMMAND, commandAck(0)).action());
  }

  @Test
  void staleDifferentMissionCurrentDoesNotConfirmSelection() {
    MavlinkSetCurrentMissionAcknowledgementHandler handler = handler();

    assertEquals(
        MavlinkAcknowledgementHandler.Action.WAIT,
        handler.acknowledge(COMMAND, commandAck(0)).action());
    assertEquals(
        MavlinkAcknowledgementHandler.Action.NOT_RELATED,
        handler.acknowledge(COMMAND, missionCurrent(2)).action());
  }

  @Test
  void rejectedCommandAckFails() {
    MavlinkSetCurrentMissionAcknowledgementHandler handler = handler();

    assertEquals(
        MavlinkAcknowledgementHandler.Action.FAIL,
        handler.acknowledge(COMMAND, commandAck(2)).action());
  }

  private MavlinkSetCurrentMissionAcknowledgementHandler handler() {
    return new MavlinkSetCurrentMissionAcknowledgementHandler(EXPECTED_SEQUENCE);
  }

  private CommandAckPacket commandAck(int result) {
    return new CommandAckPacket(
        frame(
            "COMMAND_ACK",
            Map.of(
                "command",
                MavlinkCommandLongFactory.MAV_CMD_DO_SET_MISSION_CURRENT,
                "result",
                result)));
  }

  private MissionCurrentPacket missionCurrent(int sequence) {
    return new MissionCurrentPacket(
        frame("MISSION_CURRENT", Map.of("seq", sequence)));
  }

  private ProcessedFrame frame(String type, Map<String, Object> fields) {
    return new ProcessedFrame(type, null, fields, true, List.of(), null);
  }
}
