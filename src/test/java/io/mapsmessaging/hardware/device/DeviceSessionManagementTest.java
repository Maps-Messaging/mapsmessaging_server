package io.mapsmessaging.hardware.device;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.hardware.device.filter.DataFilter;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceSessionManagementTest {

  @Test
  void runPublishesDevicePayloadWithDeviceMetadata() throws Exception {
    DeviceController controller = mock(DeviceController.class);
    DeviceHandler device = mock(DeviceHandler.class);
    BusHandler bus = mock(BusHandler.class);
    Destination destination = mock(Destination.class);
    Session session = mock(Session.class);
    UUID schema = UUID.randomUUID();

    when(device.getSchemaId()).thenReturn(schema);
    when(device.getController()).thenReturn(controller);
    when(device.getName()).thenReturn("temperature");
    when(device.getBusName()).thenReturn("i2c");
    when(device.getBusNumber()).thenReturn(1);
    when(device.getVersion()).thenReturn("1.0");
    when(device.getData()).thenReturn("{\"value\":21}".getBytes());
    when(session.getName()).thenReturn("device-session");

    DeviceSessionManagement management =
        new DeviceSessionManagement(device, "/device/[device_name]", DataFilter.ALWAYS_SEND, bus, null);
    management.setSession(session);
    management.setDestination(destination);

    management.run();

    ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
    verify(destination).storeMessage(sent.capture());
    assertEquals("i2c", sent.getValue().getMeta().get("busName"));
    assertEquals("1", sent.getValue().getMeta().get("busNumber"));
    assertEquals("temperature", sent.getValue().getMeta().get("device"));
    assertArrayEquals("{\"value\":21}".getBytes(), sent.getValue().getOpaqueData());
    verify(controller).setRaiseExceptionOnError(true);
  }

  @Test
  void sendMessageAlwaysCompletesEvenWhenDeviceUpdateFails() throws Exception {
    DeviceController controller = mock(DeviceController.class);
    DeviceHandler device = mock(DeviceHandler.class);
    when(device.getSchemaId()).thenReturn(UUID.randomUUID());
    when(device.getController()).thenReturn(controller);
    when(device.updateConfig(any())).thenThrow(new java.io.IOException("bad config"));

    DeviceSessionManagement management =
        new DeviceSessionManagement(
            device, "/device/[device_name]", DataFilter.ALWAYS_SEND, mock(BusHandler.class), null);

    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);
    when(message.getOpaqueData()).thenReturn(new byte[]{1});

    assertDoesNotThrow(() -> management.sendMessage(event));
    verify(completion).run();
  }
}
