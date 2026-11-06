/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.mavlink.messages;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkDurationTest {

  @ParameterizedTest
  @MethodSource("durationCases")
  void convertsDurationToMavlinkSeconds(Duration duration, float expected) {
    assertEquals(expected, MavlinkDuration.toSeconds(duration, "duration"), 0.000001f);
  }

  @Test
  void nullDurationReturnsZero() {
    assertEquals(0.0f, MavlinkDuration.toSeconds(null, "duration"));
  }

  @Test
  void negativeDurationIsRejected() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> MavlinkDuration.toSeconds(Duration.ofMillis(-1), "hold"));

    assertTrue(exception.getMessage().contains("hold"));
  }

  @Test
  void nullNameIsRejectedBeforeDurationHandling() {
    assertThrows(NullPointerException.class, () -> MavlinkDuration.toSeconds(null, null));
    assertThrows(NullPointerException.class,
        () -> MavlinkDuration.toSeconds(Duration.ofSeconds(1), null));
  }

  private static Stream<Arguments> durationCases() {
    return Stream.of(
        Arguments.of(Duration.ZERO, 0.0f),
        Arguments.of(Duration.ofNanos(1), 0.000000001f),
        Arguments.of(Duration.ofMillis(1), 0.001f),
        Arguments.of(Duration.ofMillis(250), 0.25f),
        Arguments.of(Duration.ofMillis(500), 0.5f),
        Arguments.of(Duration.ofSeconds(1), 1.0f),
        Arguments.of(Duration.ofMillis(1500), 1.5f),
        Arguments.of(Duration.ofMillis(10_125), 10.125f),
        Arguments.of(Duration.ofMinutes(1), 60.0f),
        Arguments.of(Duration.ofHours(1), 3600.0f),
        Arguments.of(Duration.ofDays(1), 86400.0f)
    );
  }
}
