/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.AuthenticationContext;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Auth5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Connect5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationData;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationMethod;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import java.io.IOException;

public class AuthListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {
    // Need to push this until we have finished our auth
    AuthenticationMethod authMethod = (AuthenticationMethod) mqttPacket.getProperties().get(MessagePropertyFactory.AUTHENTICATION_METHOD);
    AuthenticationContext context;
    if (mqttPacket instanceof Connect5) {
      try {
        context = new AuthenticationContext(authMethod.getAuthenticationMethod(), endPoint.getConfig().getProperties(), mqttPacket);
        ((MQTT5Protocol) protocol).setAuthenticationContext(context);
        mqttPacket.getProperties().remove(MessagePropertyFactory.AUTHENTICATION_METHOD);
      } catch (IOException e) {
        throw new MalformedException("Exception raised creating Authentication Server", e);
      }
    } else {
      context = ((MQTT5Protocol) protocol).getAuthenticationContext();
    }

    //
    // OK we have done the initialization above, lets process the auth packet
    if (context != null) {
      AuthenticationData clientData = (AuthenticationData) mqttPacket.getProperties().get(MessagePropertyFactory.AUTHENTICATION_DATA);
      try {
        byte[] clientChallenge = context.evaluateResponse(clientData.getAuthenticationData());
        if (context.isComplete()) {
          ((MQTT5Protocol) protocol).setAuthenticationContext(null);
          MQTTPacket5 initial = context.getParkedConnect();
          return ((MQTT5Protocol) protocol).getPacketListenerFactory().getListener(initial.getControlPacketId()).handlePacket(initial, session, endPoint, protocol);
        }
        Auth5 auth = new Auth5(context.getAuthMethod(), clientChallenge);
        auth.setReasonCode(StatusCode.CONTINUE_AUTHENTICATION.getValue());
        return auth;
      } catch (IOException e) {
        throw new MalformedException("Exception raised in processing Auth challenge", e);
      }
    } else {
      throw new MalformedException("Expected Authentication Context but none found");
    }
  }


}
