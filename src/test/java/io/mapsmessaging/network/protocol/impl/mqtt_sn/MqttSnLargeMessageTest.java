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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.spi.MqttsnException;

class MqttSnLargeMessageTest extends BaseMqttSnConfig {


  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void subscribeWithLargeTopicName(int version) throws MqttsnException, MqttsnClientConnectException, MqttsnQueueAcceptException {
    MqttSnClient client = new MqttSnClient("localhost", 1884, version);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    CountDownLatch published = new CountDownLatch(1);
    client.registerSentListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> published.countDown());
    AtomicLong receiveCount = new AtomicLong(0);
    client.registerPublishListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> receiveCount.incrementAndGet());

    String topicName = "";
    while(topicName.length() < 512){
      topicName += topicName+"/folder";
    }
    topicName += "/largeTopicName";
    client.subscribe(topicName, 1);
    client.publish(topicName, 1, "msg".getBytes(StandardCharsets.UTF_8));

    long timeout = System.currentTimeMillis() + 10000;
    while(receiveCount.get() == 0 && timeout > System.currentTimeMillis()){
      delay(1);
    }
    Assertions.assertEquals(1,receiveCount.get());
    client.disconnect();
    delay(500);
  }


  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void publishWithLargeData(int version) throws MqttsnException, MqttsnClientConnectException, MqttsnQueueAcceptException {
    MqttSnClient client = new MqttSnClient("localhost", 1884, version);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    CountDownLatch published = new CountDownLatch(1);
    client.registerSentListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> published.countDown());

    AtomicLong receiveCount = new AtomicLong(0);
    client.registerPublishListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> {
      if(bytes.length == 512) receiveCount.incrementAndGet();
    });

    String topicName = "/largeMessageTest";
    byte[] msg = new byte[512];
    client.subscribe(topicName, 1);
    client.publish(topicName, 1, msg);

    long timeout = System.currentTimeMillis() + 10000;
    while(receiveCount.get() == 0 && timeout > System.currentTimeMillis()){
      delay(1);
    }
    Assertions.assertEquals(1, receiveCount.get());
    client.disconnect();
    delay(500);
  }

}
