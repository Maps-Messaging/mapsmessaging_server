package io.mapsmessaging.hardware.device.handler.serial;

import io.mapsmessaging.config.device.SerialDeviceBusConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.serial.SerialBusManager;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SerialDeviceBusHandlerTest {

  @Test
  void emptyConfigurationScansWithoutTouchingHardware() {
    SerialDeviceBusConfig config =
        new SerialDeviceBusConfig(new ConfigurationProperties());
    config.setDevices(List.of());

    TestHandler handler =
        new TestHandler(mock(SerialBusManager.class), config, mock(Trigger.class));

    assertTrue(handler.scanDevices().isEmpty());
  }

  @Test
  void deviceControllerIsWrappedAsSerialDeviceHandler() {
    SerialDeviceBusConfig config =
        new SerialDeviceBusConfig(new ConfigurationProperties());
    config.setDevices(List.of());
    TestHandler handler =
        new TestHandler(mock(SerialBusManager.class), config, mock(Trigger.class));
    DeviceController controller = mock(DeviceController.class);

    DeviceHandler result = handler.create("ttyUSB0", controller);

    assertInstanceOf(SerialDeviceHandler.class, result);
    assertSame(controller, result.getController());
  }

  private static final class TestHandler extends SerialDeviceBusHandler {
    TestHandler(SerialBusManager manager, SerialDeviceBusConfig config, Trigger trigger) {
      super(manager, config, trigger);
    }

    Map<String, DeviceController> scanDevices() {
      return scan();
    }

    DeviceHandler create(String key, DeviceController controller) {
      return createDeviceHander(key, controller);
    }
  }
}
