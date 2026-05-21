/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

  @BeforeEach
  void setupTest() throws IOException {
    if(!BaseTestConfig.md.isStarted()){
      BaseTestConfig.md.start();
    }
    engineManager = new EngineManager(BaseTestConfig.md);
    engineManager.setup();
  }

  @AfterEach
  void resetState() {
    engineManager.reset();
  }

  public boolean hasSessions(){
    return engineManager.hasSessions();
  }

  protected void closeSubscriptionController(SubscriptionController controller) {
    engineManager.closeSubscriptionController(controller);
  }
}
