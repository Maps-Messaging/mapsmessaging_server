/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ConnAckListener extends BaseConnectionListener {

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {

    String sess = protocol.getEndPoint().getConfig().getProperties().getProperty("sessionId");
    String user = protocol.getEndPoint().getConfig().getProperties().getProperty("username");
    String pass = protocol.getEndPoint().getConfig().getProperties().getProperty("password");

    SessionContextBuilder scb = getBuilder(endPoint, protocol, sess, false, 30000, user, pass.toCharArray());
    protocol.setKeepAlive(30000);
    CompletableFuture<Session> sessionFuture = createSession(endPoint, protocol, scb, sess);
    sessionFuture.thenApply(session1 -> {
      session1.resumeState();
      protocol.setConnected(true);
      return session1;
    });

    try {
      sessionFuture.get();
    } catch (Exception ioException) {
      Thread.currentThread().interrupt();
      try {
        endPoint.close();
        protocol.setConnected(false);
      } catch (IOException e) {
        MalformedException malformedException = new MalformedException();
        malformedException.initCause(e);
        throw malformedException;
      }
    }

    return null;
  }
}
