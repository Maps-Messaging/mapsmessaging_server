package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.WillTask;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.TopicAliasManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt-sn")
class MqttSn12RegistrationAndWillSpecificationTest {

  private static final String SPEC = "MQTT-SN Version 1.2";
  private static final String SOURCE = ProtocolRequirement.MQTT_SN_12_SOURCE;

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.10-5.4.11 and 6.5 Topic registration: accepted REGISTER returns assigned TopicId and the same MsgId",
      source = SOURCE)
  void acceptedRegisterReturnsAssignedTopicIdAndMatchingMessageId() {
    Session session = mock(Session.class);
    StateEngine engine = mock(StateEngine.class);
    TopicAliasManager aliases = mock(TopicAliasManager.class);
    when(engine.getTopicAliasManager()).thenReturn(aliases);
    when(aliases.getTopicAlias("sensor/temp")).thenReturn((short) 42);

    RegisterAck ack = assertInstanceOf(
        RegisterAck.class,
        new RegisterListener().handlePacket(
            new Register((short) 0, (short) 0x1234, "sensor/temp"),
            session,
            mock(EndPoint.class),
            mock(Protocol.class),
            engine));

    assertEquals(42, ack.getTopicId());
    assertEquals(0x1234, ack.getMessageId());
    assertEquals(ReasonCodes.SUCCESS, ack.getStatus());
    verify(session).findDestination("sensor/temp", DestinationType.TOPIC);
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.10-5.4.11 and 6.5 Topic registration: rejected REGISTER preserves MsgId and returns a rejection ReturnCode",
      source = SOURCE)
  void exhaustedRegistrationCapacityReturnsRejectedRegAck() {
    Session session = mock(Session.class);
    StateEngine engine = mock(StateEngine.class);
    TopicAliasManager aliases = mock(TopicAliasManager.class);
    when(engine.getTopicAliasManager()).thenReturn(aliases);
    when(aliases.getTopicAlias("sensor/temp")).thenReturn((short) -1);

    RegisterAck ack = assertInstanceOf(
        RegisterAck.class,
        new RegisterListener().handlePacket(
            new Register((short) 0, (short) 0x4321, "sensor/temp"),
            session,
            mock(EndPoint.class),
            mock(Protocol.class),
            engine));

    assertEquals(0x4321, ack.getMessageId());
    assertEquals(ReasonCodes.NOT_SUPPORTED, ack.getStatus());
    verify(session, never()).findDestination(anyString(), any());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.22, 5.4.24 and 6.4 Will update: WILLTOPICUPD replaces topic, QoS and retain and receives WILLTOPICRESP",
      source = SOURCE)
  void willTopicUpdateReplacesStoredWillTopicSettings() throws Exception {
    Session session = mock(Session.class);
    WillTask will = mock(WillTask.class);
    when(session.getWillTask()).thenReturn(will);

    WillTopicUpdate update = assertInstanceOf(
        WillTopicUpdate.class,
        parse(bytes(
            11,
            MQTT_SNPacket.WILLTOPICUPD,
            0x30,
            'w', 'i', 'l', 'l', '/', 'n', 'e', 'w')));

    WillTopicResponse response = assertInstanceOf(
        WillTopicResponse.class,
        new WillTopicUpdateListener().handlePacket(
            update,
            session,
            mock(EndPoint.class),
            mock(Protocol.class, RETURNS_DEEP_STUBS),
            mock(StateEngine.class)));

    assertEquals(ReasonCodes.SUCCESS, response.getStatus());
    verify(will).updateTopic("will/new");
    verify(will).updateQoS(QualityOfService.AT_LEAST_ONCE);
    verify(will).updateRetainFlag(true);
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.23, 5.4.25 and 6.4 Will update: WILLMSGUPD replaces Will message and receives WILLMSGRESP",
      source = SOURCE)
  void willMessageUpdateReplacesStoredWillPayload() throws Exception {
    Session session = mock(Session.class);
    WillTask will = mock(WillTask.class);
    when(session.getWillTask()).thenReturn(will);
    byte[] payload = "new-will".getBytes(StandardCharsets.UTF_8);

    byte[] wire = new byte[2 + payload.length];
    wire[0] = (byte) wire.length;
    wire[1] = (byte) MQTT_SNPacket.WILLMSGUPD;
    System.arraycopy(payload, 0, wire, 2, payload.length);

    WillMessageUpdate update = assertInstanceOf(WillMessageUpdate.class, parse(wire));
    WillMessageResponse response = assertInstanceOf(
        WillMessageResponse.class,
        new WillMessageUpdateListener().handlePacket(
            update,
            session,
            mock(EndPoint.class),
            mock(Protocol.class, RETURNS_DEEP_STUBS),
            mock(StateEngine.class)));

    assertEquals(ReasonCodes.SUCCESS, response.getStatus());
    verify(will).updateMessage(payload);
  }

  private MQTT_SNPacket parse(byte[] wire) throws Exception {
    return new PacketFactory().parseFrame(new Packet(ByteBuffer.wrap(wire)));
  }

  private static byte[] bytes(int... values) {
    byte[] result = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = (byte) values[i];
    }
    return result;
  }
}
