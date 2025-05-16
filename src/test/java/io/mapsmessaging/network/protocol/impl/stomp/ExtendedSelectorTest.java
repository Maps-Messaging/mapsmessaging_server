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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExtendedSelectorTest extends StompBaseTest implements Listener {

  private final String[] trueSelectors = {
      "JMSTimestamp >= 0",
      "JMSTimestamp >= {time}",
      "17 not between 18 and 19",
      "true",
      "TrUe",
      "false <> true",
      "true and true",
      " true or false",
      "true = true",
      "true or dummy",
      "dummy or true",
      "'abc' <> 'ABC'",
      "'abc' = 'abc'",
      "17 Not Between 4 * 5 And 20 / 1",
      "17 between 4 * 4 and 10 + 8 ",
      "--1.0 = 1.0",
      "-1.0 = -1.0",
      "1.0 <> 2.0",
      "1.0 = 1.0",
      "true or true",
      "17 between 16 and 18",
      "1.0 = --1.0",
      "2.0 >= 1.0",
      "0.0 = 0.0",
      "1.0 < 2.0",
  };

  private final String[] falseSelectors = {
      "'it''s' = 'its'",
      "-1.0 = 1.0",
      "1.0 = -1.0",
      "true = false",
      "100 BETWEEN 5 AND 20",
  };


  private final String[][] complexDoubleSelectors = {
      {"rate = 0.2 ", "rate", "0.2"},
      {"10 / zero = 10 / zero", "zero", "0.0"},
  };

  private final String[][] complexStringSelectors = {
      {"93f = 93", "key", "value"},
      {"'abc' <> 'ABC'", "key", "value" },
      {"'abc' = 'abc'", "key", "value"},
      {"Country in ('France')","Country", "France"},
      {"$State = 'VIC' ", "$State", "VIC"},
      {"_postcode_ = '3001'", "_postcode_", "3001"},
      {" Country_name = 'France'",  "Country_name","France"},
  };


  private static final int EVENT_COUNT = 100;

  LongAdder adder = new LongAdder();

  @ParameterizedTest
  @MethodSource("testParameters")
  void testTrueSelectors(String protocol, boolean auth) throws LoginException, InterruptedException, IOException {
    for(String selector:trueSelectors){
      shouldReceiveAllEvents(protocol, auth, selector);
    }
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  void testFalseSelectors(String protocol, boolean auth) throws LoginException, InterruptedException, IOException {
    for(String selector:falseSelectors){
      shouldReceiveANoEvents(protocol, auth, selector);
    }
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  void testStringSelectors(String protocol, boolean auth) throws LoginException, InterruptedException, IOException {
    for(String[] selector:complexStringSelectors){
      shouldReceiveAllEvents(protocol, auth, selector[0], selector[1], selector[2]);
    }
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  void testDoubleSelectors(String protocol, boolean auth) throws LoginException, InterruptedException, IOException {
    for(String[] selector:complexDoubleSelectors){
      shouldReceiveAllEvents(protocol, auth, selector[0], selector[1], selector[2]);
    }
  }

  void shouldReceiveAllEvents(String protocol, boolean auth, String selector, String key, String val) throws IOException, LoginException, InterruptedException {
    adder.reset();
    Client client = getClient(protocol, auth);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    map.put("selector",selector);
    String topicName = getTopicName();
    client.subscribeW(topicName, this, map);
    for(int x=0;x<EVENT_COUNT;x++){
      String json = "{\"temperature\":28, \"count\":"+x+", \"testName\":simpleSelectorTest, \"odd\":"+x%2+"}";
      map = new HashMap<>();
      map.put(key, val);
      client.send(topicName, json, map);
    }
    WaitForState.waitFor(10, TimeUnit.SECONDS, () -> adder.sum() == EVENT_COUNT);
    client.disconnect();
    Assertions.assertEquals(EVENT_COUNT, adder.sum(), selector);
  }


  void shouldReceiveAllEvents(String protocol, boolean auth, String selector) throws IOException, LoginException, InterruptedException {
    adder.reset();
    Client client = getClient(protocol, auth);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    map.put("selector",selector);
    String topicName = getTopicName();
    client.subscribeW(topicName, this, map);
    for(int x=0;x<EVENT_COUNT;x++){
      String json = "{\"temperature\":28, \"count\":"+x+", \"testName\":simpleSelectorTest, \"odd\":"+x%2+"}";
      client.send(topicName, json);
    }
    WaitForState.waitFor(10, TimeUnit.SECONDS, () -> adder.sum() == EVENT_COUNT);
    client.disconnect();
    Assertions.assertEquals(EVENT_COUNT, adder.sum(), selector);
  }

  void shouldReceiveANoEvents(String protocol, boolean auth, String selector) throws IOException, LoginException, InterruptedException {
    adder.reset();
    Client client = getClient(protocol, auth);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    map.put("selector", selector);
    String topicName = getTopicName();
    client.subscribeW(topicName, this, map);
    for(int x=0;x<EVENT_COUNT;x++){
      String json = "{\"temperature\":28, \"count\":"+x+", \"testName\":simpleSelectorTest, \"odd\":"+x%2+"}";
      client.send(topicName, json);
    }
    WaitForState.waitFor(5, TimeUnit.SECONDS, () -> false);
    client.disconnect();
    Assertions.assertEquals(0, adder.sum());
  }

  @Override
  public void message(Map map, String s) {
    adder.add(1);
  }


}
