/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.api;

import io.mapsmessaging.engine.session.EngineManager;
import io.mapsmessaging.engine.session.FakeProtocolImpl;
import io.mapsmessaging.engine.session.SecurityManager;
import java.io.IOException;
import javax.security.auth.login.LoginException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.test.BaseTestConfig;

public class MessageAPITest extends BaseTestConfig {

  protected EngineManager engineManager;
  protected SecurityManager previous;

  @BeforeEach
  public void setupTest() {
    if(!BaseTestConfig.md.isStarted()){
      BaseTestConfig.md.start(null);
    }
    engineManager = new EngineManager(BaseTestConfig.md);
    engineManager.setup();
  }

  @AfterEach
  public void resetState() {
    engineManager.reset();
  }

  public Session createSession(String name, int keepAlive, int expiry, boolean persistent, MessageListener listener) throws LoginException, IOException {
    ProtocolImpl fakeProtocol = new FakeProtocolImpl(listener);
    SessionContextBuilder scb = new SessionContextBuilder(name, fakeProtocol);
    scb.setPersistentSession(true)
        .setKeepAlive(keepAlive)
        .setPersistentSession(persistent)
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
