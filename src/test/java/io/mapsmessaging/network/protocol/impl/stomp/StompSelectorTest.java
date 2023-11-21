/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.test.WaitForState;
import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

class StompSelectorTest extends StompBaseTest implements Listener {

  private static final int EVENT_COUNT = 100;
  LongAdder adder = new LongAdder();

  @Test
  void simpleSelectorTest() throws IOException, LoginException, InterruptedException {
    adder.reset();
    Client client = new Client("127.0.0.1", 8675, null, null);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    map.put("selector", "odd = 0");
    String topicName = getTopicName();
    SchemaConfig config = new JsonSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    client.send("$schema/"+topicName, config.pack());
    client.subscribeW(topicName, this, map);
    for(int x=0;x<EVENT_COUNT;x++){
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("temperature", 28);
      jsonObject.put("count", x);
      jsonObject.put("testName", "simpleSelectorTest");
      jsonObject.put("odd", x % 2);
      String json = jsonObject.toString(2);
      map = new HashMap<>();
      map.put("receipt","message"+x);
      client.sendW(topicName, json, map);
    }
    WaitForState.waitFor(4, TimeUnit.SECONDS, () -> adder.sum() == EVENT_COUNT/2);
    client.disconnect();
    Assertions.assertEquals(EVENT_COUNT/2, adder.sum());
  }

  @Test
  void simpleSelectorTest2() throws IOException, LoginException, InterruptedException {
    adder.reset();
    Client client = new Client("127.0.0.1", 8675, null, null);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    map.put("selector", "odd = 0");
    SchemaConfig config = new JsonSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    String topicName = getTopicName();
    client.send("$schema/"+topicName, config.pack());
    client.subscribeW(topicName, this, map);
    for(int x=0;x<EVENT_COUNT;x++){
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("temperature", 28);
      jsonObject.put("count", x);
      jsonObject.put("testName", "simpleSelectorTest");
      jsonObject.put("odd", x % 2);
      client.send(topicName, jsonObject.toString(2));
    }
    map = new HashMap<>();
    map.put("receipt","message");

    client.sendW("flush", "This is a simple flush of the network", map);
    WaitForState.waitFor(4, TimeUnit.SECONDS, () -> adder.sum() == EVENT_COUNT/2);
    client.disconnect();
    Assertions.assertEquals(EVENT_COUNT/2, adder.sum());
  }

  @Override
  public void message(Map map, String s) {
    adder.add(1);
  }
}
