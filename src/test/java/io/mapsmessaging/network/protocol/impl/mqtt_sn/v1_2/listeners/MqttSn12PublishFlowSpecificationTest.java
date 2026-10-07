package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.TopicAliasManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt-sn")
class MqttSn12PublishFlowSpecificationTest {

  private static final String SPEC = "MQTT-SN Version 1.2";
  private static final String SOURCE = ProtocolRequirement.MQTT_SN_12_SOURCE;

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.12-5.4.13 and 6.6 QoS 1: accepted PUBLISH is acknowledged with PUBACK carrying TopicId and MsgId",
      source = SOURCE)
  void qosOnePublishReturnsMatchingPubAck() throws Exception {
    Fixture fixture = fixture("sensor/temp", 42);
    Publish publish = parsePublish(
        bytes(8, MQTT_SNPacket.PUBLISH, 0x20, 0, 42, 0x12, 0x34, 'x'));

    new PublishListener().handlePacket(
        publish,
        fixture.session,
        fixture.endPoint,
        fixture.protocol,
        fixture.engine);

    ArgumentCaptor<MQTT_SNPacket> response = ArgumentCaptor.forClass(MQTT_SNPacket.class);
    verify(fixture.protocol, timeout(1_000)).writeFrame(response.capture());
    PubAck ack = assertInstanceOf(PubAck.class, response.getValue());
    assertEquals(42, ack.getTopicId());
    assertEquals(0x1234, ack.getMessageId());
    assertEquals(ReasonCodes.SUCCESS, ack.getStatus());
    verify(fixture.destination).storeMessage(any());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.12, 5.4.14 and 6.6 QoS 2: accepted PUBLISH begins the PUBREC/PUBREL/PUBCOMP handshake",
      source = SOURCE)
  void qosTwoPublishReturnsMatchingPubRec() throws Exception {
    Fixture fixture = fixture("sensor/temp", 42);
    Transaction transaction = mock(Transaction.class);
    when(fixture.session.startTransaction("client:4660")).thenReturn(transaction);
    Publish publish = parsePublish(
        bytes(8, MQTT_SNPacket.PUBLISH, 0x40, 0, 42, 0x12, 0x34, 'x'));

    new PublishListener().handlePacket(
        publish,
        fixture.session,
        fixture.endPoint,
        fixture.protocol,
        fixture.engine);

    ArgumentCaptor<MQTT_SNPacket> response = ArgumentCaptor.forClass(MQTT_SNPacket.class);
    verify(fixture.protocol, timeout(1_000)).writeFrame(response.capture());
    PubRec pubRec = assertInstanceOf(PubRec.class, response.getValue());
    assertEquals(0x1234, pubRec.getMessageId());
    verify(transaction).add(eq(fixture.destination), any());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.14 and 6.6 QoS 2: PUBREL is answered by PUBCOMP with the same MsgId",
      source = SOURCE)
  void pubRelReturnsMatchingPubCompAndCompletesTransaction() throws Exception {
    Session session = mock(Session.class);
    Transaction transaction = mock(Transaction.class);
    when(session.getName()).thenReturn("client");
    when(session.getTransaction("client:4660")).thenReturn(transaction);

    PubRel pubRel = assertInstanceOf(
        PubRel.class,
        new PacketFactory().parseFrame(
            new Packet(ByteBuffer.wrap(bytes(4, MQTT_SNPacket.PUBREL, 0x12, 0x34)))));

    PubComp pubComp = assertInstanceOf(
        PubComp.class,
        new PubRelListener().handlePacket(
            pubRel,
            session,
            mock(EndPoint.class),
            mock(MQTT_SNProtocol.class),
            mock(StateEngine.class)));

    assertEquals(0x1234, pubComp.getMessageId());
    pubComp.complete();
    verify(transaction).commit();
    verify(session).closeTransaction(transaction);
  }

  private Fixture fixture(String topicName, int topicId) {
    Session session = mock(Session.class);
    when(session.getName()).thenReturn("client");
    Destination destination = mock(Destination.class);
    when(session.findDestination(topicName, DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));

    MQTT_SNProtocol protocol = mock(MQTT_SNProtocol.class, RETURNS_DEEP_STUBS);
    StateEngine engine = mock(StateEngine.class);
    TopicAliasManager aliases = mock(TopicAliasManager.class);
    when(engine.getTopicAliasManager()).thenReturn(aliases);
    when(aliases.getTopic(any(), eq(topicId), eq(MQTT_SNPacket.TOPIC_NAME)))
        .thenReturn(topicName);

    return new Fixture(
        session,
        destination,
        mock(EndPoint.class),
        protocol,
        engine);
  }

  private Publish parsePublish(byte[] wire) throws Exception {
    Packet packet = new Packet(ByteBuffer.wrap(wire));
    packet.setFromAddress(new InetSocketAddress("127.0.0.1", 1884));
    return assertInstanceOf(
        Publish.class,
        new PacketFactory().parseFrame(packet));
  }

  private static byte[] bytes(int... values) {
    byte[] result = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = (byte) values[i];
    }
    return result;
  }

  private record Fixture(
      Session session,
      Destination destination,
      EndPoint endPoint,
      MQTT_SNProtocol protocol,
      StateEngine engine) {
  }
}
