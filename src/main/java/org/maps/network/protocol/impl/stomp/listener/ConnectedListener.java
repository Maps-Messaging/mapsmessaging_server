package org.maps.network.protocol.impl.stomp.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.UUID;
import javax.security.auth.login.LoginException;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.messaging.api.SessionManager;
import org.maps.network.protocol.impl.stomp.DefaultConstants;
import org.maps.network.protocol.impl.stomp.frames.Connected;
import org.maps.network.protocol.impl.stomp.frames.Frame;
import org.maps.network.protocol.impl.stomp.state.ClientConnectedState;
import org.maps.network.protocol.impl.stomp.state.StateEngine;
import org.maps.network.protocol.transformation.TransformationManager;

public class ConnectedListener implements FrameListener {

  private static final String CONTENT_TYPE_TEXT = "text/plain";

  private static final float MIN_VERSION = 1.0f;
  private static final float MAX_VERSION = 1.2f;

  @Override
  public void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) {
    Connected connected = (Connected) frame;

    // No version header supplied
    String versionHeader = connected.getHeader().get("version");
    if (versionHeader == null || versionHeader.length() == 0) {
      org.maps.network.protocol.impl.stomp.frames.Error error = new org.maps.network.protocol.impl.stomp.frames.Error();
      error.setContentType(CONTENT_TYPE_TEXT);
      error.setContent("No version header supplied".getBytes());
      engine.send(error);
      return;
    }

    // Check to see if we support the version
    float version = calculateVersion(versionHeader);
    if (version < 0) {
      org.maps.network.protocol.impl.stomp.frames.Error error = new org.maps.network.protocol.impl.stomp.frames.Error();
      error.setContentType(CONTENT_TYPE_TEXT);
      error.setContent(("No suitable protocol version discovered, received " + versionHeader + " : Supported = 1.1 and 1.2").getBytes());
      engine.send(error);
      return;
    }
    engine.getProtocol().setVersion(version);
    try {
      Session session = createSession(engine);
      session.login();
      engine.setSession(session);
      engine.getProtocol().setTransformation(TransformationManager.getInstance().getTransformation(engine.getProtocol().getName(), session.getSecurityContext().getUsername()));
      engine.changeState(new ClientConnectedState());
      session.resumeState();
    } catch (Exception failedAuth) {
      org.maps.network.protocol.impl.stomp.frames.Error error = new org.maps.network.protocol.impl.stomp.frames.Error();
      error.setContentType(CONTENT_TYPE_TEXT);
      error.setContent(("Failed to authenticate: " + failedAuth.getMessage()).getBytes());
      engine.send(error);
    }
  }

  private Session createSession( StateEngine engine) throws LoginException, IOException {
    SessionContextBuilder scb = new SessionContextBuilder(UUID.randomUUID().toString(), engine.getProtocol());
    scb.setPersistentSession(false);
    scb.setKeepAlive(120);
    scb.setReceiveMaximum(DefaultConstants.RECEIVE_MAXIMUM);
    scb.setSessionExpiry(0); // There is no idle Stomp sessions, so once disconnected the state is thrown away
    return SessionManager.getInstance().create(scb.build(), engine.getProtocol());
  }

  private float calculateVersion(String versionHeader){
    ArrayList<Float> versions = new ArrayList<>();
    StringTokenizer versionList = new StringTokenizer(versionHeader, ",");
    while (versionList.hasMoreElements()) {
      versions.add(Float.parseFloat(versionList.nextElement().toString().trim()));
    }
    float max = -1.0f;
    for (Float test : versions) {
      if ((test >= MIN_VERSION && test <= MAX_VERSION) && max < test) {
        max = test;
      }
    }
    return max;
  }
}
