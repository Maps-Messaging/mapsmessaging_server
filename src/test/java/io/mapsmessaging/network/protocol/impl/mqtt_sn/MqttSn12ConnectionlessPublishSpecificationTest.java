package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.config.protocol.PredefinedTopics;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PublishCodec;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt-sn")
class MqttSn12ConnectionlessPublishSpecificationTest {

  private static final String SPEC = "MQTT-SN Version 1.2";
  private static final String SOURCE = ProtocolRequirement.MQTT_SN_12_SOURCE;
  private static final String V2_SOURCE =
      "https://groups.oasis-open.org/discussion/mqtt-sn-20-draft-pdf-august-2026-uploaded-1";

  @Test
  @ProtocolRequirement(specification = SPEC, value = "Section 6.8 QoS -1: connectionless PUBLISH MAY use a predefined topic id", source = SOURCE)
  void qosMinusOnePredefinedTopicPublishesWithoutSession() throws Exception {
    Fixture fixture = fixture("", List.of(predefined(42, "pre/topic")));

    try (MockedStatic<SessionManager> sessions = mockStatic(SessionManager.class)) {
      SessionManager manager = mock(SessionManager.class);
      sessions.when(SessionManager::getInstance).thenReturn(manager);
      when(manager.publish(eq("pre/topic"), any()))
          .thenReturn(CompletableFuture.completedFuture(1));

      fixture.manager.processPacket(
          publish(
              fixture.address,
              0b01100001,
              42,
              "payload".getBytes()));

      verify(manager).publish(eq("pre/topic"), any());
    }
  }

  @Test
  @ProtocolRequirement(specification = SPEC, value = "Section 6.8 QoS -1: connectionless PUBLISH MAY use a two-octet short topic name", source = SOURCE)
  void qosMinusOneShortTopicPublishesLiteralTwoByteNameWithoutSession() throws Exception {
    Fixture fixture = fixture("", List.of());

    try (MockedStatic<SessionManager> sessions = mockStatic(SessionManager.class)) {
      SessionManager manager = mock(SessionManager.class);
      sessions.when(SessionManager::getInstance).thenReturn(manager);
      when(manager.publish(eq("AB"), any()))
          .thenReturn(CompletableFuture.completedFuture(1));

      fixture.manager.processPacket(
          publish(
              fixture.address,
              0b01100010,
              ('A' << 8) | 'B',
              "payload".getBytes()));

      verify(manager).publish(eq("AB"), any());
    }
  }

  @Test
  @ProtocolRequirement(specification = SPEC, value = "Section 6.8 QoS -1: connectionless PUBLISH is restricted to predefined or short topic forms", source = SOURCE)
  void qosMinusOneNormalRegisteredTopicIdIsNotAllowedWithoutSession() throws Exception {
    Fixture fixture = fixture("*,42,dynamic/topic", List.of());

    try (MockedStatic<SessionManager> sessions = mockStatic(SessionManager.class)) {
      SessionManager manager = mock(SessionManager.class);
      sessions.when(SessionManager::getInstance).thenReturn(manager);
      when(manager.publish(anyString(), any()))
          .thenReturn(CompletableFuture.completedFuture(1));

      fixture.manager.processPacket(
          publish(
              fixture.address,
              0b01100000,
              42,
              "payload".getBytes()));

      verify(manager, never()).publish(anyString(), any());
    }
  }

