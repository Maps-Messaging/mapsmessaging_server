/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.io.impl.canbus;

import io.mapsmessaging.canbus.device.CanDevice;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.admin.EndPointJMX;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CanbusEndPointCoreCoverageTest {

  @Test
  void readFrameDelegatesToBoundDevice() throws Exception {
    Harness h = harness(false);
    CanFrame frame = frame(0x101);
    when(h.device.readFrame()).thenReturn(frame);

    assertSame(frame, h.endPoint.readFrame());
  }

  @Test
  void writeFrameDelegatesToBoundDevice() throws Exception {
    Harness h = harness(false);
    CanFrame frame = frame(0x102);

    h.endPoint.writeFrame(frame);

    verify(h.device).writeFrame(frame);
  }

  @Test
  void writeFramesDelegatesToBoundDevice() throws Exception {
    Harness h = harness(false);
    List<CanFrame> frames = List.of(frame(0x103), frame(0x104));

    h.endPoint.writeFrames(frames);

    verify(h.device).writeFrames(frames);
  }

  @Test
  void closedEndpointRejectsReadAndWrites() throws Exception {
    Harness h = harness(true);

    assertThrows(IOException.class, h.endPoint::readFrame);
    assertThrows(IOException.class, () -> h.endPoint.writeFrame(frame(0x105)));
    assertThrows(IOException.class, () -> h.endPoint.writeFrames(List.of(frame(0x106))));
  }

  @Test
  void writeWithNoBoundDeviceIsNoOp() throws Exception {
    Harness h = harness(false);
    set(h.endPoint, "canDevice", null);

    assertDoesNotThrow(() -> h.endPoint.writeFrame(frame(0x107)));
    assertDoesNotThrow(() -> h.endPoint.writeFrames(List.of(frame(0x108))));
  }

  @Test
  void interfaceInformationRequiresBoundDevice() throws Exception {
    Harness h = harness(false);
    set(h.endPoint, "canDevice", null);

    IllegalStateException error =
        assertThrows(IllegalStateException.class, h.endPoint::getInterfaceInformation);

    assertEquals("CAN bus device is not currently bound", error.getMessage());
  }

  @Test
  void closeIsIdempotentAndClosesDeviceAndMbeanOnce() throws Exception {
    Harness h = harness(false);

    h.endPoint.close();
    h.endPoint.close();

    verify(h.device, times(1)).close();
    verify(h.mbean, times(1)).close();
    assertNull(get(h.endPoint, "canDevice"));
  }

  @Test
  void closePropagatesDeviceIOExceptionAfterClearingState() throws Exception {
    Harness h = harness(false);
    doThrow(new IOException("close failed")).when(h.device).close();

    IOException error = assertThrows(IOException.class, h.endPoint::close);

    assertEquals("close failed", error.getMessage());
    assertNull(get(h.endPoint, "canDevice"));
  }

  @Test
  void unbindIgnoresNonActivePortName() throws Exception {
    Harness h = harness(false);
    com.fazecast.jSerialComm.SerialPort port = mock(com.fazecast.jSerialComm.SerialPort.class);
    when(port.getSystemPortName()).thenReturn("ttyUSB9");
    set(h.endPoint, "activeSerialPortName", "ttyUSB0");

    h.endPoint.unbind(port);

    verify(h.device, never()).close();
  }

  @Test
  void protocolSurfaceIsStable() throws Exception {
    Harness h = harness(false);

    assertEquals("canbus", h.endPoint.getProtocol());
    assertEquals("", h.endPoint.getRemoteSocketAddress());
    assertEquals(0, h.endPoint.sendPacket(null));
    assertEquals(0, h.endPoint.readPacket(null));
    assertNull(h.endPoint.register(1, null));
    assertNull(h.endPoint.deregister(1));
  }

  private static Harness harness(boolean closed) throws Exception {
    CanbusEndPoint endPoint = mock(CanbusEndPoint.class, CALLS_REAL_METHODS);
    CanDevice device = mock(CanDevice.class);
    EndPointJMX mbean = mock(EndPointJMX.class);
    CanbusConfigDTO config = mock(CanbusConfigDTO.class);

    set(endPoint, "closed", new AtomicBoolean(closed));
    set(endPoint, "mbean", mbean);
    set(endPoint, "config", config);
    set(endPoint, "deviceLock", new Object());
    set(endPoint, "canDevice", device);
    set(endPoint, "activeSerialPort", null);
    set(endPoint, "activeSerialPortName", null);

    return new Harness(endPoint, device, mbean);
  }

  private static CanFrame frame(int id) {
    byte[] data = new byte[]{1,2,3,4};
    return new CanFrame(id, false, data.length, data);
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = CanbusEndPoint.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Object get(Object target, String name) throws Exception {
    Field field = CanbusEndPoint.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private record Harness(CanbusEndPoint endPoint, CanDevice device, EndPointJMX mbean) {
  }
}
