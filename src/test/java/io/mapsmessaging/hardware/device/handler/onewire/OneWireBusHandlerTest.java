package io.mapsmessaging.hardware.device.handler.onewire;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.onewire.OneWireBusManager;
import io.mapsmessaging.dto.rest.config.device.OneWireBusConfigDTO;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OneWireBusHandlerTest {

  @Test
  void scanDelegatesToManagerAndReturnsActiveDevices() {
    OneWireBusManager manager = mock(OneWireBusManager.class);
    DeviceController controller = mock(DeviceController.class);
    Map<String, DeviceController> active = Map.of("28-0001", controller);
    when(manager.getActive()).thenReturn(active);

    TestHandler handler =
        new TestHandler(manager, config(), mock(Trigger.class));

    assertSame(active, handler.scanDevices());
    verify(manager).scan();
  }

  @Test
  void discoveredControllerIsWrappedAsOneWireDeviceHandler() {
    TestHandler handler =
        new TestHandler(mock(OneWireBusManager.class), config(), mock(Trigger.class));
    DeviceController controller = mock(DeviceController.class);

    DeviceHandler deviceHandler = handler.create("28-0001", controller);

    assertInstanceOf(OneWireDeviceHandler.class, deviceHandler);
    assertSame(controller, deviceHandler.getController());
    assertEquals("28-0001", deviceHandler.getKey());
  }

  private static OneWireBusConfigDTO config() {
    OneWireBusConfigDTO config = new OneWireBusConfigDTO();
    config.setScanTime(1000);
    config.setTopicNameTemplate("/device/[device_name]");
    return config;
  }

  private static final class TestHandler extends OneWireBusHandler {
    TestHandler(OneWireBusManager manager, OneWireBusConfigDTO config, Trigger trigger) {
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
