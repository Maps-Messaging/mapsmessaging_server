package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.mavlink.packet.BatteryStatusPacket;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BatteryStatusListenerBranchCoverageTest {

  @Test
  void voltageCalculationReturnsNaNForNoCellsAndSumsKnownCells() throws Exception {
    BatteryStatusListener listener = new BatteryStatusListener(mock(TwinManager.class));
    Method method = BatteryStatusListener.class.getDeclaredMethod(
        "calculateVoltageVolts", BatteryStatusPacket.class);
    method.setAccessible(true);

    BatteryStatusPacket empty = mock(BatteryStatusPacket.class);
    when(empty.getKnownVoltages()).thenReturn(new int[0]);
    assertTrue(Double.isNaN((double) method.invoke(listener, empty)));

    BatteryStatusPacket cells = mock(BatteryStatusPacket.class);
    when(cells.getKnownVoltages()).thenReturn(new int[]{12000, 12100});
    assertEquals(24.1, (double) method.invoke(listener, cells), 0.0001);
  }

  @Test
  void chargingRequiresPresentChargeStateAndAcceptsChargingStatesTwoAndThree() throws Exception {
    BatteryStatusListener listener = new BatteryStatusListener(mock(TwinManager.class));
    Method method = BatteryStatusListener.class.getDeclaredMethod(
        "isCharging", BatteryStatusPacket.class);
    method.setAccessible(true);

    BatteryStatusPacket packet = mock(BatteryStatusPacket.class);
    when(packet.isChargeStatePresent()).thenReturn(false);
    assertEquals(false, method.invoke(listener, packet));

    when(packet.isChargeStatePresent()).thenReturn(true);
    when(packet.getChargeState()).thenReturn(2);
    assertEquals(true, method.invoke(listener, packet));
    when(packet.getChargeState()).thenReturn(3);
    assertEquals(true, method.invoke(listener, packet));
    when(packet.getChargeState()).thenReturn(1);
    assertEquals(false, method.invoke(listener, packet));
  }

  @Test
  void nonBatteryAndInvalidBatteryPacketsAreIgnored() {
    TwinManager manager = mock(TwinManager.class);
    BatteryStatusListener listener = new BatteryStatusListener(manager);

    listener.handle("twin", mock(io.mapsmessaging.state.mavlink.packet.MavlinkPacket.class), null);

    BatteryStatusPacket invalid = mock(BatteryStatusPacket.class);
    when(invalid.isValid()).thenReturn(false);
    listener.handle("twin", invalid, null);

    verifyNoInteractions(manager);
  }
}