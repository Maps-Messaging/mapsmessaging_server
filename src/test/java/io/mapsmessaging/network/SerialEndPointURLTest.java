package io.mapsmessaging.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerialEndPointURLTest {

  @Test
  void serialUrlRetainsProtocolButHasNoNetworkHostOrPort() {
    SerialEndPointURL url = new SerialEndPointURL("serial:///dev/ttyUSB0");

    assertEquals("serial", url.getProtocol());
    assertEquals("", url.getHost());
    assertEquals("", url.getFile());
    assertEquals(0, url.getPort());
    assertEquals("serial_", url.getJMXName());
  }

  @Test
  void protocolParsingIsCasePreservingAccordingToEndpointUrlContract() {
    SerialEndPointURL url = new SerialEndPointURL("serial://COM3");

    assertEquals("serial", url.getProtocol());
    assertEquals(0, url.getPort());
  }
}
