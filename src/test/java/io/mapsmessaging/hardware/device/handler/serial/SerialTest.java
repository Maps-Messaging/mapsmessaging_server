package io.mapsmessaging.hardware.device.handler.serial;

import com.fazecast.jSerialComm.SerialPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SerialTest {

  @Test
  void allSerialOperationsDelegateToUnderlyingPort() {
    SerialPort port = mock(SerialPort.class);
    when(port.isOpen()).thenReturn(true);
    when(port.openPort()).thenReturn(true);
    when(port.getSystemPortName()).thenReturn("ttyUSB0");
    when(port.writeBytes(any(byte[].class), eq(3L))).thenReturn(3);
    when(port.readBytes(any(byte[].class), eq(4L))).thenReturn(4);

    Serial serial = new Serial(port);

    assertTrue(serial.isOpen());
    assertTrue(serial.openPort());
    assertEquals("ttyUSB0", serial.getSystemPortName());

    byte[] request = new byte[]{1, 2, 3};
    byte[] response = new byte[4];
    assertEquals(3, serial.writeBytes(request, 3));
    assertEquals(4, serial.readBytes(response, 4));

    serial.closePort();
    verify(port).closePort();
  }
}
