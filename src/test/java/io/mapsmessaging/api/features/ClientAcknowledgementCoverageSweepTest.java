package io.mapsmessaging.api.features;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientAcknowledgementCoverageSweepTest {
  @Test
  void numericValuesMapToModesAndInvalidValuesAreRejected() {
    assertEquals(ClientAcknowledgement.AUTO, ClientAcknowledgement.getInstance(0));
    assertEquals(ClientAcknowledgement.INDIVIDUAL, ClientAcknowledgement.getInstance(1));
    assertEquals(ClientAcknowledgement.BLOCK, ClientAcknowledgement.getInstance(2));
    assertThrows(IllegalArgumentException.class, () -> ClientAcknowledgement.getInstance(3));
  }
}
