package io.mapsmessaging.hardware.device;

import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceClientConnectionTest {

  @Test
  void clientIdentityIsDerivedFromDeviceHandler() {
    DeviceHandler handler = mock(DeviceHandler.class);
    when(handler.getBusName()).thenReturn("i2c");
    when(handler.getVersion()).thenReturn("1.0");
    when(handler.getName()).thenReturn("temperature");

    DeviceClientConnection connection = new DeviceClientConnection(handler);

    assertSame(handler, connection.getDeviceHandler());
    assertEquals("i2c", connection.getName());
    assertEquals("1.0", connection.getVersion());
    assertEquals("temperature", connection.getUniqueName());
    assertEquals("temperature", connection.getPrincipal().getName());
    assertEquals(0L, connection.getTimeOut());
    assertEquals("", connection.getAuthenticationConfig());
    assertEquals("", connection.getProtocolName());
    assertEquals("", connection.getRemoteIp());
    assertDoesNotThrow(connection::sendKeepAlive);
  }
}
