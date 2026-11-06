package io.mapsmessaging.hardware.device.handler.spi;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpiDeviceHandlerTest {

  @Test
  void spiHandlerPublishesStableBusCoordinatesAndTopicIdentity() {
    DeviceController controller = mock(DeviceController.class);
    when(controller.getName()).thenReturn("adc");
    when(controller.getType()).thenReturn(DeviceType.SENSOR);

    SpiDeviceHandler handler = new SpiDeviceHandler("spi-adc", controller);

    assertEquals("spi", handler.getBusName());
    assertEquals(0, handler.getBusNumber());
    assertEquals(0, handler.getDeviceAddress());
    assertEquals(
        "/device/sensor/spi/0/0x0/adc",
        handler.getTopicName(
            "/device/[device_type]/[bus_name]/[bus_number]/[device_addr]/[device_name]"
        )
    );
  }
}
