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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.test.WaitForState;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class MQTTStoredMessageTest extends MQTTBaseTest {


  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS publishing")
  void connectSubscribeDisconnectPublishAndCheck(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    String topicName = getTopicName();

    String subId = UUID.randomUUID().toString().substring(0, 12).replaceAll("-", "A");
    MqttClientPersistence persistence = new MemoryPersistence();

    MqttClient subscribe = new MqttClient(getUrl(protocol, auth), subId, persistence);
    MqttConnectOptions subOption = getOptions(auth, version);
    subOption.setCleanSession(false);

    MessageListener ml = new MessageListener();

    subscribe.connectWithResult(subOption).waitForCompletion(2000);
    subscribe.subscribeWithResponse(topicName, QoS, ml).waitForCompletion(2000);
    Assertions.assertTrue(subscribe.isConnected());
    subscribe.disconnect();

    String pubId = UUID.randomUUID().toString().substring(0, 12).replaceAll("-", "A");
    MqttClient publish = new MqttClient(getUrl(protocol, auth), pubId, new MemoryPersistence());
    MqttConnectOptions pubOption = getOptions(auth, version);

    publish.connect(pubOption);
    Assertions.assertTrue(publish.isConnected());

    for(int x=0;x<10;x++) {
      MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
      message.setQos(QoS);
      message.setId(x);
      message.setRetained(false);
      publish.publish(topicName, message);
    }
    publish.disconnect();
    Assertions.assertFalse(publish.isConnected());
    publish.close();
    Assertions.assertEquals(0, ml.getCounter());

    // OK, so now we either have the events ready for us or not, depending on QoS:0 or above
    subscribe.reconnect();
    WaitForState.waitFor(2, TimeUnit.SECONDS, subscribe::isConnected);
    Assertions.assertTrue(subscribe.isConnected());
    if(QoS == 0){
      WaitForState.waitFor(2, TimeUnit.SECONDS,() -> ml.getCounter() != 0 );
      Assertions.assertEquals(0, ml.getCounter());
    }
    else {
      WaitForState.waitFor(15, TimeUnit.SECONDS,() -> ml.getCounter() == 10 );
      Assertions.assertEquals(10, ml.getCounter());
    }
    subscribe.unsubscribe(topicName);
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
