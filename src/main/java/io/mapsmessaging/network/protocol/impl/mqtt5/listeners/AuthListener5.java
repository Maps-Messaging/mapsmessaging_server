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
    AuthenticationContext context= null;
    if (mqttPacket instanceof Connect5) {
      try {
        MQTT5Protocol mqtt5Protocol = ((MQTT5Protocol) protocol);
        if(mqtt5Protocol.getAuthenticationContext() != null) {
          String serverConfig = mqtt5Protocol.getAuthenticationContext().getAuthMethod();
          String clientConfig = authMethod.getAuthenticationMethod();
          if (!serverConfig.equalsIgnoreCase(clientConfig)) {
            throw new IOException("Unsupported Authentication mechanism, expected " + mqtt5Protocol.getAuthenticationContext().getAuthMethod() + " client specified " + authMethod.getName());
          }
          context = mqtt5Protocol.getAuthenticationContext();
          mqttPacket.getProperties().remove(MessagePropertyFactory.AUTHENTICATION_METHOD);
          context.setConnectMsg(mqttPacket);
        }
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
        if(clientChallenge != null && clientChallenge.length > 0){
          byte state = StatusCode.CONTINUE_AUTHENTICATION.getValue();
          if(context.isComplete()){
            state = StatusCode.SUCCESS.getValue();
          }
          Auth5 auth5 = new Auth5(state, context.getAuthMethod(), clientChallenge);
          ((MQTT5Protocol) protocol).writeFrame(auth5);
        }
        if (context.isComplete()) {
          ((MQTT5Protocol) protocol).setAuthenticationContext(null);
          MQTTPacket5 initial = context.getConnectMsg();
          return ((MQTT5Protocol) protocol).getPacketListenerFactory().getListener(initial.getControlPacketId()).handlePacket(initial, session, endPoint, protocol);
        }
        return null;
      } catch (IOException e) {
        throw new MalformedException("Exception raised in processing Auth challenge", e);
      }
    } else {
      throw new MalformedException("Expected Authentication Context but none found");
    }
  }


}
