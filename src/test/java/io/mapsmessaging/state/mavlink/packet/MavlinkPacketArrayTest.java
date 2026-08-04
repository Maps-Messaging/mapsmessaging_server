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
 */

package io.mapsmessaging.state.mavlink.packet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MavlinkPacketArrayTest {

  private final TestPacket packet = new TestPacket();

  @Test
  void convertsJsonNumberListToIntArray() {
    Map<String, Object> fields = Map.of("voltages", List.of(12001.0, 11998.0, 65535.0));

    assertArrayEquals(
        new int[] {12001, 11998, 65535}, packet.readIntArray(fields, "voltages"));
  }

  @Test
  void rejectsListsContainingNonNumericValues() {
    Map<String, Object> fields = Map.of("voltages", List.of(12001.0, "invalid"));

    assertArrayEquals(new int[0], packet.readIntArray(fields, "voltages"));
  }

  private static final class TestPacket extends MavlinkPacket {

    private int[] readIntArray(Map<String, Object> fields, String key) {
      return getIntArray(fields, key);
    }
  }
}
