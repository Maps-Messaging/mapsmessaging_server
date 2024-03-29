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

class EchoPublishMessagesTest extends BaseTestConfig {

  @Test
  @DisplayName("Test simple echo messages")
  void testEchoMessages() throws IOException {
    EchoClient echoClient = new EchoClient("localhost", 2001);
    for(int x=0;x<100;x++) {
      String sentMsg = "This should be returned to me";
      echoClient.send(sentMsg);
      String receivedMsg = echoClient.read();
      Assertions.assertEquals(sentMsg, receivedMsg);
    }
    echoClient.close();
  }

}
