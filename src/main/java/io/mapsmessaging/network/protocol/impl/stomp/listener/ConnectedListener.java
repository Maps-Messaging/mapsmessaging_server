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

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Connected;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.state.ClientConnectedState;
import io.mapsmessaging.network.protocol.impl.stomp.state.StateEngine;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.security.uuid.UuidGenerator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ConnectedListener extends BaseConnectListener {

  @Override
  public void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) {
    Connected connected = (Connected) frame;

    // No version header supplied
    String versionHeader = connected.getHeader().get("version");
    float version = processVersion(engine, versionHeader);
    if (Float.isNaN(version)) {
      return; // Unable to process the versioning
    }

    CompletableFuture<Session> future = createSession(engine).thenApply(session -> {
      try {
        session.login();
        engine.setSession(session);
        ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(
            engine.getProtocol().getEndPoint().getProtocol(),
            engine.getProtocol().getEndPoint().getName(),
            "stomp",
            session.getSecurityContext().getUsername()
        );
        engine.getProtocol().setTransformation(transformation);
        engine.changeState(new ClientConnectedState());
        session.resumeState();
      } catch (Exception failedAuth) {
        handleFailedAuth(failedAuth, engine);
      }
      return null;
    });

    try {
      future.get();
    } catch (InterruptedException | ExecutionException e) {
     Thread.currentThread().interrupt();
    }
  }

  private CompletableFuture<Session> createSession(StateEngine engine) {
    SessionContextBuilder scb = new SessionContextBuilder(UuidGenerator.getInstance().generate().toString(), new ProtocolClientConnection(engine.getProtocol()));
    scb.setPersistentSession(false);
    scb.setKeepAlive(120);
    scb.setReceiveMaximum(engine.getProtocol().getMaxReceiveSize());
    scb.setSessionExpiry(0); // There is no idle Stomp sessions, so once disconnected the state is thrown away
    return SessionManager.getInstance().createAsync(scb.build(), engine.getProtocol());
  }
}
