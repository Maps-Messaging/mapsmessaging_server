package io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkStatusTest {

  @Test
  void ats54StatesAreClassifiedAccordingToTransmitCapability() {
    assertTrue(NetworkStatus.parse("5").canSend());
    assertNull(NetworkStatus.parse("5").noSendReason());

    assertEquals("unknown", NetworkStatus.parse("0").noSendReason());
    assertEquals("stopped/failed", NetworkStatus.parse("1").noSendReason());
    assertEquals("searching", NetworkStatus.parse("2").noSendReason());
    assertEquals("receive only", NetworkStatus.parse("3").noSendReason());
    assertEquals("receive only", NetworkStatus.parse("4").noSendReason());
    assertEquals("TX suspended (network)", NetworkStatus.parse("6").noSendReason());
    assertEquals("TX muted (user)", NetworkStatus.parse("7").noSendReason());
    assertEquals("TX blocked (no beam)", NetworkStatus.parse("8").noSendReason());
    assertEquals("invalid state", NetworkStatus.parse("garbage").noSendReason());
  }

  @Test
  void ogxConnectedStateRequiresRegistrationNetworkAndTxPermission() {
    NetworkStatus allowed = NetworkStatus.parse("%NETINFO: 2,5,0,0,0");
    assertTrue(allowed.canSend());

    assertEquals("not registered", NetworkStatus.parse("1,5,0,0,0").noSendReason());
    assertEquals("network offline", NetworkStatus.parse("2,0,0,0,0").noSendReason());
    assertEquals("acquiring", NetworkStatus.parse("2,2,0,0,0").noSendReason());
    assertEquals("downloading config", NetworkStatus.parse("2,3,0,0,0").noSendReason());
    assertEquals("registered (RX only)", NetworkStatus.parse("2,4,0,0,0").noSendReason());
  }

  @Test
  void ogxTransmitGatesExposeSpecificReason() {
    assertEquals("network mute", NetworkStatus.parse("2,5,1,0,0").noSendReason());
    assertEquals("user mute", NetworkStatus.parse("2,5,2,0,0").noSendReason());
    assertEquals("GNSS position stale", NetworkStatus.parse("2,5,3,0,0").noSendReason());
    assertEquals("satellite motion model", NetworkStatus.parse("2,5,4,0,0").noSendReason());
    assertEquals("doppler velocity stale", NetworkStatus.parse("2,5,5,0,0").noSendReason());
    assertEquals("unknown", NetworkStatus.parse("2,5,99,0,0").noSendReason());
  }

  @Test
  void emulatorErrorIsTreatedAsSendCapable() {
    NetworkStatus status = NetworkStatus.parse("ERROR");

    assertTrue(status.canSend());
    assertEquals("Modem Emulator", status.noSendReason());
  }
}
