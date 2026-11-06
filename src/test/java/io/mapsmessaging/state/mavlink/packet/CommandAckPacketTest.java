package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandAckPacketTest {

  @Test
  void acceptedAndInProgressResultsHaveDistinctLifecycleSemantics() {
    CommandAckPacket accepted = packet(CommandAckPacket.MAV_RESULT_ACCEPTED);
    assertTrue(accepted.isAccepted());
    assertFalse(accepted.isInProgress());
    assertFalse(accepted.isRejected());
    assertTrue(accepted.isTerminal());
    assertEquals("ACCEPTED", accepted.getResultName());

    CommandAckPacket inProgress = packet(CommandAckPacket.MAV_RESULT_IN_PROGRESS);
    assertFalse(inProgress.isAccepted());
    assertTrue(inProgress.isInProgress());
    assertFalse(inProgress.isRejected());
    assertFalse(inProgress.isTerminal());
    assertEquals("IN_PROGRESS", inProgress.getResultName());
  }

  @Test
  void allDefinedFailureResultsAreRejectedAndTerminal() {
    int[] failures = {
        CommandAckPacket.MAV_RESULT_TEMPORARILY_REJECTED,
        CommandAckPacket.MAV_RESULT_DENIED,
        CommandAckPacket.MAV_RESULT_UNSUPPORTED,
        CommandAckPacket.MAV_RESULT_FAILED,
        CommandAckPacket.MAV_RESULT_CANCELLED,
        CommandAckPacket.MAV_RESULT_COMMAND_LONG_ONLY,
        CommandAckPacket.MAV_RESULT_COMMAND_INT_ONLY,
        CommandAckPacket.MAV_RESULT_COMMAND_UNSUPPORTED_MAV_FRAME
    };

    for (int result : failures) {
      CommandAckPacket packet = packet(result);
      assertTrue(packet.isRejected(), "Expected rejected result " + result);
      assertTrue(packet.isTerminal(), "Expected terminal result " + result);
      assertFalse(packet.isAccepted());
      assertFalse(packet.isInProgress());
      assertNotEquals("UNKNOWN", packet.getResultName());
    }
  }

  @Test
  void optionalExtensionFieldsExposePresenceSeparatelyFromSentinel() {
    CommandAckPacket complete = new CommandAckPacket(frame(Map.of(
        "command", 400,
        "result", 0,
        "progress", 42,
        "result_param2", 7,
        "target_system", 255,
        "target_component", 190
    ), true));

    assertEquals(400, complete.getCommand());
    assertTrue(complete.isProgressPresent());
    assertEquals(42, complete.getProgress());
    assertTrue(complete.isResultParameter2Present());
    assertEquals(7, complete.getResultParameter2());
    assertTrue(complete.isTargetSystemPresent());
    assertEquals(255, complete.getTargetSystem());
    assertTrue(complete.isTargetComponentPresent());
    assertEquals(190, complete.getTargetComponent());

    CommandAckPacket minimal = new CommandAckPacket(frame(Map.of(
        "command", 400,
        "result", 0
    ), false));

    assertFalse(minimal.isProgressPresent());
    assertEquals(-1, minimal.getProgress());
    assertFalse(minimal.isResultParameter2Present());
    assertFalse(minimal.isTargetSystemPresent());
    assertFalse(minimal.isTargetComponentPresent());
    assertFalse(minimal.isValid());
  }

  @Test
  void unknownResultIsTerminalButNotClassifiedAsRejected() {
    CommandAckPacket packet = packet(99);

    assertFalse(packet.isAccepted());
    assertFalse(packet.isInProgress());
    assertFalse(packet.isRejected());
    assertTrue(packet.isTerminal());
    assertEquals("UNKNOWN", packet.getResultName());
  }

  private static CommandAckPacket packet(int result) {
    return new CommandAckPacket(frame(Map.of("command", 1, "result", result), true));
  }

  private static ProcessedFrame frame(Map<String, Object> fields, boolean valid) {
    return new ProcessedFrame("COMMAND_ACK", null, fields, valid, List.of(), null);
  }
}
