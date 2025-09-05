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

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.AuthenticationContext;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationData;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationMethod;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuthListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, Protocol protocol) throws MalformedException {
    // Need to push this until we have finished our auth
    AuthenticationContext context= null;
    if (mqttPacket instanceof Connect5) {
      MQTT5Protocol mqtt5Protocol = ((MQTT5Protocol) protocol);
      AuthenticationMethod authMethod = (AuthenticationMethod) mqttPacket.getProperties().get(MessagePropertyFactory.AUTHENTICATION_METHOD);
      if (mqtt5Protocol.getAuthenticationContext() != null && authMethod != null) {
        if (!mqtt5Protocol.getAuthenticationContext().getAuthMethod().equalsIgnoreCase(authMethod.getAuthenticationMethod())) {
          return rejectAuth(authMethod, protocol);
        }
        context = mqtt5Protocol.getAuthenticationContext();
        mqttPacket.getProperties().remove(MessagePropertyFactory.AUTHENTICATION_METHOD);
        context.setConnectMsg(mqttPacket);
        context.setAuthenticationMethod(authMethod);
      }
    } else {
      context = ((MQTT5Protocol) protocol).getAuthenticationContext();
    }

    //
    // OK we have done the initialization above, lets process the auth packet
    if (context != null) {
      return handleAuth(context, protocol, mqttPacket, session, endPoint);
    } else {
      throw new MalformedException("Expected Authentication Context but none found");
    }
  }
  
  private MQTTPacket5 rejectAuth(AuthenticationMethod authMethod, Protocol protocol){
    ConnAck5 connAck = new ConnAck5();
    connAck.setStatusCode(StatusCode.BAD_AUTHENTICATION_METHOD); // MQTT Standard
    connAck.getProperties().add(authMethod);
    connAck.setCallback(() -> SimpleTaskScheduler.getInstance().schedule(() -> {
      try {
        protocol.close();
      } catch (IOException e1) {
        logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e1);
      }
    }, 100, TimeUnit.MILLISECONDS));
    return connAck;

  }

  private MQTTPacket5 handleAuth(AuthenticationContext context, Protocol protocol, MQTTPacket5 mqttPacket, Session session, EndPoint endPoint) throws MalformedException {
      AuthenticationData clientData = (AuthenticationData) mqttPacket.getProperties().get(MessagePropertyFactory.AUTHENTICATION_DATA);
      byte[] clientChallenge;
      try {
        clientChallenge = context.evaluateResponse(clientData.getAuthenticationData());
      } catch (IOException e) {
        // Auth has failed, so we need to send a ConnAck with a reject and close the connection
        ConnAck5 connAck = new ConnAck5();
        connAck.setStatusCode(StatusCode.BAD_USERNAME_PASSWORD); // MQTT Standard
        connAck.getProperties().add(context.getAuthenticationMethod());
        connAck.setCallback(() -> SimpleTaskScheduler.getInstance().schedule(() -> {
          try {
            protocol.close();
          } catch (IOException e1) {
            logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e1);
          }
        }, 100, TimeUnit.MILLISECONDS));
        return connAck;
      }

      // Auth is ok to continue, so check to see if it has completed or needs more information

      if(clientChallenge != null && clientChallenge.length > 0){
        byte state = StatusCode.CONTINUE_AUTHENTICATION.getValue();
        if(context.isComplete()){
          state = StatusCode.SUCCESS.getValue();
        }
        Auth5 auth5 = new Auth5(state, context.getAuthMethod(), clientChallenge);
        ((MQTT5Protocol) protocol).writeFrame(auth5);
      }
      if (context.isComplete()) {
        MQTTPacket5 initial = context.getConnectMsg();
        return ((MQTT5Protocol) protocol).getPacketListenerFactory().getListener(initial.getControlPacketId()).handlePacket(initial, session, endPoint, protocol);
      }
      return null;
  }
}
