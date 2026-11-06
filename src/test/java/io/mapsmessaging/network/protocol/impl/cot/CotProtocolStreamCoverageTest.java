/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import java.util.concurrent.CompletableFuture;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CotProtocolStreamCoverageTest {

  @ParameterizedTest
  @MethodSource("streamCases")
  void publishesCompleteDocumentsAndRetainsOnlyRemainder(
      List<String> chunks, List<String> expectedDocuments, String expectedRemainder) throws Exception {
    Fixture fixture = new Fixture();
    for (String chunk : chunks) {
      fixture.append(chunk);
    }

    ArgumentCaptor<Message> published = ArgumentCaptor.forClass(Message.class);
    verify(fixture.destination, times(expectedDocuments.size())).storeMessage(published.capture());
    assertEquals(expectedDocuments, published.getAllValues().stream()
        .map(message -> new String(message.getOpaqueData(), StandardCharsets.UTF_8)).toList());
    verify(fixture.session, times(expectedDocuments.isEmpty() ? 0 : 1))
        .findDestination("/configured/cot", DestinationType.TOPIC);
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
            List.of(), ""),
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

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = CotProtocol.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static final class Fixture {
    private final Session session = mock(Session.class);
    private final Destination destination = mock(Destination.class);
    private final CotProtocol protocol = mock(CotProtocol.class, CALLS_REAL_METHODS);

    private Fixture() throws Exception {
      when(session.findDestination("/configured/cot", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(destination));
      setField(protocol, "streamCodec", new CotXmlStreamCodec(1_048_576));
      setField(protocol, "inboundDestinationName", "/configured/cot");
      setField(protocol, "session", session);
      setField(protocol, "publicationFuture", CompletableFuture.completedFuture(null));
    }

    private void append(String chunk) throws Exception {
      byte[] data = bytes(chunk);
      Packet packet = new Packet(data.length, false);
      packet.put(data);
      packet.flip();
      protocol.processPacket(packet);
    }

  }
}
