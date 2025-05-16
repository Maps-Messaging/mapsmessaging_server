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

package io.mapsmessaging.network.protocol.impl.echo;

import io.mapsmessaging.test.BaseTestConfig;
import java.io.IOException;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EchoSSLConnectionTest extends BaseTestConfig  {

  @Test
  @Disabled
  @DisplayName("Connect to ECHO server over SSL")
  void connectToServer() throws IOException {
    try {
      EchoClient echoClient = new SSLEchoClient("localhost", 8444);
      echoClient.close();
    } catch (IOException e) {
      Assertions.fail("Server should be running on the port defined and we should be able to connect");
    }
  }

  @Test
  @Disabled
  @DisplayName("Connect to ECHO server over SSL to an invalid port")
  void connectToInvalidServer() throws IOException {
    try {
      EchoClient echoClient = new SSLEchoClient("localhost", 2001);
      echoClient.close();
      Assertions.fail("Should have thrown an exception");
    }catch( SSLException ssl){
      // ignore
    } catch (IOException e) {
      throw e;
    }
  }

}
