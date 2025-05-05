package io.mapsmessaging.network.protocol.impl.nats.listener;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.nats.frames.ConnectFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.OkFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.ConnectedState;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
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
        engine.getProtocol().setTransformation(transformation);
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
