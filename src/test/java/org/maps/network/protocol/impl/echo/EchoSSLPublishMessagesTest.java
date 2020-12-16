/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.echo;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.maps.test.BaseTestConfig;

public class EchoSSLPublishMessagesTest extends BaseTestConfig {

  @Test
  @DisplayName("Connect to ECHO server over SSL")
  public void testEchoMessages() throws IOException {
    EchoClient echoClient = new SSLEchoClient("localhost", 8444);
    for(int x=0;x<100;x++) {
      String sentMsg = "This should be returned to me";
      echoClient.send(sentMsg);
      String receivedMsg = echoClient.read();
      Assertions.assertEquals(sentMsg, receivedMsg);
    }
    echoClient.close();
  }

}
