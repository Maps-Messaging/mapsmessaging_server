package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt-sn")
class MqttSn12LifecycleSpecificationTest {

  private static final String SPEC = "MQTT-SN Version 1.2";
  private static final String SOURCE = ProtocolRequirement.MQTT_SN_12_SOURCE;

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.4 and 6.2 CONNECT: Will flag requests WILLTOPICREQ before connection completion",
      source = SOURCE)
  void connectWithWillRequestsWillTopicBeforeConnAck() throws Exception {
    MQTT_SNProtocol protocol = mock(MQTT_SNProtocol.class);
    EndPoint endPoint = mock(EndPoint.class, RETURNS_DEEP_STUBS);
    MqttSnConfig config = mock(MqttSnConfig.class);
    StateEngine engine = mock(StateEngine.class);
    when(endPoint.getConfig().getProtocolConfig("mqtt-sn")).thenReturn(config);

    Connect connect = assertInstanceOf(
        Connect.class,
        parse(bytes(8, MQTT_SNPacket.CONNECT, 0x0c, 0x01, 0, 30, 'i', 'd')));

    MQTT_SNPacket response =
        new InitialConnectionState().handleMQTTEvent(connect, null, endPoint, protocol, engine);

    assertInstanceOf(WillTopicRequest.class, response);
    verify(protocol).setKeepAlive(30_000L);
    verify(engine).setSessionContextBuilder(any(SessionContextBuilder.class));
    verify(engine).setState(any(InitialWillTopicState.class));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.6-5.4.9 and 6.2 Will handshake: WILLTOPIC is followed by WILLMSGREQ",
      source = SOURCE)
  void willTopicIsFollowedByWillMessageRequest() throws Exception {
    MQTT_SNProtocol protocol = mock(MQTT_SNProtocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    StateEngine engine = mock(StateEngine.class);
    SessionContextBuilder builder = mock(SessionContextBuilder.class);
    when(engine.getSessionContextBuilder()).thenReturn(builder);

    WillTopic willTopic = assertInstanceOf(
        WillTopic.class,
        parse(bytes(
            13,
            MQTT_SNPacket.WILLTOPIC,
            0x20,
            'w', 'i', 'l', 'l', '/', 't', 'o', 'p', 'i', 'c')));

    MQTT_SNPacket response =
        new InitialWillTopicState(new WillTopicRequest())
            .handleMQTTEvent(willTopic, null, endPoint, protocol, engine);

    assertInstanceOf(WillMessageRequest.class, response);
    verify(builder).setWillTopic("will/topic");
    verify(engine).setState(any(InitialWillMessageState.class));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.19-5.4.20 and 6.11 Keep-alive: gateway acknowledges PINGREQ with PINGRESP",
      source = SOURCE)
  void connectedPingRequestGetsPingResponse() throws Exception {
    ConnectedState state = new ConnectedState(new ConnAck(ReasonCodes.SUCCESS));

    MQTT_SNPacket response = state.handleMQTTEvent(
        parse(bytes(2, MQTT_SNPacket.PINGREQ)),
        mock(Session.class),
        mock(EndPoint.class),
        mock(MQTT_SNProtocol.class),
        mock(StateEngine.class));

    assertInstanceOf(PingResponse.class, response);
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.21 and 6.12 DISCONNECT: gateway acknowledges a normal disconnect with DISCONNECT without Duration",
      source = SOURCE)
  void normalDisconnectGetsDisconnectAcknowledgement() throws Exception {
    ConnectedState state = new ConnectedState(new ConnAck(ReasonCodes.SUCCESS));
    Session session = mock(Session.class);
    EndPoint endPoint = mock(EndPoint.class);
    MQTT_SNProtocol protocol = mock(MQTT_SNProtocol.class);
    StateEngine engine = mock(StateEngine.class);

    MQTT_SNPacket response = state.handleMQTTEvent(
        parse(bytes(2, MQTT_SNPacket.DISCONNECT)),
        session,
        endPoint,
        protocol,
        engine);

    Disconnect ack = assertInstanceOf(Disconnect.class, response);
    assertEquals(0, ack.getDuration());
    verify(engine).setState(any(DisconnectedState.class));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.21 and 6.14 Sleeping client: DISCONNECT with Duration enters asleep state and is acknowledged without Duration",
      source = SOURCE)
  void disconnectWithDurationEntersSleepAndIsAcknowledged() throws Exception {
    ConnectedState state = new ConnectedState(new ConnAck(ReasonCodes.SUCCESS));
    Session session = mock(Session.class);
    EndPoint endPoint = mock(EndPoint.class);
    MQTT_SNProtocol protocol = mock(MQTT_SNProtocol.class);
    StateEngine engine = mock(StateEngine.class);

    MQTT_SNPacket response = state.handleMQTTEvent(
        parse(bytes(4, MQTT_SNPacket.DISCONNECT, 0x0e, 0x10)),
        session,
        endPoint,
        protocol,
        engine);

    Disconnect ack = assertInstanceOf(Disconnect.class, response);
    assertEquals(0, ack.getDuration());
    verify(engine).sleep();

    ArgumentCaptor<State> stateCaptor = ArgumentCaptor.forClass(State.class);
    verify(engine).setState(stateCaptor.capture());
    SleepState sleepState = assertInstanceOf(SleepState.class, stateCaptor.getValue());

    sleepState.handleMQTTEvent(
        parse(bytes(8, MQTT_SNPacket.CONNECT, 0x04, 0x01, 0, 30, 'i', 'd')),
        session,
        endPoint,
        protocol,
        engine);
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.19-5.4.20 and 6.14 Sleeping client: PINGREQ wakes buffered delivery and PINGRESP terminates the wake cycle",
      source = SOURCE)
  void sleepingPingRequestCompletesWakeCycleWithPingResponse() throws Exception {
    Session session = mock(Session.class);
    EndPoint endPoint = mock(EndPoint.class);
    MQTT_SNProtocol protocol = mock(MQTT_SNProtocol.class);
    StateEngine engine = mock(StateEngine.class);
    SleepState state = new SleepState(3600, protocol);

    MQTT_SNPacket response = state.handleMQTTEvent(
        parse(bytes(8, MQTT_SNPacket.PINGREQ, 's', 'l', 'e', 'e', 'p', 'y')),
        session,
        endPoint,
        protocol,
        engine);

    assertNull(response);
    ArgumentCaptor<Runnable> completion = ArgumentCaptor.forClass(Runnable.class);
    verify(engine).emptyQueue(eq(0), completion.capture());
    completion.getValue().run();
    verify(protocol).writeFrame(any(PingResponse.class));

    ConnAck connAck = assertInstanceOf(
        ConnAck.class,
        state.handleMQTTEvent(
            parse(bytes(8, MQTT_SNPacket.CONNECT, 0x04, 0x01, 0, 30, 'i', 'd')),
            session,
            endPoint,
            protocol,
            engine));
    assertEquals(ReasonCodes.SUCCESS, connAck.getStatus());
    verify(engine).wake();
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
