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

import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpiredEventTest extends StompBaseTest implements Listener {

  private static final int EVENT_COUNT = 10;

  private final LongAdder adder = new LongAdder();

  @ParameterizedTest
  @MethodSource("testParameters")
  @DisplayName("Test Expired Events")
  void expiryEventTest(String protocol, boolean auth) throws IOException, LoginException {
    adder.reset();
    Client client = getClient(protocol, auth);
    Map<String, String> publishMap = new HashMap<>();
    publishMap.put("expiry", "1");
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    String topicName = getTopicName();
    client.subscribe(topicName, this, map);

    for(int x=0;x<EVENT_COUNT;x++){
      String json = "{\"temperature\":28, \"count\":"+x+", \"testName\":simpleSelectorTest, \"odd\":"+x%2+"}";
      client.send(topicName, json, publishMap);
    }
    WaitForState.waitFor(4, TimeUnit.SECONDS, () -> adder.sum() == EVENT_COUNT);
    client.disconnect();
    Assertions.assertEquals(EVENT_COUNT, adder.sum());
  }

  @Override
  public void message(Map map, String s) {
    adder.increment();
  }
}
