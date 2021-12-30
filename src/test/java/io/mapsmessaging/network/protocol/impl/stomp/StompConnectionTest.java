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

package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.engine.security.TestLoginModule;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.client.StompClient;

class StompConnectionTest extends StompBaseTest {


  @Test
  @Disabled("Old log implementation in the client")
  @DisplayName("Test anonymous Stomp client connection")
  void testAnonymous() throws URISyntaxException, StompException, InterruptedException, SSLException, TimeoutException {
    StompClient client = new StompClient("stomp://127.0.0.1/");
    client.connect(20000);
    Assertions.assertTrue(client.isConnected());
    client.disconnect(20000);
    Assertions.assertFalse(client.isConnected());
  }

  @Test
  @Disabled("Old log implementation in the client")
  @DisplayName("Test passing null username and password Stomp client connection")
  void testNullAuth() throws URISyntaxException, InterruptedException, SSLException {
    StompClient client = new StompClient("stomp://127.0.0.1:2001/");
    try {
      client.connect(1000);
      Assertions.assertFalse(client.isConnected());
    } catch (StompException | TimeoutException e) {
      Assertions.assertFalse(client.isConnected());
    }
  }

  @Test
  @DisplayName("Test passing valid username and password Stomp client connection")
  void testAuth() throws IOException, LoginException {
    Client client = new Client("127.0.0.1", 2001,  TestLoginModule.getUsernames()[0],  new String(TestLoginModule.getPasswords()[0]));
    WaitForState.waitFor(2, TimeUnit.SECONDS, client::isConnected);
    Assertions.assertTrue(client.isConnected());
  }

  @Test
  @DisplayName("Test passing bad username and password Stomp client connection")
  void testBadPassAuth() throws IOException {
    Client client = null;
    try {
      client = new Client("127.0.0.1", 2001,  TestLoginModule.getUsernames()[0],  new String(TestLoginModule.getPasswords()[1]));
      Assertions.assertFalse(client.isConnected());
    } catch (LoginException e) {
      Assertions.assertTrue(e.getMessage().contains("Failed to authenticate"));
    }
  }

  @Test
  @DisplayName("Test Keep Alive header")
  void testKeepAlive() throws IOException, LoginException  {
    Client client = new Client("127.0.0.1", 2001,  TestLoginModule.getUsernames()[0],  new String(TestLoginModule.getPasswords()[0]));
    WaitForState.waitFor(2, TimeUnit.SECONDS, client::isConnected);
    Assertions.assertTrue(client.isConnected());
  }
}
