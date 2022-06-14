/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.echo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.test.BaseTestConfig;

import java.io.IOException;

class EchoConnectionTest  extends BaseTestConfig {

  @Test
  @DisplayName("Simple connect to the server using the ECHO protocol")
  void connectToServer() {
    try {
      EchoClient echoClient = new EchoClient("localhost", 2001);
      echoClient.close();
    } catch (IOException e) {
      Assertions.fail("Test should have connected but failed with ", e);
    }
  }

  @Test
  @DisplayName("Simple connect to the server using the ECHO protocol but not a supported protocol")
  void connectToInvalidServer() throws IOException {
    EchoClient echoClient = null; // No echo configured
    try {
      echoClient = new EchoClient("localhost", 8675);
      echoClient.close();
      Assertions.fail("Should have thrown an exception");
    } catch (IOException e) {
      if(!e.getMessage().contains("Invalid response from server, protocol not supported")){
        throw e;
      }
    }
  }

  @Test
  @DisplayName("Test simple invalid passphrase")
  void checkInvalidPassphraseEndPoint() throws IOException {
    EchoClient echoClient;
    try {
      echoClient = new EchoClient("localhost", 8999);
      echoClient.close();
      Assertions.fail("Should have thrown an exception");
    } catch (IOException e) {
      if(!e.getMessage().contains("Connection refused")){
        throw e;
      }
    }
  }

}
