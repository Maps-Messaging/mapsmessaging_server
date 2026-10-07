/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.cot.CotStreamDecoder;
import io.mapsmessaging.SubSystemManager;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.state.StateManagerAgent;
import io.mapsmessaging.state.adapter.cot.CotIngestAdapter;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.tak.CotToTwinMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CotProtocolStreamCoverageTest {

  @ParameterizedTest
  @MethodSource("streamCases")
  void publishesCompleteDocumentsAndRetainsOnlyRemainder(
      List<String> chunks, List<String> expectedDocuments, String expectedRemainder) throws Exception {
    try (Fixture fixture = new Fixture()) {
      for (String chunk : chunks) {
        fixture.append(chunk);
      }

      ArgumentCaptor<byte[]> archived = ArgumentCaptor.forClass(byte[].class);
      verify(fixture.adapter, times(expectedDocuments.size())).publishLocal(archived.capture());

      ArgumentCaptor<byte[]> routed = ArgumentCaptor.forClass(byte[].class);
      verify(fixture.mapper, times(expectedDocuments.size()))
          .routeToTwinManager(eq(fixture.twinManager), routed.capture(), eq("cot-ingest"));

      assertEquals(expectedDocuments, asStrings(archived.getAllValues()));
      assertEquals(expectedDocuments, asStrings(routed.getAllValues()));
      verifyNoInteractions(fixture.session);
    }
  }

  private static Stream<Arguments> streamCases() {
    return Stream.of(
        Arguments.of(List.of(""), List.of(), ""),
        Arguments.of(List.of("noise"), List.of(), "noise"),
        Arguments.of(List.of("<event"), List.of(), "<event"),
        Arguments.of(List.of("<event>"), List.of(), "<event>"),
        Arguments.of(List.of("</event>"), List.of(), "</event>"),
        Arguments.of(List.of("<EVENT></EVENT>"), List.of(), "<EVENT></EVENT>"),
        Arguments.of(List.of("<event></event>"), List.of("<event></event>"), ""),
        Arguments.of(List.of("<event uid='1'></event>"), List.of("<event uid='1'></event>"), ""),
        Arguments.of(List.of("noise<event></event>"), List.of("<event></event>"), ""),
        Arguments.of(List.of("</event><event></event>"), List.of("<event></event>"), ""),
        Arguments.of(List.of("<event></event>tail"), List.of("<event></event>"), "tail"),
        Arguments.of(List.of("<event></event><event"), List.of("<event></event>"), "<event"),
        Arguments.of(List.of("<event>1</event><event>2</event>"),
            List.of("<event>1</event>", "<event>2</event>"), ""),
        Arguments.of(List.of("<event>1</event>junk<event>2</event>"),
            List.of("<event>1</event>", "<event>2</event>"), ""),
        Arguments.of(List.of("<?xml?><event></event>"),
            List.of("<event></event>"), ""),
        Arguments.of(List.of("<?xml?> <event></event>"),
            List.of("<event></event>"), ""),
        Arguments.of(List.of("<?xml?>\n\t<event></event>"),
            List.of("<event></event>"), ""),
        Arguments.of(List.of("<?xml?>X<event></event>"),
            List.of("<event></event>"), ""),
        Arguments.of(List.of("noise<?xml?> <event></event>"),
            List.of("<event></event>"), ""),
        Arguments.of(List.of("<?xml<event></event>"),
            List.of("<event></event>"), ""),
        Arguments.of(List.of("<?xml?><event>1</event><?xml?><event>2</event>"),
            List.of("<event>1</event>", "<event>2</event>"), ""),
        Arguments.of(List.of("<ev", "ent></event>"),
            List.of("<event></event>"), ""),
        Arguments.of(List.of("<event></ev", "ent>"),
            List.of("<event></event>"), ""),
        Arguments.of(List.of("<?x", "ml?>", " ", "<event></event>"),
            List.of("<event></event>"), ""),
        Arguments.of(List.of("<event", " uid='split'", ">", "</event>"),
            List.of("<event uid='split'></event>"), ""),
        Arguments.of(List.of("noise<ev", "ent>1</ev", "ent>tail"),
            List.of("<event>1</event>"), "tail"),
        Arguments.of(List.of("<event>1</event><ev", "ent>2</event>"),
            List.of("<event>1</event>", "<event>2</event>"), ""),
        Arguments.of(List.of("<?xml?>", "   ", "<event>1</event>"),
            List.of("<event>1</event>"), ""),
        Arguments.of(List.of("abc<?xml?>", "\n", "<event>1</event>xyz"),
            List.of("<event>1</event>"), "xyz"),
        Arguments.of(List.of("<event>one</event>\n<event>two</event>\n"),
            List.of("<event>one</event>", "<event>two</event>"), "\n")
    );
  }

  private static List<String> asStrings(List<byte[]> values) {
    return values.stream().map(value -> new String(value, StandardCharsets.UTF_8)).toList();
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  @SuppressWarnings("unchecked")
  private static <T> T invoke(Object target, String name, Class<?>[] parameterTypes, Object... args)
      throws Exception {
    Method method = CotProtocol.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(target, args);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = CotProtocol.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static final class Fixture implements AutoCloseable {
    private final Session session = mock(Session.class);
    private final CotIngestAdapter adapter = mock(CotIngestAdapter.class);
    private final TwinManager twinManager = mock(TwinManager.class);
    private final CotToTwinMapper mapper = mock(CotToTwinMapper.class);
    private final CotProtocol protocol = mock(CotProtocol.class, CALLS_REAL_METHODS);
    private final MockedStatic<MessageDaemon> daemonStatic;

    private Fixture() throws Exception {
      MessageDaemon daemon = mock(MessageDaemon.class);
      SubSystemManager subSystemManager = mock(SubSystemManager.class);
      StateManagerAgent stateManager = mock(StateManagerAgent.class);
      when(daemon.getSubSystemManager()).thenReturn(subSystemManager);
      when(subSystemManager.getStateManager()).thenReturn(stateManager);
      when(stateManager.getStateMessageAdapter(CotIngestAdapter.class))
          .thenReturn(Optional.of(adapter));
      when(stateManager.getTwinManager()).thenReturn(twinManager);
      when(adapter.publishLocal(any(byte[].class))).thenReturn(true);
      when(mapper.routeToTwinManager(
          eq(twinManager), any(byte[].class), eq("cot-ingest"))).thenReturn(true);

      daemonStatic = mockStatic(MessageDaemon.class);
      daemonStatic.when(MessageDaemon::getInstance).thenReturn(daemon);

      setField(protocol, "buffer", new ByteArrayOutputStream());
      setField(protocol, "streamDecoder", new CotStreamDecoder(1_048_576));
      setField(protocol, "cotToTwinMapper", mapper);
      setField(protocol, "session", session);
    }

    private void append(String chunk) throws Exception {
      byte[] data = bytes(chunk);
      Packet packet = new Packet(data.length, false);
      packet.put(data);
      packet.flip();
      invoke(protocol, "appendAndProcess", new Class<?>[]{Packet.class}, packet);
    }

    private String bufferContents() throws Exception {
      Field field = CotProtocol.class.getDeclaredField("buffer");
      field.setAccessible(true);
      return ((ByteArrayOutputStream) field.get(protocol)).toString(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
      daemonStatic.close();
    }
  }
}
