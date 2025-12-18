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

package io.mapsmessaging.network.protocol.impl.nats.listener;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.protocol.impl.nats.frames.ConnectFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.OkFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.ConnectedState;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.security.uuid.UuidGenerator;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ConnectListener implements FrameListener {

  @Override
  public void frameEvent(NatsFrame frame, SessionState engine, boolean endOfBuffer) throws IOException {
    ConnectFrame connect = (ConnectFrame) frame;

    // No version header supplied
    CompletableFuture<Session> future = createSession(engine, connect).thenApply(session -> {
      try {
        session.login();
        engine.setSession(session);
        ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(
            engine.getProtocol().getEndPoint().getProtocol(),
            engine.getProtocol().getEndPoint().getName(),
            "NATS",
            session.getSecurityContext().getUsername()

        );
        engine.getProtocol().setProtocolMessageTransformation(transformation);
        engine.setVerbose(connect.isVerbose());
        engine.changeState(new ConnectedState());
        engine.setEchoEvents(connect.isEcho());
        engine.setHeaders(connect.isHeaders());
        session.resumeState();
        if (connect.isVerbose()) engine.send(new OkFrame());
        return session;
      } catch (Exception failedAuth) {
        ErrFrame errFrame = new ErrFrame();
        errFrame.setError(failedAuth.getMessage());
        errFrame.setCallback(() -> engine.getProtocol().close());
        engine.send(errFrame);
      }
      return null;
    });

    try {
      future.get();
    } catch (InterruptedException failedAuth) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      // We have handled it in the method
    }
  }

  private CompletableFuture<Session> createSession(SessionState engine, ConnectFrame connect) {
    SessionContextBuilder scb = new SessionContextBuilder(UuidGenerator.getInstance().generate().toString(), new ProtocolClientConnection(engine.getProtocol()));
    String username = connect.getUser();
    if (username == null) {
      scb.setUsername("anonymous").setPassword("".toCharArray());
    } else {
      scb.setUsername(username).setPassword(connect.getPass().toCharArray());
    }
    scb.setPersistentSession(false);
    int inFlight = (engine.getProtocol().getMaxReceiveSize());
    scb.setReceiveMaximum(inFlight);
    scb.setSessionExpiry(0);
    return SessionManager.getInstance().createAsync(scb.build(), engine.getProtocol());
  }
}
