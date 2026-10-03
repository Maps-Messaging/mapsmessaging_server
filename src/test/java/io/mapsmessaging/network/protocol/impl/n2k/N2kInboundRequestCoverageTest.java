/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.n2k;

import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.canbus.j1939.CanIdBuilder;
import io.mapsmessaging.canbus.j1939.n2k.framing.FramePacker;
import io.mapsmessaging.network.io.impl.canbus.CanbusEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.n2k.msg.AbstractAisFieldValueSource;
import io.mapsmessaging.schemas.formatters.impl.CanbusFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class N2kInboundRequestCoverageTest {

  @ParameterizedTest
  @CsvSource({
      "126996,255",
      "126996,42",
      "126998,255",
      "126998,42"
  })
  void supportedRequestsReplyToTheRequestingSource(int requestedPgn, int requestDestination)
      throws Exception {
    Fixture fixture = fixture();
    int requester = 37;
    byte[] encoded = {9, 8, 7, 6};
    List<CanFrame> packed =
        List.of(frame(requestedPgn, 42, requester, new byte[]{1, 2, 3}));

    when(fixture.formatter.getParser().encodeFromSource(
        eq(requestedPgn), any(AbstractAisFieldValueSource.class))).thenReturn(encoded);
    when(fixture.framePacker.packFastPacket(
        eq(requestedPgn), anyInt(), eq(42), eq(requester), same(encoded))).thenReturn(packed);

    invokeHandle(
        fixture.protocol,
        frame(59904, requester, requestDestination, requestedPgn(requestedPgn)));

    verify(fixture.formatter.getParser()).encodeFromSource(
        eq(requestedPgn), any(AbstractAisFieldValueSource.class));
    verify(fixture.framePacker).packFastPacket(
        eq(requestedPgn), anyInt(), eq(42), eq(requester), same(encoded));
    verify(fixture.endPoint).writeFrames(same(packed));
    verify(fixture.endPoint, never()).writeFrame(any());
  }

  @Test
  void encodingFailureIsPropagated() throws Exception {
    Fixture fixture = fixture();
    when(fixture.formatter.getParser().encodeFromSource(
        eq(126996), any(AbstractAisFieldValueSource.class)))
        .thenThrow(new IllegalStateException("encode"));

    InvocationTargetException thrown = assertThrows(
        InvocationTargetException.class,
        () -> invokeHandle(
            fixture.protocol,
            frame(59904, 22, 255, requestedPgn(126996))));

    assertInstanceOf(IllegalStateException.class, thrown.getCause());
    assertEquals("encode", thrown.getCause().getMessage());
    verify(fixture.endPoint, never()).writeFrames(anyList());
  }

  @Test
  void responseWriteFailureIsPropagated() throws Exception {
    Fixture fixture = fixture();
    byte[] encoded = {4, 5, 6};
    List<CanFrame> packed = List.of(frame(126998, 42, 19, new byte[]{1}));
    when(fixture.formatter.getParser().encodeFromSource(
        eq(126998), any(AbstractAisFieldValueSource.class))).thenReturn(encoded);
    when(fixture.framePacker.packFastPacket(
        eq(126998), anyInt(), eq(42), eq(19), same(encoded))).thenReturn(packed);
    doThrow(new java.io.IOException("write"))
        .when(fixture.endPoint).writeFrames(same(packed));

    InvocationTargetException thrown = assertThrows(
        InvocationTargetException.class,
        () -> invokeHandle(
            fixture.protocol,
            frame(59904, 19, 255, requestedPgn(126998))));

    assertInstanceOf(java.io.IOException.class, thrown.getCause());
    assertEquals("write", thrown.getCause().getMessage());
  }

  private static void invokeHandle(N2kProtocol protocol, CanFrame frame) throws Exception {
    Method method = N2kProtocol.class.getDeclaredMethod("handleInboundRequest", CanFrame.class);
    method.setAccessible(true);
    method.invoke(protocol, frame);
  }

  private static byte[] requestedPgn(int pgn) {
    return new byte[]{
        (byte) (pgn & 0xFF),
        (byte) ((pgn >> 8) & 0xFF),
        (byte) ((pgn >> 16) & 0xFF)
    };
  }

  private static CanFrame frame(int pgn, int source, int destination, byte[] data) {
    int canId = CanIdBuilder.build(pgn, 6, source, destination);
    return new CanFrame(canId, true, data.length, data);
  }

  private static Fixture fixture() throws Exception {
    N2kProtocol protocol = mock(N2kProtocol.class, CALLS_REAL_METHODS);
    CanbusEndPoint endPoint = mock(CanbusEndPoint.class);
    CanbusFormatter formatter = mock(CanbusFormatter.class, RETURNS_DEEP_STUBS);
    FramePacker framePacker = mock(FramePacker.class);

    setField(protocol, N2kProtocol.class, "formatter", formatter);
    setField(protocol, N2kProtocol.class, "framePacker", framePacker);
    setField(protocol, N2kProtocol.class, "canbusAddress", 42);
    setField(protocol, Protocol.class, "endPoint", endPoint);

    return new Fixture(protocol, endPoint, formatter, framePacker);
  }

  private static void setField(Object target, Class<?> owner, String name, Object value)
      throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private record Fixture(
      N2kProtocol protocol,
      CanbusEndPoint endPoint,
      CanbusFormatter formatter,
      FramePacker framePacker) {
  }
}
