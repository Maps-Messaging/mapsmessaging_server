/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.io.impl.canbus;

import io.mapsmessaging.canbus.device.CanDevice;
import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.io.EndPoint;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CanbusEndPointLifecycleTest {

  @Test
  void socketcan_creation_opts_in_to_bounded_reads() throws Exception {
    AtomicReference<List<?>> arguments = new AtomicReference<>();
    CanbusConfigDTO config = new CanbusConfigDTO();
    config.setDeviceName("vcan0");
    config.setQueuedWritesEnabled(false);
    try (MockedConstruction<SocketCanDevice> devices = mockConstruction(SocketCanDevice.class,
        (device, context) -> arguments.set(context.arguments()));
        MockedConstruction<EndPointJMX> beans = mockConstruction(EndPointJMX.class)) {
      try (CanbusEndPoint endpoint = new CanbusEndPoint(config, mock(CanbusEndPointServer.class), List.of())) {
        assertEquals(List.of("vcan0", true), arguments.get());
      }
      verify(devices.constructed().getFirst()).close();
    }
  }

  @Test
  void failed_device_creation_does_not_leak_connection_accounting() {
    CanbusConfigDTO config = new CanbusConfigDTO();
    config.setDeviceName("vcan0");
    long before = EndPoint.currentConnections.sum();
    try (MockedConstruction<SocketCanDevice> ignored = mockConstruction(SocketCanDevice.class,
        (device, context) -> { throw new IOException("device creation failed"); })) {
      assertThrows(RuntimeException.class,
          () -> new CanbusEndPoint(config, mock(CanbusEndPointServer.class), List.of()));
    }
    assertEquals(before, EndPoint.currentConnections.sum());
  }

  @Test
  void close_runs_base_accounting_and_callback_once_even_when_device_close_fails() throws Exception {
    CanbusConfigDTO config = new CanbusConfigDTO();
    config.setDeviceName("vcan0");
    config.setQueuedWritesEnabled(false);
    long connections = EndPoint.currentConnections.sum();
    try (MockedConstruction<SocketCanDevice> devices = mockConstruction(SocketCanDevice.class);
        MockedConstruction<EndPointJMX> beans = mockConstruction(EndPointJMX.class)) {
      CanbusEndPoint endpoint = new CanbusEndPoint(config, mock(CanbusEndPointServer.class), List.of());
      AtomicInteger callbacks = new AtomicInteger();
      endpoint.setCloseHandler(callbacks::incrementAndGet);
      IOException failure = new IOException("device close failed");
      CanDevice device = devices.constructed().getFirst();
      doThrow(failure).when(device).close();
      long disconnections = EndPoint.totalDisconnections.sum();

      assertSame(failure, assertThrows(IOException.class, endpoint::close));
      endpoint.close();

      assertTrue(endpoint.isClosed());
      assertEquals(1, callbacks.get());
      assertEquals(disconnections + 1, EndPoint.totalDisconnections.sum());
      assertEquals(connections, EndPoint.currentConnections.sum());
      verify(device).close();
    }
  }

  @Test
  void socketcan_read_failure_notifies_lifecycle_owner() throws Exception {
    CanDevice device = mock(CanDevice.class);
    IOException failure = new IOException("interface disappeared");
    when(device.readFrame()).thenThrow(failure);
    CanbusEndPoint endpoint = endpoint(device, false);
    CanbusEndPointServer server = mock(CanbusEndPointServer.class);
    set(endpoint, EndPoint.class, "server", server);

    assertSame(failure, assertThrows(IOException.class, endpoint::readFrame));
    verify(server).handleCloseEndPoint(endpoint);
  }

  @Test
  void serial_read_failure_keeps_existing_serial_discovery_path() throws Exception {
    CanDevice device = mock(CanDevice.class);
    IOException failure = new IOException("serial read failed");
    when(device.readFrame()).thenThrow(failure);
    CanbusEndPoint endpoint = endpoint(device, true);
    CanbusEndPointServer server = mock(CanbusEndPointServer.class);
    set(endpoint, EndPoint.class, "server", server);

    assertSame(failure, assertThrows(IOException.class, endpoint::readFrame));
    verify(server, never()).handleCloseEndPoint(any());
  }

  private static CanbusEndPoint endpoint(CanDevice device, boolean serial) throws Exception {
    CanbusEndPoint endpoint = mock(CanbusEndPoint.class, CALLS_REAL_METHODS);
    CanbusConfigDTO config = new CanbusConfigDTO();
    if (serial) {
      config.setSerialConfig(mock(SerialConfigDTO.class, RETURNS_DEEP_STUBS));
    }
    set(endpoint, CanbusEndPoint.class, "closed", new AtomicBoolean());
    set(endpoint, CanbusEndPoint.class, "deviceLock", new Object());
    set(endpoint, CanbusEndPoint.class, "canDevice", device);
    set(endpoint, CanbusEndPoint.class, "mbean", mock(EndPointJMX.class));
    set(endpoint, CanbusEndPoint.class, "config", config);
    return endpoint;
  }

  private static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
