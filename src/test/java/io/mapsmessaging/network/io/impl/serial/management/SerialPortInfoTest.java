package io.mapsmessaging.network.io.impl.serial.management;

import org.junit.jupiter.api.Test;

import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class SerialPortInfoTest {

  @Test
  void equalityAndHashingAreCaseInsensitive() {
    SerialPortInfo first = new SerialPortInfo("/dev/ttyUSB0", "ABC123");
    SerialPortInfo same = new SerialPortInfo("/DEV/TTYUSB0", "abc123");

    assertEquals(first, same);
    assertEquals(first.hashCode(), same.hashCode());
  }

  @Test
  void orderingUsesSerialNumberThenPortNameCaseInsensitively() {
    SerialPortInfo b = new SerialPortInfo("ttyB", "SERIAL1");
    SerialPortInfo a = new SerialPortInfo("ttyA", "serial1");
    SerialPortInfo later = new SerialPortInfo("tty0", "serial2");

    TreeSet<SerialPortInfo> set = new TreeSet<>();
    set.add(later);
    set.add(b);
    set.add(a);

    assertEquals(a, set.first());
    assertEquals(later, set.last());
    assertTrue(a.compareTo(b) < 0);
  }

  @Test
  void equalityRejectsNullAndOtherTypes() {
    SerialPortInfo info = new SerialPortInfo("ttyA", "serial");

    assertNotEquals(info, null);
    assertNotEquals(info, "ttyA");
  }
}
