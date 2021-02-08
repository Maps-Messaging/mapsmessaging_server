/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.mqtt_sn.state;

import java.io.IOException;
import javax.security.auth.login.LoginException;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.impl.mqtt_sn.DefaultConstants;
import org.maps.network.protocol.impl.mqtt_sn.MQTT_SNProtocol;
import org.maps.network.protocol.impl.mqtt_sn.packet.ConnAck;
import org.maps.network.protocol.impl.mqtt_sn.packet.Connect;
import org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import org.maps.network.protocol.impl.mqtt_sn.packet.WillTopicRequest;
import org.maps.network.protocol.transformation.TransformationManager;

/**
 * Protocol dictates that we need to a) Receive a Connect packet b) Respond with a Will Topic Request c) Receive a Will Topic Response d) Respond with a Will Message Request e)
 * Receive a Will Message response f) At this point we can establish a valid MQTT session and we respond with a ConAck response
 */
public class InitialConnectionState implements State {

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session oldSession, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) {
    if (mqtt.getControlPacketId() == MQTT_SNPacket.CONNECT) {
      Connect connect = (Connect) mqtt;
      SessionContextBuilder scb = new SessionContextBuilder(connect.getClientId(), protocol);
      scb.setPersistentSession(true);
      scb.setResetState(connect.clean());
      scb.setKeepAlive(connect.getDuration());
      scb.setReceiveMaximum(DefaultConstants.RECEIVE_MAXIMUM);
      scb.setSessionExpiry(endPoint.getConfig().getProperties().getIntProperty("maximumSessionExpiry", DefaultConstants.SESSION_TIME_OUT));
      if (connect.will()) {
        stateEngine.setSessionContextBuilder(scb);
        WillTopicRequest topicRequest = new WillTopicRequest();
        InitialWillTopicState nextState = new InitialWillTopicState(topicRequest);
        stateEngine.setState(nextState);
        return topicRequest;
      } else {
        try {
          MQTT_SNPacket response = new ConnAck(MQTT_SNPacket.ACCEPTED);
          Session session = stateEngine.createSession(scb, protocol, response);
          protocol.setTransformation(TransformationManager.getInstance().getTransformation(protocol.getName(), session.getSecurityContext().getUsername()));
          session.login();
          return response;
        } catch (LoginException | IOException e) {
          ConnAck response = new ConnAck(MQTT_SNPacket.NOT_SUPPORTED);
          response.setCallback(() -> {
            try {
              protocol.close();
            } catch (IOException ioException) {
              // we can ignore this, we are about to close it since we have no idea what it is
            }
          });
        }
      }
    }
    return null;
  }

}
