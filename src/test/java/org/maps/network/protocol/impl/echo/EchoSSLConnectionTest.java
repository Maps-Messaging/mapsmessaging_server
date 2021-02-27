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

package org.maps.network.protocol.impl.echo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.maps.test.BaseTestConfig;

import javax.net.ssl.SSLException;
import java.io.IOException;

class EchoSSLConnectionTest extends BaseTestConfig  {

  @Test
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
