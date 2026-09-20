package io.mapsmessaging.api.features;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetainHandlerCoverageSweepTest {
  @Test
  void handlerIdsMapToRetainPolicies() {
    assertEquals(RetainHandler.SEND_ALWAYS, RetainHandler.getInstance(0));
    assertEquals(RetainHandler.SEND_IF_NEW, RetainHandler.getInstance(1));
    assertEquals(RetainHandler.DO_NOT_SEND, RetainHandler.getInstance(2));
    assertThrows(IllegalArgumentException.class, () -> RetainHandler.getInstance(-1));
  }
}
