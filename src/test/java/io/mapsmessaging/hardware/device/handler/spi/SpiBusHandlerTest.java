package io.mapsmessaging.hardware.device.handler.spi;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.spi.SpiBusManager;
import io.mapsmessaging.devices.spi.SpiDeviceController;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceConfigDTO;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpiBusHandlerTest {

  @Test
  void configuredMissingDeviceIsMountedIntoActiveMap() throws Exception {
    SpiBusManager manager = mock(SpiBusManager.class);
    Map<String, DeviceController> active = new HashMap<>();
    when(manager.getActive()).thenReturn(active);
    SpiDeviceController controller = mock(SpiDeviceController.class);
    when(manager.configureDevice(eq("adc"), any())).thenReturn(controller);

    SpiDeviceConfigDTO device = mock(SpiDeviceConfigDTO.class);
    when(device.getName()).thenReturn("adc");
    when(device.getConfig()).thenReturn(Map.of());

    SpiDeviceBusConfigDTO config = new SpiDeviceBusConfigDTO();
    config.setDevices(List.of(device));

    TestHandler handler = new TestHandler(manager, config, mock(Trigger.class));

    assertSame(active, handler.scanDevices());
    assertSame(controller, active.get("adc"));
  }

  @Test
  void existingDeviceIsNotConfiguredAgain() throws Exception {
    SpiBusManager manager = mock(SpiBusManager.class);
    Map<String, DeviceController> active = new HashMap<>();
    active.put("adc", mock(DeviceController.class));
    when(manager.getActive()).thenReturn(active);

    SpiDeviceConfigDTO device = mock(SpiDeviceConfigDTO.class);
    when(device.getName()).thenReturn("adc");
    SpiDeviceBusConfigDTO config = new SpiDeviceBusConfigDTO();
    config.setDevices(List.of(device));

    new TestHandler(manager, config, mock(Trigger.class)).scanDevices();

    verify(manager, never()).configureDevice(anyString(), any());
  }

  @Test
  void controllerIsWrappedAsSpiDeviceHandler() {
    SpiDeviceBusConfigDTO config = new SpiDeviceBusConfigDTO();
    config.setDevices(List.of());
    TestHandler handler =
        new TestHandler(mock(SpiBusManager.class), config, mock(Trigger.class));
    DeviceController controller = mock(DeviceController.class);

    DeviceHandler result = handler.create("adc", controller);

    assertInstanceOf(SpiDeviceHandler.class, result);
  }

  private static final class TestHandler extends SpiBusHandler {
    TestHandler(SpiBusManager manager, SpiDeviceBusConfigDTO config, Trigger trigger) {
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
