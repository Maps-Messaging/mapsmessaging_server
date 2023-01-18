package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListenerFactory;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.State;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.listeners.PacketListenerFactoryV2;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Auth;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MQTT_SN_2_Packet;
import io.mapsmessaging.network.protocol.sasl.SaslAuthenticationMechanism;
import java.io.IOException;

public class AuthenticationState implements State {

  private final PacketListenerFactory packetListenerFactory;
  private final MQTT_SNPacket connectRequest;
  private final SaslAuthenticationMechanism saslAuthenticationMechanism;

  public AuthenticationState(Connect connect, SaslAuthenticationMechanism saslAuthenticationMechanism) {
    packetListenerFactory = new PacketListenerFactoryV2();
    this.saslAuthenticationMechanism = saslAuthenticationMechanism;
    connectRequest = connect;
  }

  @Override
  public String getName() {
    return "Authentication";
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session session, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) throws MalformedException {

    if (mqtt.getControlPacketId() == MQTT_SN_2_Packet.AUTH) {
      Auth auth = (Auth) mqtt;
      try {
        byte[] data = saslAuthenticationMechanism.challenge(auth.getData());
        ReasonCodes reasonCodes = saslAuthenticationMechanism.complete() ? ReasonCodes.SUCCESS : ReasonCodes.CONTINUE_AUTHENTICATION;
        return new Auth(reasonCodes, saslAuthenticationMechanism.getName(), data);
      } catch (IOException e) {
        // Log this and allow the session to be closed
      }
    }
    try {
      protocol.close();
    } catch (IOException e) {
      // log the issue
    }
    return null;
  }

  @Override
  public void sendPublish(MQTT_SNProtocol protocol, String destination, MQTT_SNPacket publish) {
    protocol.writeFrame(publish);
  }
}
