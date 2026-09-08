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

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.dto.rest.config.auth.AuthConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ConnAckListener extends BaseConnectionListener {

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol) throws MalformedException {

    MQTTProtocol  mqttProtocol = (MQTTProtocol)protocol;
    if(mqttProtocol.getSession() != null){
      mqttProtocol.getSession().resumeState();
      protocol.setConnected(true);
      return null; // already connected
    }
    AuthConfigDTO config =  ((EndPointConnectionServerConfig)endPoint.getConfig()).getAuthConfig();

    String sess = config.getSessionId();
    String user = config.getUsername();
    String pass = config.getPassword();

    ConnAck connAck = (ConnAck) mqttPacket;
    if (connAck.getResponseCode() != ConnAck.SUCCESS) {
      closeEndPoint(endPoint);
      protocol.setConnected(false);
      return null;
    }

    char[] password = pass == null ? null : pass.toCharArray();
    SessionContextBuilder scb = getBuilder(protocol, sess, false, (int) protocol.getKeepAlive(), user, password);
    // Session Present describes state held by the remote broker. It must not delete the
    // local engine subscription and its queued messages when the peer loses that state.
    scb.setResetState(false);
    CompletableFuture<Session> sessionFuture = createSession(endPoint, protocol, scb, sess);
    sessionFuture.thenApply(session1 -> {
      session1.resumeState();
      protocol.setConnected(true);
      return session1;
    });

    try {
      sessionFuture.get();
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      closeEndPoint(endPoint);
      protocol.setConnected(false);
    } catch (ExecutionException executionException) {
      closeEndPoint(endPoint);
      protocol.setConnected(false);
    }

    return null;
  }

  private void closeEndPoint(EndPoint endPoint) throws MalformedException {
    try {
      endPoint.close();
    } catch (IOException e) {
      throw new MalformedException("Unable to close the rejected MQTT connection", e);
    }
  }
}
