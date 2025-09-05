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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.security.uuid.UuidGenerator;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MQTTStoredMessageTest extends MQTTBaseTest {

  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS wildcard subscription")
  void connectSubscribeDisconnectPublishAndCheck(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    String subId = UuidGenerator.getInstance().generate().toString();
    MqttClientPersistence persistence = new MemoryPersistence();
    MqttClient subscribe = new MqttClient(getUrl(protocol, auth), subId, persistence);
    MqttConnectionOptions subOption = getOptions(auth);
    subOption.setCleanStart(false);

    MessageListener ml = new MessageListener();

    subscribe.connectWithResult(subOption).waitForCompletion(2000);
    MqttSubscription[] subscriptions = new MqttSubscription[1];
    IMqttMessageListener[] listeners = new IMqttMessageListener[1];
    subscriptions[0] = new MqttSubscription("/topic/test", QoS);
    listeners[0] = ml;
    subscribe.setCallback(new MqttCallback() {
      @Override
      public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

      }

      @Override
      public void mqttErrorOccurred(MqttException e) {

      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        listeners[0].messageArrived(s, mqttMessage);
      }

      @Override
      public void deliveryComplete(IMqttToken iMqttToken) {

      }

      @Override
      public void connectComplete(boolean b, String s) {

      }

      @Override
      public void authPacketArrived(int i, MqttProperties mqttProperties) {

      }
    });
    subscribe.subscribe(subscriptions, listeners);
    Assertions.assertTrue(subscribe.isConnected());
    subscribe.disconnect();

    MqttClient publish = new MqttClient(getUrl(protocol, auth), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    MqttConnectionOptions pubOption = getOptions(auth);

    publish.connect(pubOption);
    Assertions.assertTrue(publish.isConnected());

    for (int x = 0; x < 10; x++) {
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
    subscribe.reconnect();
    WaitForState.waitFor(2, TimeUnit.SECONDS, subscribe::isConnected);
    Assertions.assertTrue(subscribe.isConnected());
    if (QoS == 0) {
      WaitForState.waitFor(2, TimeUnit.SECONDS, () -> ml.getCounter() != 0);
      Assertions.assertEquals(0, ml.getCounter());
    } else {
      WaitForState.waitFor(15, TimeUnit.SECONDS, () -> ml.getCounter() == 10);
      Assertions.assertEquals(10, ml.getCounter());
    }
    subscribe.unsubscribe("/topic/test");
    subscribe.disconnect();
    subscribe.close();
  }

  public static class MessageListener implements IMqttMessageListener {

    private final AtomicLong counter;

    public MessageListener() {
      counter = new AtomicLong(0);
    }

    public long getCounter() {
      return counter.get();
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
      counter.incrementAndGet();
    }
  }

}
