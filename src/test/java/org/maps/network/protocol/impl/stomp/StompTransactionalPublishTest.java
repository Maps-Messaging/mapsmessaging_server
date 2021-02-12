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

package org.maps.network.protocol.impl.stomp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.StompMessages;

public class StompTransactionalPublishTest extends StompBaseTest {

  @Test
  @DisplayName("Test transactional publishing with commit")
  public void testPublishTransactionalWithCommit() throws IOException, LoginException, InterruptedException {
    Client client = new Client("127.0.0.1", 8675, null, null);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    String topicName = getTopicName();
 //   client.subscribe(topicName, map);
    byte[] buffer = new byte[10];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x % 90) + 32);
    }
    String tmp = new String(buffer);

    for(int y=0;y<100;y++) {
      String transaction = "tx"+y;
      Map<String, String> map2 = new HashMap<>();
      map2.put("transaction", transaction);
      client.begin(map2);
      for (int x = 0; x < 10; x++) {
        StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
        Map<String, String> map1 = new HashMap<>();
        map1.put("packet_id", "" + x);
        map1.put("transaction", transaction);
        msg.getHeaders().put("packet_id", "" + x);
        client.send(topicName, tmp, map1);
      }
      client.commitW(map2);
    }
    client.disconnect();
  }

  @Test
  @DisplayName("Test transactional publishing with abort")
  public void testPublishTransactionalWithAbort() throws IOException, LoginException, InterruptedException {
    Client client = new Client("127.0.0.1", 8675, null, null);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    String topicName = getTopicName();

    client.subscribe(topicName, map);
    byte[] buffer = new byte[10];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x % 90) + 32);
    }
    String tmp = new String(buffer);

    for(int y=0;y<100;y++) {
      String transaction = "tx"+y;
      Map<String, String> map2 = new HashMap<>();
      map2.put("transaction", transaction);
      client.begin(map2);
      for (int x = 0; x < 10; x++) {
        StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
        Map<String, String> map1 = new HashMap<>();
        map1.put("packet_id", "" + x);
        map1.put("transaction", transaction);
        msg.getHeaders().put("packet_id", "" + x);
        client.send(topicName, tmp, map1);
      }
      client.abort(map2);
    }
    client.disconnect();
  }
}
