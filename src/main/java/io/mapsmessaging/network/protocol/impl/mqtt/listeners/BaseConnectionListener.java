/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.mqtt.DefaultConstants;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public abstract class BaseConnectionListener extends PacketListener {

  protected CompletableFuture<Session> createSession(EndPoint endPoint, ProtocolImpl protocol, SessionContextBuilder scb, String sessionId) {
    CompletableFuture<Session> future = SessionManager.getInstance().createAsync(scb.build(), protocol);
    future.thenApply(session -> {
      try {
        ((MQTTProtocol) protocol).setSession(session);
        session.login();
        ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(
            endPoint.getProtocol(),
            endPoint.getName(),
            "mqtt",
            session.getSecurityContext().getUsername()
        );

        protocol.setTransformation(transformation);
        return session;
      } catch (IOException ioe) {
        logger.log(ServerLogMessages.MQTT_CONNECT_LISTENER_SESSION_EXCEPTION, ioe, sessionId);
        future.completeExceptionally(new MalformedException("Unable to construct the required Will Topic"));
      }
      try {
        endPoint.close();
      } catch (IOException e) {
        // Ignore
      }
      return null;
    });
    return future;
  }

  protected SessionContextBuilder getBuilder(EndPoint endPoint, ProtocolImpl protocol, String sessionId, boolean isClean, int keepAlive, String username, char[] pass) {
    SessionContextBuilder scb = new SessionContextBuilder(sessionId, new ProtocolClientConnection(protocol));
    scb.setPersistentSession(true)
        .setResetState(isClean)
        .setKeepAlive(keepAlive)
        .setSessionExpiry( ((MQTTProtocol)protocol).getMqttConfig().getMaximumSessionExpiry());

    if (pass != null && pass.length > 0) {
      scb.setPassword(pass);
    }
    if (username != null && !username.isEmpty()) {
      scb.setUsername(username);
    }
    scb.setReceiveMaximum(DefaultConstants.RECEIVE_MAXIMUM);
    return scb;
  }

}
