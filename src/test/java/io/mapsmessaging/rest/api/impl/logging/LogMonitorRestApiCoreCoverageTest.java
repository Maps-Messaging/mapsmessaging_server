/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.rest.api.impl.logging;

import io.mapsmessaging.logging.LogEntry;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogMonitorRestApiCoreCoverageTest {

  @Test
  void optionalFilterNormalisationHandlesNullBlankAndTrimmedValues() throws Exception {
    LogMonitorRestApi api = new LogMonitorRestApi();

    assertNull(normalize(api, null));
    assertNull(normalize(api, ""));
    assertNull(normalize(api, "   "));
    assertEquals("level = 'ERROR'", normalize(api, "  level = 'ERROR'  "));
  }

  @Test
  void safeCloseClosesOpenSink() throws Exception {
    LogMonitorRestApi api = new LogMonitorRestApi();
    SseEventSink sink = mock(SseEventSink.class);

    safeClose(api, sink);

    verify(sink).close();
  }

  @Test
  void safeCloseSwallowsCloseFailure() throws Exception {
    LogMonitorRestApi api = new LogMonitorRestApi();
    SseEventSink sink = mock(SseEventSink.class);
    doThrow(new IOException("close failed")).when(sink).close();

    assertDoesNotThrow(() -> safeClose(api, sink));
  }

  @Test
  void buildLogEventUsesExpectedEventNameAndJsonPayload() throws Exception {
    LogMonitorRestApi api = new LogMonitorRestApi();
    Sse sse = mock(Sse.class);
    OutboundSseEvent.Builder builder = mock(OutboundSseEvent.Builder.class);
    OutboundSseEvent event = mock(OutboundSseEvent.class);
    when(sse.newEventBuilder()).thenReturn(builder);
    when(builder.name("logEvent")).thenReturn(builder);
    when(builder.data(eq(String.class), anyString())).thenReturn(builder);
    when(builder.build()).thenReturn(event);
    set(api, "sse", sse);

    assertSame(event, build(api, mock(LogEntry.class)));

    verify(builder).name("logEvent");
    verify(builder).data(eq(String.class), anyString());
    verify(builder).build();
  }

  private static String normalize(LogMonitorRestApi api, String value) throws Exception {
    Method method = LogMonitorRestApi.class.getDeclaredMethod(
        "normalizeOptionalFilter", String.class);
    method.setAccessible(true);
    return (String) method.invoke(api, value);
  }

  private static void safeClose(LogMonitorRestApi api, SseEventSink sink) throws Exception {
    Method method = LogMonitorRestApi.class.getDeclaredMethod(
        "safeClose", SseEventSink.class);
    method.setAccessible(true);
    method.invoke(api, sink);
  }

  private static OutboundSseEvent build(LogMonitorRestApi api, LogEntry entry) throws Exception {
    Method method = LogMonitorRestApi.class.getDeclaredMethod(
        "buildLogEvent", LogEntry.class);
    method.setAccessible(true);
    return (OutboundSseEvent) method.invoke(api, entry);
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = LogMonitorRestApi.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
