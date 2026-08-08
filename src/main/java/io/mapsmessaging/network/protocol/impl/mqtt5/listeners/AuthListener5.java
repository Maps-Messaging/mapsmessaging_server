/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Auth5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Connect5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationData;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationMethod;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuthListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, Protocol protocol) throws MalformedException {
    AuthenticationContext context = null;

    if (mqttPacket instanceof Connect5) {
      MQTT5Protocol mqtt5Protocol = (MQTT5Protocol) protocol;
      AuthenticationMethod authMethod = (AuthenticationMethod) mqttPacket.getProperties().get(MessagePropertyFactory.AUTHENTICATION_METHOD);

      if (mqtt5Protocol.getAuthenticationContext() != null && authMethod != null) {
        if (!mqtt5Protocol.getAuthenticationContext().getAuthMethod().equals(authMethod.getAuthenticationMethod())) {
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

    if (context != null) {
      return handleAuth(context, protocol, mqttPacket, session, endPoint);
    }

    throw new MalformedException("Expected Authentication Context but none found");
  }

  private MQTTPacket5 rejectAuth(AuthenticationMethod authMethod, Protocol protocol) {
    ConnAck5 connAck = new ConnAck5();
    connAck.setStatusCode(StatusCode.BAD_AUTHENTICATION_METHOD);
    connAck.getProperties().add(authMethod);
    connAck.setCallback(
        () ->
            SimpleTaskScheduler.getInstance()
                .schedule(
                    () -> {
                      try {
                        protocol.close();
                      } catch (IOException e) {
                        logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
                      }
                    },
                    100,
                    TimeUnit.MILLISECONDS));

    return connAck;
  }

  private MQTTPacket5 handleAuth(AuthenticationContext context, Protocol protocol, MQTTPacket5 mqttPacket, Session session, EndPoint endPoint) throws MalformedException {
    AuthenticationData clientData = (AuthenticationData) mqttPacket.getProperties().get(MessagePropertyFactory.AUTHENTICATION_DATA);
    if (clientData == null) {
      throw new MalformedException("Authentication Data is required for SASL authentication");
    }

    byte[] serverResponse;
    try {
      serverResponse = context.evaluateResponse(clientData.getAuthenticationData());
    } catch (IOException e) {
      ConnAck5 connAck = new ConnAck5();
      connAck.setStatusCode(StatusCode.BAD_USERNAME_PASSWORD);
      connAck.getProperties().add(context.getAuthenticationMethod());
      connAck.setCallback(
          () ->
              SimpleTaskScheduler.getInstance()
                  .schedule(
                      () -> {
                        try {
                          protocol.close();
                        } catch (IOException closeException) {
                          logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, closeException);
                        }
                      },
                      100,
                      TimeUnit.MILLISECONDS));
      return connAck;
    }

    if (!context.isComplete()) {
      if (serverResponse != null && serverResponse.length > 0) {
        Auth5 auth = new Auth5(StatusCode.CONTINUE_AUTHENTICATION.getValue(), context.getAuthMethod(), serverResponse);
        ((MQTT5Protocol) protocol).writeFrame(auth);
      }
      return null;
    }

    MQTTPacket5 initial = context.getConnectMsg();
    MQTTPacket5 response = ((MQTT5Protocol) protocol).getPacketListenerFactory().getListener(initial.getControlPacketId()).handlePacket(initial, session, endPoint, protocol);

    if (response instanceof ConnAck5 connAck && connAck.getStatusCode() == StatusCode.SUCCESS && serverResponse != null && serverResponse.length > 0) {
      connAck.getProperties().add(new AuthenticationData(serverResponse));
    }

    return response;
  }
}