package io.mapsmessaging.network.io.impl.serial.management;

import com.fazecast.jSerialComm.SerialPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SerialPortRegistryTest {

  @Test
  void addAndLookupAreCaseInsensitiveByNameAndSerial() {
    SerialPortRegistry registry = new SerialPortRegistry();
    SerialPort first = mock(SerialPort.class);
    SerialPort second = mock(SerialPort.class);

    registry.add(new SerialPortInfo("ttyB", "SERIAL2"), second);
    registry.add(new SerialPortInfo("ttyA", "SERIAL1"), first);

    assertSame(first, registry.getByName("TTYA"));
    assertSame(second, registry.getBySerial("serial2"));
    assertEquals(2, registry.size());
  }

  @Test
  void listIsSortedBySerialThenPortName() {
    SerialPortRegistry registry = new SerialPortRegistry();
    registry.add(new SerialPortInfo("ttyB", "SERIAL1"), mock(SerialPort.class));
    registry.add(new SerialPortInfo("ttyA", "serial1"), mock(SerialPort.class));
    registry.add(new SerialPortInfo("tty0", "SERIAL2"), mock(SerialPort.class));

    List<SerialPortInfo> infos = registry.listInfos();

    assertEquals("ttyA", infos.get(0).getName());
    assertEquals("ttyB", infos.get(1).getName());
    assertEquals("tty0", infos.get(2).getName());
  }

  @Test
  void removeReturnsPortAndUnknownLookupsReturnNull() {
    SerialPortRegistry registry = new SerialPortRegistry();
    SerialPort port = mock(SerialPort.class);
    registry.add(new SerialPortInfo("ttyA", "SERIAL1"), port);

    assertSame(port, registry.remove("TTYA"));
    assertEquals(0, registry.size());
    assertNull(registry.remove("missing"));
    assertNull(registry.getByName("missing"));
    assertNull(registry.getBySerial("missing"));
  }
}
