package io.mapsmessaging.network.protocol.impl.mqtt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketIdentifierExhaustedExceptionTest {

  @Test
  void exceptionCarriesStableDiagnosticMessageAndIllegalStateType() {
    PacketIdentifierExhaustedException exception = new PacketIdentifierExhaustedException();

    assertInstanceOf(IllegalStateException.class, exception);
    assertEquals("No MQTT packet identifiers are available", exception.getMessage());
  }
}