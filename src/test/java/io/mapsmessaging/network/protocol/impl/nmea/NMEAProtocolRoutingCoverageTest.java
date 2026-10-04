/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.nmea.sentences.SentenceFactory;
import io.mapsmessaging.schemas.formatters.ParseMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NMEAProtocolRoutingCoverageTest {

  @Test
  void unregisteredSentenceUsesTopicTemplate() throws Exception {
    Harness h = harness(true);

    prepare(h.protocol, "$GPGGA,1", "GPGGA", java.util.List.<String>of("1").iterator());

    verify(h.session).findDestination("/nmea/device/GPGGA", DestinationType.TOPIC);
    verify(h.destination).storeMessage(any(Message.class));
  }

  @Test
  void exactRemoteMappingOverridesTopicTemplate() throws Exception {
    Harness h = harness(true);
    h.protocol.subscribeRemote(
        "GPRMC", "/mapped/position", QualityOfService.AT_MOST_ONCE,
        null, null, null, Map.of());

    prepare(h.protocol, "$GPRMC,1", "GPRMC", java.util.List.<String>of("1").iterator());

    verify(h.session).findDestination("/mapped/position", DestinationType.TOPIC);
  }

  @Test
  void wildcardRemoteMappingReplacesHashWithSentenceId() throws Exception {
    Harness h = harness(true);
    h.protocol.subscribeRemote(
        "#", "/mapped/#", QualityOfService.AT_MOST_ONCE,
        null, null, null, Map.of());

    prepare(h.protocol, "$GPVTG,1", "GPVTG", java.util.List.<String>of("1").iterator());

    verify(h.session).findDestination("/mapped/GPVTG", DestinationType.TOPIC);
  }

  @Test
  void wildcardMappingWithoutHashUsesLiteralDestination() throws Exception {
    Harness h = harness(true);
    h.protocol.subscribeRemote(
        "#", "/mapped/all", QualityOfService.AT_MOST_ONCE,
        null, null, null, Map.of());

    prepare(h.protocol, "$GPGLL,1", "GPGLL", java.util.List.<String>of("1").iterator());

    verify(h.session).findDestination("/mapped/all", DestinationType.TOPIC);
  }

  @Test
  void unmatchedSentenceWithRegistrationsIsNotPublished() throws Exception {
    Harness h = harness(true);
    h.protocol.subscribeRemote(
        "GPGGA", "/mapped/gga", QualityOfService.AT_MOST_ONCE,
        null, null, null, Map.of());

    prepare(h.protocol, "$GPRMC,1", "GPRMC", java.util.List.<String>of("1").iterator());

    verify(h.session, never()).findDestination(anyString(), any());
  }

  @Test
  void publishingDisabledDoesNotResolveDestination() throws Exception {
    Harness h = harness(false);

    prepare(h.protocol, "$GPGGA,1", "GPGGA", java.util.List.<String>of("1").iterator());

    verify(h.session, never()).findDestination(anyString(), any());
  }

  @Test
  void destinationIsCachedPerSentenceId() throws Exception {
    Harness h = harness(true);

    prepare(h.protocol, "$GPGGA,1", "GPGGA", java.util.List.<String>of("1").iterator());
    prepare(h.protocol, "$GPGGA,2", "GPGGA", java.util.List.<String>of("2").iterator());

    verify(h.session, times(1)).findDestination("/nmea/device/GPGGA", DestinationType.TOPIC);
    verify(h.destination, times(2)).storeMessage(any(Message.class));
  }

  @Test
  void nullDestinationDoesNotAttemptStore() throws Exception {
    Harness h = harness(true);
    when(h.session.findDestination(anyString(), eq(DestinationType.TOPIC)))
        .thenReturn(CompletableFuture.completedFuture(null));

    assertDoesNotThrow(() ->
        prepare(h.protocol, "$GPGGA,1", "GPGGA", java.util.List.<String>of("1").iterator()));
  }

  @Test
  void rawFormatPublishesRawSentence() throws Exception {
    Harness h = harness(true);

    prepare(h.protocol, "$GPGGA,raw", "GPGGA", java.util.List.<String>of("raw").iterator());

    var captor = org.mockito.ArgumentCaptor.forClass(Message.class);
    verify(h.destination).storeMessage(captor.capture());
    assertEquals("$GPGGA,raw", new String(captor.getValue().getOpaqueData()));
  }

  @Test
  void protocolIdentityIsStable() throws Exception {
    Harness h = harness(false);

    assertEquals("NMEA-0183", h.protocol.getName());
    assertEquals("0183", h.protocol.getVersion());
    assertEquals("GPS_device", h.protocol.getSessionId());
  }

  private static Harness harness(boolean publish) throws Exception {
    NMEAProtocol protocol = mock(NMEAProtocol.class, CALLS_REAL_METHODS);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    EndPoint endPoint = mock(EndPoint.class);
    ProtocolConfigDTO config = mock(ProtocolConfigDTO.class);
    SentenceFactory factory = mock(SentenceFactory.class);

    when(endPoint.getName()).thenReturn("device");
    when(session.findDestination(anyString(), eq(DestinationType.TOPIC)))
        .thenReturn(CompletableFuture.completedFuture(destination));

    set(protocol, "session", session);
    set(protocol, "sentenceMap", new LinkedHashMap<String, Destination>());
    set(protocol, "sentenceFactory", factory);
    set(protocol, "format", "raw");
    set(protocol, "serverLocationSentence", "");
    set(protocol, "publishRecords", publish);
    set(protocol, "registeredSentences", new LinkedHashMap<>());
    set(protocol, "topicTemplate", "/nmea/device/{sentence}");
    set(protocol, "storeOffline", true);
    set(protocol, "qos", QualityOfService.AT_LEAST_ONCE);
    setInherited(protocol, "endPoint", endPoint);
    setInherited(protocol, "protocolConfig", config);

    return new Harness(protocol, session, destination);
  }

  private static void prepare(
      NMEAProtocol protocol, String raw, String id, Iterator<String> entries) throws Exception {
    Method method = NMEAProtocol.class.getDeclaredMethod(
        "prepareSentence", String.class, String.class, Iterator.class);
    method.setAccessible(true);
    method.invoke(protocol, raw, id, entries);
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = NMEAProtocol.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static void setInherited(Object target, String name, Object value) throws Exception {
    Class<?> type = NMEAProtocol.class.getSuperclass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
        return;
      } catch (NoSuchFieldException ignored) {
        type = type.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }

  private record Harness(NMEAProtocol protocol, Session session, Destination destination) {
  }
}
