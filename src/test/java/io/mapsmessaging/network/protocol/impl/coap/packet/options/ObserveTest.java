package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObserveTest {

  @Test
  void zeroRepresentsRegistration() {
    Observe observe = new Observe();

    assertEquals(Constants.OBSERVE, observe.getId());
    assertTrue(observe.register());
    assertEquals("Observer( Register:true)", observe.toString());
    assertArrayEquals(new byte[0], observe.pack());
  }

  @Test
  void nonZeroValueRepresentsNotificationSequence() {
    Observe observe = new Observe(42);

    assertFalse(observe.register());
    assertArrayEquals(new byte[]{42}, observe.pack());
    assertEquals("Observer( Register:false)", observe.toString());
  }

  @Test
  void updateCanSwitchBetweenRegisterAndSequenceStates() throws Exception {
    Observe observe = new Observe(1);

    observe.update(new byte[0]);
    assertTrue(observe.register());

    observe.update(new byte[]{5});
    assertFalse(observe.register());
    assertEquals(5L, observe.getValue());
  }
}
