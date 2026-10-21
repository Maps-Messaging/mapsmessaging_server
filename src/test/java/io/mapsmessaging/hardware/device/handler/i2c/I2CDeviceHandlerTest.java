package io.mapsmessaging.hardware.device.handler.i2c;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class I2CDeviceHandlerTest {

  @Test
  void genericControllerUsesI2cIdentityAndFallbackBusCoordinates() {
    DeviceController controller = mock(DeviceController.class);
    when(controller.getName()).thenReturn("sensor");
    when(controller.getType()).thenReturn(DeviceType.SENSOR);

    I2CDeviceHandler handler = new I2CDeviceHandler("key", controller);

    assertEquals("i2c", handler.getBusName());
    assertEquals(-1, handler.getBusNumber());
    assertEquals(-1, handler.getDeviceAddress());
    assertTrue(handler.enableConfig());
    assertTrue(handler.enableRaw());
    assertEquals("/device/sensor/i2c/sensor",
        handler.getTopicName("/device/[device_type]/[bus_name]/[device_name]"));
  }
}
