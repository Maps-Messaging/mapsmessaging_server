package io.mapsmessaging.network.protocol.impl.satellite.gateway.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceIdUtilTest {

  @Test
  void acceptsSupportedFifteenAndSixteenCharacterDeviceIds() {
    assertTrue(DeviceIdUtil.isValidDeviceId("12345678SKY1a2B"));
    assertTrue(DeviceIdUtil.isValidDeviceId("123456789QCSabcd"));
    assertTrue(DeviceIdUtil.isValidDeviceId("123456789UBXABCD"));
  }

  @Test
  void rejectsNullWrongLengthAndNonNumericMtid() {
    assertFalse(DeviceIdUtil.isValidDeviceId(null));
    assertFalse(DeviceIdUtil.isValidDeviceId(""));
    assertFalse(DeviceIdUtil.isValidDeviceId("1234567SKYabcd"));
    assertFalse(DeviceIdUtil.isValidDeviceId("1234A678SKYabcd"));
  }

  @Test
  void rejectsUnknownManufacturerAndNonHexChecksum() {
    assertFalse(DeviceIdUtil.isValidDeviceId("12345678ABCabcd"));
    assertFalse(DeviceIdUtil.isValidDeviceId("12345678SKYxyz!"));
    assertFalse(DeviceIdUtil.isValidDeviceId("12345678skyabcd"));
  }
}
