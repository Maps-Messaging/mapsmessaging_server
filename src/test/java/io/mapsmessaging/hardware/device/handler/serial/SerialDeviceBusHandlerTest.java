package io.mapsmessaging.hardware.device.handler.serial;

import io.mapsmessaging.config.device.SerialDeviceBusConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.serial.ComputedSerialDeviceController;
import io.mapsmessaging.devices.serial.SerialBusManager;
import io.mapsmessaging.devices.serial.SerialDeviceController;
import io.mapsmessaging.hardware.device.DeviceSessionManagement;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SerialDeviceBusHandlerTest {

  @Test
  void emptyConfigurationScansWithoutTouchingHardware() {
    TestHandler handler = new TestHandler(mock(SerialBusManager.class), emptyConfig(), mock(Trigger.class));
    assertTrue(handler.scanDevices().isEmpty());
  }

  @Test
  void deviceControllerIsWrappedAsSerialDeviceHandler() {
    TestHandler handler = new TestHandler(mock(SerialBusManager.class), emptyConfig(), mock(Trigger.class));
    DeviceController controller = mock(DeviceController.class);
    DeviceHandler result = handler.create("ttyUSB0", controller);
    assertInstanceOf(SerialDeviceHandler.class, result);
    assertSame(controller, result.getController());
  }

  @Test
  void onlyComputedControllersEnableSerialConfiguration() {
    SerialDeviceHandler physical = new SerialDeviceHandler("COM3", mock(SerialDeviceController.class));
    SerialDeviceHandler computed = new SerialDeviceHandler("COM3_change", mock(ComputedSerialDeviceController.class));
    assertFalse(physical.enableConfig());
    assertTrue(computed.enableConfig());
  }

  @Test
  void sessionNamesIdentifyPhysicalPortAndLogicalDevice() {
    TestHandler handler = new TestHandler(mock(SerialBusManager.class), emptyConfig(), mock(Trigger.class));
    assertEquals("serial_COM3", handler.sessionName(
        new SerialDeviceHandler("COM3", mock(SerialDeviceController.class))));
    assertEquals("serial_COM4", handler.sessionName(
        new SerialDeviceHandler("COM4", mock(SerialDeviceController.class))));
    assertEquals("serial_COM3_change", handler.sessionName(
        new SerialDeviceHandler("COM3_change", mock(ComputedSerialDeviceController.class))));
  }

  @Test
  void computedSessionFailureLeavesPhysicalDeviceRegistered() {
    SerialBusManager manager = mock(SerialBusManager.class);
    TestHandler handler = new TestHandler(manager, emptyConfig(), mock(Trigger.class));
    SerialDeviceController physical = mock(SerialDeviceController.class);
    ComputedSerialDeviceController computed = mock(ComputedSerialDeviceController.class);
    DeviceSessionManagement physicalSession = session("COM3", physical);
    DeviceSessionManagement computedSession = session("COM3_change", computed);
    handler.register(physicalSession);
    handler.register(computedSession);

    handler.closedSession(computedSession);

    assertTrue(handler.contains("COM3"));
    assertFalse(handler.contains("COM3_change"));
    verifyNoInteractions(manager);
    verify(physical, never()).close();
  }

  @Test
  void physicalSessionFailureStopsChildAndUnmountsOwner() throws Exception {
    SerialBusManager manager = mock(SerialBusManager.class);
    TestHandler handler = new TestHandler(manager, emptyConfig(), mock(Trigger.class));
    SerialDeviceController physical = mock(SerialDeviceController.class);
    ComputedSerialDeviceController computed = mock(ComputedSerialDeviceController.class);
    DeviceSessionManagement physicalSession = session("COM3", physical);
    DeviceSessionManagement computedSession = session("COM3_change", computed);
    handler.register(physicalSession);
    handler.register(computedSession);

    handler.closedSession(physicalSession);

    verify(computedSession).stop();
    verify(manager).unmount(physical);
    verify(physical).close();
    assertFalse(handler.contains("COM3"));
    assertFalse(handler.contains("COM3_change"));
  }

  @Test
  void repeatedFailureDoesNotClosePhysicalDeviceTwice() {
    SerialBusManager manager = mock(SerialBusManager.class);
    TestHandler handler = new TestHandler(manager, emptyConfig(), mock(Trigger.class));
    SerialDeviceController physical = mock(SerialDeviceController.class);
    DeviceSessionManagement physicalSession = session("COM3", physical);
    handler.register(physicalSession);
    handler.closedSession(physicalSession);
    handler.closedSession(physicalSession);

    verify(manager, times(1)).unmount(physical);
    verify(physical, times(1)).close();
  }

  @Test
  void delayedFailureCannotUnregisterReplacementSession() {
    SerialBusManager manager = mock(SerialBusManager.class);
    TestHandler handler = new TestHandler(manager, emptyConfig(), mock(Trigger.class));
    SerialDeviceController physical = mock(SerialDeviceController.class);
    DeviceSessionManagement oldSession = session("COM3", physical);
    DeviceSessionManagement newSession = session("COM3", physical);
    handler.register(oldSession);
    handler.register(newSession);

    handler.closedSession(oldSession);

    assertTrue(handler.contains("COM3"));
    verifyNoInteractions(manager);
    verify(physical, never()).close();
  }

  private static DeviceSessionManagement session(String key, DeviceController controller) {
    DeviceHandler device = new SerialDeviceHandler(key, controller);
    DeviceSessionManagement result = mock(DeviceSessionManagement.class);
    when(result.getDevice()).thenReturn(device);
    return result;
  }

  private static SerialDeviceBusConfig emptyConfig() {
    SerialDeviceBusConfig config = new SerialDeviceBusConfig(new ConfigurationProperties());
    config.setDevices(List.of());
    return config;
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

    String sessionName(DeviceHandler handler) {
      return getSessionName(handler);
    }

    void register(DeviceSessionManagement session) {
      foundDevices.put(session.getDevice().getKey(), session.getDevice());
      deviceSessionCreated(session);
    }

    boolean contains(String key) {
      return foundDevices.containsKey(key);
    }
  }
}
