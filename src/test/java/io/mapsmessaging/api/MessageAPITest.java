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

package io.mapsmessaging.api;

import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.EngineManager;
import io.mapsmessaging.engine.session.FakeProtocol;
import io.mapsmessaging.engine.session.SecurityManager;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.test.BaseTestConfig;
import java.io.IOException;
import javax.security.auth.login.LoginException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class MessageAPITest extends BaseTestConfig {

  protected EngineManager engineManager;
  protected SecurityManager previous;

  @BeforeEach
  public void setupTest() throws IOException {
    if(!BaseTestConfig.md.isStarted()){
      BaseTestConfig.md.start();
    }
    engineManager = new EngineManager(BaseTestConfig.md);
    engineManager.setup();
  }

  @AfterEach
  public void resetState() {
    engineManager.reset();
  }

  public Session createSession(String name, int keepAlive, int expiry, boolean persistent, MessageListener listener) throws LoginException, IOException {
    return createSession(name, keepAlive, expiry, persistent, listener, false);
  }

  public Session createSession(String name, int keepAlive, int expiry, boolean persistent, MessageListener listener, boolean resetState) throws LoginException, IOException {
    Protocol fakeProtocol = new FakeProtocol(listener);
    SessionContextBuilder scb = new SessionContextBuilder(name, new ProtocolClientConnection(fakeProtocol));
    scb.setPersistentSession(true)
        .setPersistentSession(persistent)
        .setResetState(resetState)
        .setSessionExpiry(expiry);
    return createSession(scb, fakeProtocol);
  }

  public Session createSession(SessionContextBuilder scb, MessageListener listener) throws LoginException, IOException {
    Session session = SessionManager.getInstance().create(scb.build(), listener);
    session.login();
    session.resumeState();
    //Assertions.assertTrue(session.isRestored());
    return session;
  }

  public void close(Session session) throws IOException {
    SessionManager.getInstance().close(session, false);
  }

  public boolean hasSessions(){
    return engineManager.hasSessions();
  }

  protected void closeSubscriptionController(SubscriptionController controller) {
    engineManager.closeSubscriptionController(controller);
  }
}
