package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SerialDeviceConfigBranchCoverageTest {

  @Test
  void serializationOmitsBlankSelectorAndIncludesSerialNumberWhenPresent() {
    ConfigurationProperties properties = base();
    ((ConfigurationProperties) properties.get("serial")).put("serialNo", "ABC123");
    SerialDeviceConfig config = new SerialDeviceConfig(properties);

    ConfigurationProperties packed = config.toConfigurationProperties();
    ConfigurationProperties serial = (ConfigurationProperties) packed.get("serial");

    assertFalse(packed.containsKey("selector"));
    assertEquals("ABC123", serial.getProperty("serialNo"));
  }

  @Test
  void nonSerialDtoIsRejected() {
    assertFalse(new SerialDeviceConfig(base()).update(new BaseConfigDTO()));
  }

  @Test
  void inheritedMapUpdateReportsChangedAndUnchangedValues() {
    SerialDeviceConfig config = new SerialDeviceConfig(base());
    Map<String, Object> current = new LinkedHashMap<>();
    current.put("a", 1);

    assertFalse(config.updateMap(current, Map.of("a", 1)));
    assertTrue(config.updateMap(current, Map.of("a", 2, "b", 3)));
    assertEquals(Map.of("a", 2, "b", 3), current);
  }

  private static ConfigurationProperties base() {
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
    root.put("readTimeOut", 100);
    root.put("writeTimeOut", 200);
    root.put("bufferSize", 1024);
    return root;
  }
}