  @Test
  @ProtocolRequirement(specification = SPEC, value = "Sections 6.2 and 6.8: only QoS -1 publishing bypasses prior connection setup", source = SOURCE)
  void ordinaryQosZeroPublishWithoutSessionIsNotAcceptedAsConnectionlessPublish()
      throws Exception {
    Fixture fixture = fixture("*,42,dynamic/topic", List.of());

    try (MockedStatic<SessionManager> sessions = mockStatic(SessionManager.class)) {
      SessionManager manager = mock(SessionManager.class);
      sessions.when(SessionManager::getInstance).thenReturn(manager);

      fixture.manager.processPacket(
          publish(
              fixture.address,
              MQTT_SNPacket.TOPIC_NAME,
              42,
              "payload".getBytes()));

      verifyNoInteractions(manager);
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-SN 2.0 CSD01",
      value = "Sections 3.6 and 4.2.1 PUBWOS by Topic Name without a Virtual Connection",
      source = V2_SOURCE)
  void v2PubwosByNamePublishesWithoutCreatingLegacySession() throws Exception {
    Fixture fixture = fixture("", List.of());
    try (MockedStatic<SessionManager> sessions = mockStatic(SessionManager.class)) {
      SessionManager manager = mock(SessionManager.class);
      sessions.when(SessionManager::getInstance).thenReturn(manager);
      when(manager.publish(eq("v2/telemetry"), any()))
          .thenReturn(CompletableFuture.completedFuture(1));
      ByteBuffer frame = MqttSn2PublishCodec.encode(new MqttSn2PublishCodec.Publish(
          true, 0, false, false, 3, 0, "v2/telemetry", 0, new byte[]{7}));
      Packet packet = new Packet(frame);
      packet.setFromAddress(fixture.address);
      fixture.manager.processPacket(packet);
      verify(manager).publish(eq("v2/telemetry"), any());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-SN 2.0 CSD01",
      value = "CSD01 sections 2.1.3 and 3.6.1.2.1: shared-version UDP dispatch rejects ambiguous unprotected 0x12 alias forms",
      source = V2_SOURCE)
  void v2PubwosPredefinedAliasDoesNotMasqueradeAsLegacySubscribe() throws Exception {
    Fixture fixture = fixture("", List.of(predefined(42, "pre/topic")));
    try (MockedStatic<SessionManager> sessions = mockStatic(SessionManager.class)) {
      SessionManager manager = mock(SessionManager.class);
      sessions.when(SessionManager::getInstance).thenReturn(manager);
      ByteBuffer frame = MqttSn2PublishCodec.encode(new MqttSn2PublishCodec.Publish(
          true, 0, false, false, 1, 0, null, 42, new byte[]{7}));
      Packet packet = new Packet(frame);
      packet.setFromAddress(fixture.address);
      fixture.manager.processPacket(packet);
      verifyNoInteractions(manager);
    }
  }

  private Fixture fixture(String registeredTopics, List<PredefinedTopics> predefined)
      throws Exception {
    SelectorTask selectorTask = mock(SelectorTask.class);
    EndPoint endPoint = mock(EndPoint.class, RETURNS_DEEP_STUBS);
    MqttSnConfig config = mock(MqttSnConfig.class);
    TransformationManager transformationManager = mock(TransformationManager.class);
    ProtocolMessageTransformation transformation = mock(ProtocolMessageTransformation.class);
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 1884);

    when(endPoint.getName()).thenReturn("mqtt-sn-test");
    when(endPoint.getConfig().getProtocolConfig("mqtt-sn")).thenReturn(config);
    when(config.getIdleSessionTimeout()).thenReturn(60L);
    when(config.isEnablePortChanges()).thenReturn(false);
    when(config.isEnableAddressChanges()).thenReturn(false);
    when(config.isAdvertiseGateway()).thenReturn(false);
    when(config.getRegisteredTopics()).thenReturn(registeredTopics);
    when(config.getPredefinedTopicsList()).thenReturn(predefined);
    when(transformationManager.getTransformation(any(), anyString(), eq("mqtt-sn"), eq("<registered>")))
        .thenReturn(transformation);

    MQTTSNInterfaceManager manager;
    try (MockedStatic<TransformationManager> transformations =
             mockStatic(TransformationManager.class)) {
      transformations.when(TransformationManager::getInstance)
          .thenReturn(transformationManager);
      manager = new MQTTSNInterfaceManager((byte) 1, selectorTask, endPoint);
    }

    return new Fixture(manager, address);
  }

  private PredefinedTopics predefined(int id, String topic) {
    PredefinedTopics value = mock(PredefinedTopics.class);
    when(value.getId()).thenReturn(id);
    when(value.getTopic()).thenReturn(topic);
    when(value.getAddress()).thenReturn("*");
    return value;
  }

  private Packet publish(
      InetSocketAddress address,
      int flags,
      int topicField,
      byte[] payload) {
    byte[] wire = new byte[7 + payload.length];
    wire[0] = (byte) wire.length;
    wire[1] = (byte) MQTT_SNPacket.PUBLISH;
    wire[2] = (byte) flags;
    wire[3] = (byte) ((topicField >>> 8) & 0xff);
    wire[4] = (byte) (topicField & 0xff);
    wire[5] = 0;
    wire[6] = 0;
    System.arraycopy(payload, 0, wire, 7, payload.length);

    Packet packet = new Packet(ByteBuffer.wrap(wire));
    packet.setFromAddress(address);
    return packet;
  }

  private record Fixture(
      MQTTSNInterfaceManager manager,
      InetSocketAddress address) {
  }
}
