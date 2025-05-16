/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.State;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Auth;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MQTT_SN_2_Packet;
import io.mapsmessaging.network.protocol.sasl.SaslAuthenticationMechanism;

import java.io.IOException;

public class AuthenticationState implements State {

  private final MQTT_SNPacket connectRequest;
  private final SaslAuthenticationMechanism saslAuthenticationMechanism;

  public AuthenticationState(Connect connect, SaslAuthenticationMechanism saslAuthenticationMechanism) {
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
        if(saslAuthenticationMechanism.complete()){
          ((Connect)connectRequest).setAuthentication(false); // We have processed it, so now restart
          stateEngine.setState(new InitialConnectionState());
          return stateEngine.handleMQTTEvent(connectRequest, session, endPoint, protocol);
        }
        return new Auth(reasonCodes, saslAuthenticationMechanism.getName(), data);
      } catch (IOException e) {
        e.printStackTrace();
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
