package io.mapsmessaging.network.io.impl.canbus;

import io.mapsmessaging.canbus.device.CanCapabilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CanbusInterfaceInfoTest {

  @Test
  void mtuComesFromCanCapabilitiesAndNetworkTraitsAreNonIp() throws Exception {
    CanCapabilities capabilities = mock(CanCapabilities.class);
    when(capabilities.interfaceMaxPayloadBytes()).thenReturn(64);

    CanbusInterfaceInfo info = new CanbusInterfaceInfo(capabilities);

    assertEquals(64, info.getMTU());
    assertFalse(info.isLoopback());
    assertFalse(info.isUp());
    assertFalse(info.isLoRa());
    assertFalse(info.isIPV4());
    assertNull(info.getBroadcast());
    assertTrue(info.getInterfaces().isEmpty());
  }
}
