package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.device.SerialBusDeviceDTO;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerialDeviceConfigTest {

  @Test
  void nestedSerialConfigurationRoundTrips() {
    SerialDeviceConfig source = new SerialDeviceConfig(properties());

    ConfigurationProperties packed = source.toConfigurationProperties();
    SerialDeviceConfig restored = new SerialDeviceConfig(packed);

    assertEquals(source.getName(), restored.getName());
    assertEquals(source.getSelector(), restored.getSelector());
    assertEquals(source.getSerialConfig(), restored.getSerialConfig());
    assertEquals(source.getReadTimeOut(), restored.getReadTimeOut());
    assertEquals(source.getWriteTimeOut(), restored.getWriteTimeOut());
  }

  @Test
  void updateAppliesSerialIdentityAndTimeoutChanges() {
    SerialDeviceConfig config = new SerialDeviceConfig(properties());

    SerialDeviceDTO serial = new SerialDeviceDTO();
    serial.setPort("/dev/ttyUSB9");
    serial.setBaudRate(115200);
    serial.setDataBits(8);
    serial.setStopBits(1);
    serial.setParity("n");
    serial.setFlowControl(0);
    serial.setBufferSize(8192);

    SerialBusDeviceDTO update = new SerialBusDeviceDTO();
    update.setName("updated");
    update.setSelector("x = 1");
    update.setSerialConfig(serial);
    update.setReadTimeOut(1234);
    update.setWriteTimeOut(5678);

    assertTrue(config.update(update));
    assertEquals("updated", config.getName());
    assertEquals(1234, config.getReadTimeOut());
    assertEquals(5678, config.getWriteTimeOut());
    assertEquals(1234, config.getSerialConfig().getReadTimeOut());
    assertEquals(5678, config.getSerialConfig().getWriteTimeOut());
    assertFalse(config.update(update));
  }

  private static ConfigurationProperties properties() {
    ConfigurationProperties serial = new ConfigurationProperties();
    serial.put("port", "/dev/ttyUSB0");
    serial.put("baudRate", 9600);
    serial.put("dataBits", 8);
    serial.put("stopBits", 1.0f);
    serial.put("parity", "n");
    serial.put("flowControl", 0);

    ConfigurationProperties root = new ConfigurationProperties();
    root.put("name", "gps");
    root.put("selector", "");
    root.put("serial", serial);
    root.put("readTimeOut", 3000);
    root.put("writeTimeOut", 4000);
    root.put("bufferSize", 4096);
    return root;
  }
}
