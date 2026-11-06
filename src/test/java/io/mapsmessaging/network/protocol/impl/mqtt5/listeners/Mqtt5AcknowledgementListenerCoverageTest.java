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
import io.mapsmessaging.network.protocol.impl.mqtt.RemoteSubscriptionTracker;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
  void sub_ack_completes_pending_remote_subscription_and_releases_control_id(int packetId) {
    SubAckFixture fixture = subAckFixture(packetId);
    int registered = fixture.tracker.register(List.of("remote/topic"));
    assertEquals(packetId, registered);
    var completion = fixture.tracker.awaitAcknowledgements();
    assertFalse(completion.isDone());
    assertNotNull(fixture.packetIdManager.receivedPacket(packetId));

    assertNull(fixture.acknowledge(packetId, StatusCode.SUCCESS));

    assertTrue(completion.isDone());
    assertDoesNotThrow(completion::join);
    assertNull(fixture.packetIdManager.receivedPacket(packetId));
    assertEquals(0, fixture.packetIdManager.size());
  }

  @ParameterizedTest
  @MethodSource("packetIdentifiers")
  void unknown_sub_ack_preserves_publish_mapping_and_pending_remote_subscription(int packetId) {
    SubAckFixture fixture = subAckFixture(packetId);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    long messageId = 200_000L + packetId;
    int publishId = fixture.packetIdManager.nextPacketIdentifier(subscription, messageId);
    assertEquals(packetId, publishId);
    PacketIdentifierMap mapping = fixture.packetIdManager.receivedPacket(publishId);
    int controlId = fixture.tracker.register(List.of("remote/topic"));
    var completion = fixture.tracker.awaitAcknowledgements();

    assertNull(fixture.acknowledge(packetId, StatusCode.SUCCESS));

    assertFalse(completion.isDone());
    assertSame(mapping, fixture.packetIdManager.receivedPacket(publishId));
    assertNotNull(fixture.packetIdManager.receivedPacket(controlId));
    assertEquals(2, fixture.packetIdManager.size());
    verifyNoInteractions(subscription);

    assertNull(fixture.acknowledge(controlId, StatusCode.SUCCESS));
    assertDoesNotThrow(completion::join);
    assertNull(fixture.packetIdManager.receivedPacket(controlId));
    assertSame(mapping, fixture.packetIdManager.receivedPacket(publishId));
    assertEquals(1, fixture.packetIdManager.size());
    verifyNoInteractions(subscription);
  }

  @ParameterizedTest
  @EnumSource(value = StatusCode.class, names = {"SUCCESS_QOS_1", "SUCCESS_QOS_2"})
  void sub_ack_accepts_granted_qos_results(StatusCode status) {
    SubAckFixture fixture = subAckFixture(1);
    int packetId = fixture.tracker.register(List.of("remote/topic"));
    var completion = fixture.tracker.awaitAcknowledgements();

    assertNull(fixture.acknowledge(packetId, status));

    assertTrue(completion.isDone());
    assertDoesNotThrow(completion::join);
    assertEquals(0, fixture.packetIdManager.size());
  }

  @ParameterizedTest
  @EnumSource(value = StatusCode.class,
      names = {"UNSPECIFIED_ERROR", "NOT_AUTHORISED", "WILDCARD_SUBSCRIPTION_NOT_SUPPORTED"})
  void sub_ack_rejection_reports_exact_topic_and_unsigned_reason(StatusCode status) {
    SubAckFixture fixture = subAckFixture(1);
    int packetId = fixture.tracker.register(List.of("allowed/topic", "restricted/topic"));
    var completion = fixture.tracker.awaitAcknowledgements();

    assertNull(fixture.acknowledge(packetId, StatusCode.SUCCESS, status));

    CompletionException failure = assertThrows(CompletionException.class, completion::join);
    assertInstanceOf(IOException.class, failure.getCause());
    assertTrue(failure.getCause().getMessage().contains("restricted/topic"));
    assertTrue(failure.getCause().getMessage().contains(
        String.format("0x%02X", Byte.toUnsignedInt(status.getValue()))));
    assertNull(fixture.packetIdManager.receivedPacket(packetId));
    assertEquals(0, fixture.packetIdManager.size());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2})
  void sub_ack_result_count_mismatch_fails_completion_and_releases_control_id(int resultCount) {
    SubAckFixture fixture = subAckFixture(1);
    int packetId = fixture.tracker.register(List.of("remote/topic"));
    var completion = fixture.tracker.awaitAcknowledgements();
    StatusCode[] results = resultCount == 0
        ? new StatusCode[0] : new StatusCode[]{StatusCode.SUCCESS, StatusCode.SUCCESS_QOS_1};

    assertNull(fixture.acknowledge(packetId, results));

    CompletionException failure = assertThrows(CompletionException.class, completion::join);
    assertInstanceOf(IOException.class, failure.getCause());
    assertTrue(failure.getCause().getMessage().contains("SUBACK result count mismatch"));
    assertNull(fixture.packetIdManager.receivedPacket(packetId));
    assertEquals(0, fixture.packetIdManager.size());
  }

  @Test
  void sub_ack_waits_for_all_pending_packets_and_ignores_unknown_and_duplicate_ids() {
    SubAckFixture fixture = subAckFixture(1);
    int first = fixture.tracker.register(List.of("first/topic"));
    int second = fixture.tracker.register(List.of("second/topic"));
    var completion = fixture.tracker.awaitAcknowledgements();

    assertNull(fixture.acknowledge(65_535, StatusCode.NOT_AUTHORISED));
    assertFalse(completion.isDone());
    assertEquals(2, fixture.packetIdManager.size());
    assertNull(fixture.acknowledge(second, StatusCode.SUCCESS_QOS_2));
    assertFalse(completion.isDone());
    assertNull(fixture.packetIdManager.receivedPacket(second));
    assertNotNull(fixture.packetIdManager.receivedPacket(first));
    assertNull(fixture.acknowledge(second, StatusCode.NOT_AUTHORISED));
    assertFalse(completion.isDone());
    assertEquals(1, fixture.packetIdManager.size());

    assertNull(fixture.acknowledge(first, StatusCode.SUCCESS_QOS_1));

    assertTrue(completion.isDone());
    assertDoesNotThrow(completion::join);
    assertEquals(0, fixture.packetIdManager.size());
    assertNull(fixture.acknowledge(first, StatusCode.NOT_AUTHORISED));
    assertDoesNotThrow(completion::join);
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

  private static SubAckFixture subAckFixture(int nextPacketId) {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    PacketIdManager packetIdManager = new PacketIdManager();
    // Advance the real allocator without reserving IDs to retain packet boundary coverage.
    for (int identifier = 1; identifier < nextPacketId; identifier++) {
      packetIdManager.nextPacketIdentifier();
    }
    RemoteSubscriptionTracker tracker = new RemoteSubscriptionTracker(packetIdManager);
    when(protocol.getPacketIdManager()).thenReturn(packetIdManager);
    when(protocol.getRemoteSubscriptionTracker()).thenReturn(tracker);
    return new SubAckFixture(protocol, packetIdManager, tracker);
  }

  private record SubAckFixture(
      MQTT5Protocol protocol,
      PacketIdManager packetIdManager,
      RemoteSubscriptionTracker tracker) {

    private MQTTPacket5 acknowledge(int packetId, StatusCode... results) {
      return new SubAckListener5().handlePacket(new SubAck5(packetId, results), null, null, protocol);
    }
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
