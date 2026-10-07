/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Typed, transport-independent MQTT-SN 2.0 input decoding boundary.
 *
 * <p>Never falls back to MQTT-SN 1.2 packet handling. The returned value is
 * a wire DTO, not an internal session or legacy MQTT-SN packet. Unimplemented
 * wire types fail explicitly until their typed decoders are available.</p>
 */
public final class MqttSn2PacketDecoder {

  public record Decoded(MqttSn2PacketType type, Object content) {
  }

  private MqttSn2PacketDecoder() {
  }

  public static Decoded decode(ByteBuffer wire) throws IOException {
    MqttSn2FrameCodec.Frame frame = MqttSn2FrameCodec.decode(wire);
    Object content = switch (frame.type()) {
      case CONNECT -> MqttSn2ConnectCodec.decode(frame);
      case PUBLISH, PUBWOS -> MqttSn2PublishCodec.decode(frame);
      case PUBACK, PUBREC, PUBREL, PUBCOMP, UNSUBACK -> MqttSn2AckCodec.decode(frame);
      case SUBSCRIBE, UNSUBSCRIBE -> MqttSn2SubscriptionCodec.decode(frame);
      case PINGREQ -> MqttSn2ControlCodec.decodePingRequest(frame);
      case PINGRESP -> MqttSn2ControlCodec.decodePingResponse(frame);
      case AUTH -> MqttSn2ControlCodec.decodeAuth(frame);
      case REGISTER -> MqttSn2RegisterCodec.decode(frame);
      case SUBACK -> MqttSn2ReplyCodec.decodeSubAck(frame);
      case SLEEPREQ -> MqttSn2SleepCodec.decodeRequest(frame);
      case SLEEPRESP -> MqttSn2SleepCodec.decodeResponse(frame);
      case WAKEUP -> MqttSn2SleepCodec.decodeWakeup(frame);
      case DISCONNECT -> MqttSn2DisconnectCodec.decode(frame);
      case ADVERTISE -> MqttSn2GatewayCodec.decodeAdvertise(frame);
      case SEARCHGW -> MqttSn2GatewayCodec.decodeSearchGateway(frame);
      case GWINFO -> MqttSn2GatewayCodec.decodeGatewayInfo(frame);
      case FORWARDER_ENCAPSULATION -> MqttSn2EncapsulationCodec.decodeForwarder(frame);
      case CONNECTION_ENCAPSULATION -> MqttSn2EncapsulationCodec.decodeConnection(frame);
      case CONNACK, REGACK, PROTECTION_ENCAPSULATION ->
          throw new IOException("MQTT-SN 2.0 decoder not implemented for " + frame.type());
    };
    return new Decoded(frame.type(), content);
  }
}
