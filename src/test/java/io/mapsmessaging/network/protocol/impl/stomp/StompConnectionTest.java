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

package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.engine.security.TestLoginModule;
import java.io.IOException;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class StompConnectionTest extends StompBaseTest {


  @ParameterizedTest
  @MethodSource("testParameters")
  @DisplayName("Test Stomp client connection")
  void testAnonymous(String protocol, boolean auth) throws IOException, LoginException {
    Client client = getClient(protocol, auth);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
  }


  @Test
  @DisplayName("Test passing bad username and password Stomp client connection")
  void testBadPassAuth() throws IOException {
    Client client = null;
    try {
      client = new Client("127.0.0.1", 8675, TestLoginModule.getUsernames()[0], new String(TestLoginModule.getPasswords()[1]));
      Assertions.assertFalse(client.isConnected());
    } catch (LoginException e) {
      Assertions.assertTrue(e.getMessage().contains("Failed to authenticate"));
    }
  }
}
