package io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationModeTest {

  @Test
  void everyDefinedCodeRoundTrips() {
    for (OperationMode mode : OperationMode.values()) {
      assertSame(mode, OperationMode.fromCode(mode.getCode()));
    }
  }

  @Test
  void nullAndUnknownCodesAreNotInvented() {
    assertNull(OperationMode.fromCode(null));
    assertNull(OperationMode.fromCode(-1));
    assertNull(OperationMode.fromCode(99));
  }
}
