package io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ForwardStatusTest {

  @Test
  void onlyAcknowledgedStateCountsAsDelivered() {
    ForwardStatus status = new ForwardStatus();

    for (ForwardStatus.MessageState state : ForwardStatus.MessageState.values()) {
      status.setState(state);
      assertEquals(
          state == ForwardStatus.MessageState.ACKNOWLEDGED,
          status.isDelivered(),
          "Unexpected delivery classification for " + state
      );
    }
  }

  @Test
  void missingStateIsNotDelivered() {
    ForwardStatus status = new ForwardStatus();
    status.setState(null);

    assertFalse(status.isDelivered());
  }
}
