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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MQTTStoredMessageTest extends BaseTestConfig {


  @Test
  void testQos0() throws MqttException, IOException {
    connectSubscribeDisconnectPublishAndCheck(0);
  }

  @Test
  void testQos1() throws MqttException, IOException {
    connectSubscribeDisconnectPublishAndCheck(1);
  }

  @Test
  void testQos2() throws MqttException, IOException {
    connectSubscribeDisconnectPublishAndCheck(2);
  }

  void connectSubscribeDisconnectPublishAndCheck(int QoS) throws MqttException, IOException {

    String subId = UUID.randomUUID().toString();
    MqttClient subscribe = new MqttClient("tcp://localhost:2001", subId, new MemoryPersistence());
    MessageListener ml = new MessageListener();
    MqttConnectOptions subOption = new MqttConnectOptions();
    subOption.setUserName("user1");
    subOption.setPassword("password1".toCharArray());
    subOption.setCleanSession(false);

    subscribe.connectWithResult(subOption).waitForCompletion(2000);
    subscribe.subscribeWithResponse("/topic/test", QoS, ml).waitForCompletion(2000);
    Assertions.assertTrue(subscribe.isConnected());
    subscribe.disconnect();
    subscribe.close();

    MqttClient publish = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
    MqttConnectOptions pubOption = new MqttConnectOptions();
    pubOption.setUserName("user1");
    pubOption.setPassword("password1".toCharArray());
    publish.connect(pubOption);
    Assertions.assertTrue(publish.isConnected());

    for(int x=0;x<10;x++) {
      MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
      message.setQos(QoS);
      message.setId(x);
      message.setRetained(false);
      publish.publish("/topic/test", message);
    }
    publish.disconnect();
    Assertions.assertFalse(publish.isConnected());
    publish.close();
    Assertions.assertEquals(0, ml.getCounter());

    // OK, so now we either have the events ready for us or not, depending on QoS:0 or above
    subOption = new MqttConnectOptions();
    subOption.setUserName("user1");
    subOption.setPassword("password1".toCharArray());
    subOption.setCleanSession(false);
    subscribe = new MqttClient("tcp://localhost:2001", subId, new MemoryPersistence());
    subscribe.connect(subOption);
    subscribe.subscribe("/topic/test", QoS, ml);
    Assertions.assertTrue(subscribe.isConnected());
    if(QoS == 0){
      WaitForState.waitFor(2, TimeUnit.SECONDS,() -> ml.getCounter() != 0 );
      Assertions.assertEquals(0, ml.getCounter());
    }
    else {
      WaitForState.waitFor(15, TimeUnit.SECONDS,() -> ml.getCounter() == 10 );
      Assertions.assertEquals(10, ml.getCounter());
    }
    subscribe.unsubscribe("/topic/test");
    subscribe.disconnect();
    subscribe.close();
  }

  public static class MessageListener implements IMqttMessageListener{

    private final AtomicLong counter;
    public MessageListener(){
      counter = new AtomicLong(0);
    }

    public long getCounter(){
      return counter.get();
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
      counter.incrementAndGet();
    }
  }

}
