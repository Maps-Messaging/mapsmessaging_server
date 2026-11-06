package io.mapsmessaging.hardware.device.handler.i2c;

import io.mapsmessaging.config.device.I2CBusConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.i2c.I2CBusManager;
import io.mapsmessaging.dto.rest.config.device.I2CDeviceConfigDTO;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class I2CBusHandlerTest {

  @Test
  void configuredDevicesAreRegisteredAndSelectorsOverrideBusDefault() throws Exception {
    I2CBusManager manager = mock(I2CBusManager.class);
    I2CBusConfig config = config();
    I2CDeviceConfigDTO device = new I2CDeviceConfigDTO();
    device.setAddress(42);
    device.setName("sensor");
    device.setSelector("temperature > 20");
    config.setDevices(List.of(device));

    TestHandler handler = new TestHandler(manager, config, mock(Trigger.class));

    verify(manager).configureDevice(42, "sensor");
    assertEquals("temperature > 20", handler.selector(42));
    assertEquals("fallback", handler.selector(7));
  }

  @Test
  void scanRequestsAutoDiscoveryAndReturnsActiveControllers() throws Exception {
    I2CBusManager manager = mock(I2CBusManager.class);
    Map<String, DeviceController> active = new HashMap<>();
    active.put("42", mock(DeviceController.class));
    when(manager.getActive()).thenReturn(active);

    I2CBusConfig config = config();
    config.setAutoScan(true);
    TestHandler handler = new TestHandler(manager, config, mock(Trigger.class));

    assertSame(active, handler.scanDevices());
    verify(manager).scanForDevices(10);
  }

  @Test
  void controllerIsWrappedInI2cDeviceHandler() {
    TestHandler handler =
        new TestHandler(mock(I2CBusManager.class), config(), mock(Trigger.class));
    DeviceController controller = mock(DeviceController.class);

    DeviceHandler result = handler.create("key", controller);

    assertInstanceOf(I2CDeviceHandler.class, result);
    assertSame(controller, result.getController());
  }

  private static I2CBusConfig config() {
    I2CBusConfig config = new I2CBusConfig(new ConfigurationProperties());
    config.setSelector("fallback");
    config.setDevices(List.of());
    return config;
  }

  private static final class TestHandler extends I2CBusHandler {
    TestHandler(I2CBusManager manager, I2CBusConfig config, Trigger trigger) {
      super(manager, config, trigger);
    }

    String selector(int address) {
      return getSelector(address);
    }

    Map<String, DeviceController> scanDevices() {
      return scan();
    }

    DeviceHandler create(String key, DeviceController controller) {
      return createDeviceHander(key, controller);
    }
  }
}
