/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

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
import org.maps.network.protocol.impl.stomp.frames.Connect;
import org.maps.network.protocol.impl.stomp.frames.Connected;
import org.maps.network.protocol.impl.stomp.frames.Frame;
import org.maps.network.protocol.impl.stomp.frames.HeartBeat;
import org.maps.network.protocol.impl.stomp.state.ConnectedState;
import org.maps.network.protocol.impl.stomp.state.StateEngine;
import org.maps.network.protocol.transformation.TransformationManager;

public class ConnectListener extends BaseConnectListener {


  @Override
  public void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) {
    Connect connect = (Connect) frame;

    // No version header supplied
    String versionHeader = connect.getAcceptedVersion();
    float version = processVersion(engine, versionHeader);
    if(Float.isNaN(version)){
      return; // Unable to process the versioning
    }
    HeartBeat hb = connect.getHeartBeat();
    try {

      Session session = createSession(engine, connect, hb);
      session.login();
      engine.setSession(session);
      engine.getProtocol().setTransformation(TransformationManager.getInstance().getTransformation(engine.getProtocol().getName(), session.getSecurityContext().getUsername()));
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
    } catch (Exception failedAuth) {
      handleFailedAuth(failedAuth, engine);
    }
  }

  private Session createSession( StateEngine engine, Connect connect, HeartBeat hb) throws LoginException, IOException {
    SessionContextBuilder scb = new SessionContextBuilder(UUID.randomUUID().toString(), engine.getProtocol());
    String username = connect.getLogin();
    if (username == null) {
      scb.setUsername("anonymous").setPassword("".toCharArray());
    } else {
      scb.setUsername(username).setPassword(connect.getPasscode().toCharArray());
    }
    scb.setPersistentSession(false);
    scb.setKeepAlive(hb.getPreferred());
    int inFlight = connect.getReceiveMaximum();
    if(inFlight <= 0){
      inFlight = DefaultConstants.RECEIVE_MAXIMUM;
    }
    scb.setReceiveMaximum(inFlight);
    scb.setSessionExpiry(0); // There is no idle Stomp sessions, so once disconnected the state is thrown away
    return SessionManager.getInstance().create(scb.build(), engine.getProtocol());
  }
}
