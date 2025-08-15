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

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Connect;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Connected;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.HeartBeat;
import io.mapsmessaging.network.protocol.impl.stomp.state.ConnectedState;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.security.uuid.UuidGenerator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ConnectListener extends BaseConnectListener {

  @Override
  public void frameEvent(Frame frame, SessionState engine, boolean endOfBuffer) {
    Connect connect = (Connect) frame;

    // No version header supplied
    String versionHeader = connect.getAcceptedVersion();
    float version = processVersion(engine, versionHeader);
    if (Float.isNaN(version)) {
      return; // Unable to process the versioning
    }
    HeartBeat hb = connect.getHeartBeat();
    CompletableFuture<Session> future = createSession(engine, connect, hb).thenApply(session -> {
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
        Connected connected = new Connected();
        connected.setServer("MESSAGING/STOMP");
        connected.setVersion("" + version);
        connected.setSession(session.getName());
        if (hb.getPreferred() != 0) {
          connected.setHeartBeat(new HeartBeat(hb.getPreferred(), hb.getPreferred()));
        }
        engine.send(connected);
        engine.changeState(new ConnectedState());
        session.resumeState();
        return session;
      } catch (Exception failedAuth) {
        handleFailedAuth(failedAuth, engine);
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

  private CompletableFuture<Session> createSession(SessionState engine, Connect connect, HeartBeat hb) {
    SessionContextBuilder scb = new SessionContextBuilder(UuidGenerator.getInstance().generate().toString(), new ProtocolClientConnection(engine.getProtocol()));
    String username = connect.getLogin();
    if (username == null) {
      scb.setUsername("anonymous").setPassword("".toCharArray());
    } else {
      scb.setUsername(username).setPassword(connect.getPasscode().toCharArray());
    }
    scb.setPersistentSession(false);

    int inFlight = connect.getReceiveMaximum();
    if (inFlight <= 0) {
      inFlight =  (engine.getProtocol().getMaxReceiveSize());
    }
    scb.setReceiveMaximum(inFlight);
    scb.setSessionExpiry(0); // There is no idle Stomp sessions, so once disconnected the state is thrown away
    return SessionManager.getInstance().createAsync(scb.build(), engine.getProtocol());
  }
}
