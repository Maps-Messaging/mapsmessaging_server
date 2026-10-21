package io.mapsmessaging.hardware.device.handler.onewire;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OneWireDeviceHandlerTest {

  @Test
  void deviceUsesOneWireBusIdentityAndInheritedDeviceMetadata() {
    DeviceController controller = mock(DeviceController.class);
    when(controller.getName()).thenReturn("temperature");
    when(controller.getType()).thenReturn(DeviceType.SENSOR);

    OneWireDeviceHandler handler =
        new OneWireDeviceHandler("28-0001", controller);

    assertEquals("oneWire", handler.getBusName());
    assertEquals("temperature", handler.getName());
    assertEquals(-1, handler.getBusNumber());
    assertEquals(-1, handler.getDeviceAddress());
    assertEquals(
        "/device/sensor/oneWire/temperature",
        handler.getTopicName("/device/[device_type]/[bus_name]/[device_name]")
    );
  }
}
