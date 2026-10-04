/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdentifierMap;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubComp5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubRec5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubRel5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.SubAck5;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Mqtt5AcknowledgementListenerCoverageTest {

  @ParameterizedTest
  @MethodSource("packetIdentifiers")
  void pubAckCompletesMappedPacketAndAcknowledgesSubscription(int packetId) {
    ListenerFixture fixture = fixture();
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    long messageId = 100_000L + packetId;
    PacketIdentifierMap mapping = new PacketIdentifierMap(packetId, subscription, messageId);
    when(fixture.packetIdManager.completePacketId(packetId)).thenReturn(mapping);

    MQTTPacket5 response = new PubAckListener5().handlePacket(
        new PubAck5(packetId, StatusCode.SUCCESS),
        fixture.session,
        fixture.endPoint,
        fixture.protocol);

    assertNull(response);
    verify(fixture.packetIdManager).completePacketId(packetId);
    verify(subscription).ackReceived(messageId);
  }

  @ParameterizedTest
  @MethodSource("packetIdentifiers")
  void pubAckWithUnknownPacketIdIsSafelyIgnored(int packetId) {
    ListenerFixture fixture = fixture();
    when(fixture.packetIdManager.completePacketId(packetId)).thenReturn(null);

    MQTTPacket5 response = new PubAckListener5().handlePacket(
        new PubAck5(packetId, StatusCode.SUCCESS),
        fixture.session,
        fixture.endPoint,
        fixture.protocol);

    assertNull(response);
    verify(fixture.packetIdManager).completePacketId(packetId);
  }

  @ParameterizedTest
  @MethodSource("packetIdentifiers")
  void subAckCompletesMappedPacketAndAcknowledgesSubscription(int packetId) {
    ListenerFixture fixture = fixture();
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    long messageId = 200_000L + packetId;
    PacketIdentifierMap mapping = new PacketIdentifierMap(packetId, subscription, messageId);
    when(fixture.packetIdManager.completePacketId(packetId)).thenReturn(mapping);

    MQTTPacket5 response = new SubAckListener5().handlePacket(
        new SubAck5(packetId, new StatusCode[]{StatusCode.SUCCESS}),
        fixture.session,
        fixture.endPoint,
        fixture.protocol);

    assertNull(response);
    verify(fixture.packetIdManager).completePacketId(packetId);
    verify(subscription).ackReceived(messageId);
  }

  @ParameterizedTest
  @MethodSource("packetIdentifiers")
  void subAckWithUnknownPacketIdIsSafelyIgnored(int packetId) {
    ListenerFixture fixture = fixture();
    when(fixture.packetIdManager.completePacketId(packetId)).thenReturn(null);

    MQTTPacket5 response = new SubAckListener5().handlePacket(
        new SubAck5(packetId, new StatusCode[]{StatusCode.SUCCESS}),
        fixture.session,
        fixture.endPoint,
        fixture.protocol);

    assertNull(response);
    verify(fixture.packetIdManager).completePacketId(packetId);
  }

  @ParameterizedTest
  @MethodSource("packetIdentifiers")
  void pubRecForMappedPacketAcknowledgesAndReturnsMatchingPubRel(int packetId) {
    ListenerFixture fixture = fixture();
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    long messageId = 300_000L + packetId;
    PacketIdentifierMap mapping = new PacketIdentifierMap(packetId, subscription, messageId);
    when(fixture.packetIdManager.receivedPacket(packetId)).thenReturn(mapping);

    MQTTPacket5 response = new PubRecListener5().handlePacket(
        new PubRec5(packetId),
        fixture.session,
        fixture.endPoint,
        fixture.protocol);

    PubRel5 pubRel = assertInstanceOf(PubRel5.class, response);
    assertEquals(packetId, pubRel.getPacketIdentifier());
    assertEquals(StatusCode.SUCCESS, pubRel.getStatusCode());
    verify(fixture.packetIdManager).receivedPacket(packetId);
    verify(fixture.packetIdManager, never()).completePacketId(packetId);
    verify(subscription).ackReceived(messageId);
  }

  @Test
  void unknownPubRecReturnsPacketIdentifierNotFoundPubRel_MSG373() {
    ListenerFixture fixture = fixture();
    int packetId = 321;
    when(fixture.packetIdManager.receivedPacket(packetId)).thenReturn(null);

    MQTTPacket5 response = new PubRecListener5().handlePacket(
        new PubRec5(packetId),
        fixture.session,
        fixture.endPoint,
        fixture.protocol);

    PubRel5 pubRel = assertInstanceOf(PubRel5.class, response);
    assertEquals(packetId, pubRel.getPacketIdentifier());
    assertEquals(StatusCode.PACKET_IDENTIFIER_NOT_FOUND, pubRel.getStatusCode());
    verify(fixture.packetIdManager).receivedPacket(packetId);
  }

  @ParameterizedTest
  @MethodSource("packetIdentifiers")
  void pubCompCompletesPacketAndRemovesClientOutstandingId(int packetId) {
    ListenerFixture fixture = fixture();
    io.mapsmessaging.utilities.collections.NaturalOrderedLongList outstanding =
        mock(io.mapsmessaging.utilities.collections.NaturalOrderedLongList.class);
    when(fixture.protocol.getClientOutstanding()).thenReturn(outstanding);

    MQTTPacket5 response = new PubCompListener5().handlePacket(
        new PubComp5(packetId),
        fixture.session,
        fixture.endPoint,
        fixture.protocol);

    assertNull(response);
    verify(fixture.packetIdManager).completePacketId(packetId);
    verify(outstanding).remove((long) packetId);
  }

  @ParameterizedTest
  @MethodSource("packetIdentifiers")
  void pubRelWithoutTransactionReturnsPubCompAndCallbackIsHarmless(int packetId) throws Exception {
    ListenerFixture fixture = fixture();
    when(fixture.session.getName()).thenReturn("mqtt5-session");
    when(fixture.session.getTransaction("mqtt5-session_" + packetId)).thenReturn(null);

    PubComp5 response = assertInstanceOf(
        PubComp5.class,
        new PubRelListener5().handlePacket(
            new PubRel5(packetId),
            fixture.session,
            fixture.endPoint,
            fixture.protocol));

    assertEquals(packetId, response.getPacketIdentifier());
    assertDoesNotThrow(response::complete);
    verify(fixture.session, never()).closeTransaction(org.mockito.ArgumentMatchers.any());
  }

  @ParameterizedTest
  @MethodSource("packetIdentifiers")
  void pubRelCommitsTransactionAndClosesItAfterPubCompWrite(int packetId) throws Exception {
    ListenerFixture fixture = fixture();
    Transaction transaction = mock(Transaction.class);
    when(fixture.session.getName()).thenReturn("mqtt5-session");
    when(fixture.session.getTransaction("mqtt5-session_" + packetId)).thenReturn(transaction);

    PubComp5 response = assertInstanceOf(
        PubComp5.class,
        new PubRelListener5().handlePacket(
            new PubRel5(packetId),
            fixture.session,
            fixture.endPoint,
            fixture.protocol));

    assertEquals(packetId, response.getPacketIdentifier());
    verify(transaction).commit();
    verify(fixture.session, never()).closeTransaction(transaction);

    response.complete();

    verify(fixture.session).closeTransaction(transaction);

    response.complete();
    verify(fixture.session).closeTransaction(transaction);
  }

  @Test
  void pubRelCommitFailureClosesProtocolAndStillReturnsPubComp() throws Exception {
    ListenerFixture fixture = fixture();
    int packetId = 77;
    Transaction transaction = mock(Transaction.class);
    when(fixture.session.getName()).thenReturn("mqtt5-session");
    when(fixture.session.getTransaction("mqtt5-session_" + packetId)).thenReturn(transaction);
    doThrow(new IOException("commit failed")).when(transaction).commit();

    PubComp5 response = assertInstanceOf(
        PubComp5.class,
        new PubRelListener5().handlePacket(
            new PubRel5(packetId),
            fixture.session,
            fixture.endPoint,
            fixture.protocol));

    assertEquals(packetId, response.getPacketIdentifier());
    verify(fixture.protocol).close();
  }

  @Test
  void pubRelCommitAndProtocolCloseFailuresAreContained() throws Exception {
    ListenerFixture fixture = fixture();
    int packetId = 78;
    Transaction transaction = mock(Transaction.class);
    when(fixture.session.getName()).thenReturn("mqtt5-session");
    when(fixture.session.getTransaction("mqtt5-session_" + packetId)).thenReturn(transaction);
    doThrow(new IOException("commit failed")).when(transaction).commit();
    doThrow(new IOException("close failed")).when(fixture.protocol).close();

    PubComp5 response = assertDoesNotThrow(() -> assertInstanceOf(
        PubComp5.class,
        new PubRelListener5().handlePacket(
            new PubRel5(packetId),
            fixture.session,
            fixture.endPoint,
            fixture.protocol)));

    assertEquals(packetId, response.getPacketIdentifier());
    verify(fixture.protocol).close();
  }

  @Test
  void pubCompCompletionContainsTransactionCloseFailure() throws Exception {
    ListenerFixture fixture = fixture();
    int packetId = 79;
    Transaction transaction = mock(Transaction.class);
    when(fixture.session.getName()).thenReturn("mqtt5-session");
    when(fixture.session.getTransaction("mqtt5-session_" + packetId)).thenReturn(transaction);
    doThrow(new IOException("close transaction failed"))
        .when(fixture.session).closeTransaction(transaction);

    PubComp5 response = assertInstanceOf(
        PubComp5.class,
        new PubRelListener5().handlePacket(
            new PubRel5(packetId),
            fixture.session,
            fixture.endPoint,
            fixture.protocol));

    assertDoesNotThrow(response::complete);
    verify(fixture.session).closeTransaction(transaction);
  }

  private static ListenerFixture fixture() {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    PacketIdManager packetIdManager = mock(PacketIdManager.class);
    Session session = mock(Session.class);
    EndPoint endPoint = mock(EndPoint.class);
    when(protocol.getPacketIdManager()).thenReturn(packetIdManager);
    return new ListenerFixture(protocol, packetIdManager, session, endPoint);
  }

  private static Stream<Integer> packetIdentifiers() {
    return Stream.of(1, 2, 127, 128, 32_767, 65_535);
  }

  private record ListenerFixture(
      MQTT5Protocol protocol,
      PacketIdManager packetIdManager,
      Session session,
      EndPoint endPoint) {
  }
}
