/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.stomp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExtendedSelectorTest extends StompBaseTest implements Listener {

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

  @Test
  void testTrueSelectors() throws LoginException, InterruptedException, IOException {
    for(String selector:trueSelectors){
      shouldReceiveAllEvents(selector);
    }
  }

  @Test
  void testFalseSelectors() throws LoginException, InterruptedException, IOException {
    for(String selector:falseSelectors){
      shouldReceiveANoEvents(selector);
    }
  }

  @Test
  void testStringSelectors() throws LoginException, InterruptedException, IOException {
    for(String[] selector:complexStringSelectors){
      shouldReceiveAllEvents(selector[0], selector[1], selector[2]);
    }
  }

  @Test
  void testDoubleSelectors() throws LoginException, InterruptedException, IOException {
    for(String[] selector:complexDoubleSelectors){
      shouldReceiveAllEvents(selector[0], selector[1], selector[2]);
    }
  }

  void shouldReceiveAllEvents(String selector, String key, String val) throws IOException, LoginException, InterruptedException {
    adder.reset();
    Client client = new Client("127.0.0.1", 8675, null, null);
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
    int counter =0;
    while(adder.sum() != EVENT_COUNT && counter < 3) {
      LockSupport.parkNanos(100000000);
      counter++;
    }
    client.disconnect();
    Assertions.assertEquals(EVENT_COUNT, adder.sum(), selector);
  }



  void shouldReceiveAllEvents(String selector) throws IOException, LoginException, InterruptedException {
    adder.reset();
    Client client = new Client("127.0.0.1", 8675, null, null);
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
    long end = System.currentTimeMillis() + 10000;
    while(adder.sum() != EVENT_COUNT && end > System.currentTimeMillis()) {
      LockSupport.parkNanos(100000000);
    }
    client.disconnect();
    Assertions.assertEquals(EVENT_COUNT, adder.sum(), selector);
  }

  void shouldReceiveANoEvents(String selector) throws IOException, LoginException, InterruptedException {
    adder.reset();
    Client client = new Client("127.0.0.1", 8675, null, null);
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
    LockSupport.parkNanos(500000000);
    client.disconnect();
    Assertions.assertEquals(0, adder.sum());
  }

  @Override
  public void message(Map map, String s) {
    adder.add(1);
  }



}
