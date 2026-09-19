package io.mapsmessaging.state.drone.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SyntheticMmsiGeneratorTest {

  @Test
  void generationIsStableTrimmedAndWithinSyntheticNineDigitRange() {
    long first = SyntheticMmsiGenerator.generateSyntheticMmsi("drone-001");
    long again = SyntheticMmsiGenerator.generateSyntheticMmsi("drone-001");
    long trimmed = SyntheticMmsiGenerator.generateSyntheticMmsi("  drone-001  ");

    assertEquals(first, again);
    assertEquals(first, trimmed);
    assertTrue(first >= 980_000_001L);
    assertTrue(first <= 989_999_999L);
  }

  @Test
  void differentIdsNormallyProduceDifferentSyntheticIdentities() {
    assertNotEquals(
        SyntheticMmsiGenerator.generateSyntheticMmsi("drone-alpha"),
        SyntheticMmsiGenerator.generateSyntheticMmsi("drone-bravo")
    );
  }

  @Test
  void nullAndBlankIdsAreRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SyntheticMmsiGenerator.generateSyntheticMmsi(null)
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> SyntheticMmsiGenerator.generateSyntheticMmsi("")
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> SyntheticMmsiGenerator.generateSyntheticMmsi("   ")
    );
  }

  @Test
  void formattingAcceptsOnlyNineDigitNumbers() {
    assertEquals("980000001", SyntheticMmsiGenerator.formatMmsi(980_000_001L));
    assertEquals("999999999", SyntheticMmsiGenerator.formatMmsi(999_999_999L));

    assertThrows(
        IllegalArgumentException.class,
        () -> SyntheticMmsiGenerator.formatMmsi(99_999_999L)
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> SyntheticMmsiGenerator.formatMmsi(1_000_000_000L)
    );
  }
}
