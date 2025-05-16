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

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.Configuration.PUBLISH_COUNT;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.Configuration.TIMEOUT;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.spi.MqttsnException;

class MqttSNPublishingTest extends BaseMqttSnConfig {


  @ParameterizedTest
  @MethodSource
  void  subscribeTopicAndPublish(int qos, int version) throws InterruptedException, MqttsnException, MqttsnClientConnectException, MqttsnQueueAcceptException {
    subscribeTopicAndPublishQoSn(qos, version);
  }

  private static Stream<Arguments> subscribeTopicAndPublish() {
    return createQoSVersionStream();
  }


  void subscribeTopicAndPublishQoSn(int qos, int version) throws InterruptedException, MqttsnException, MqttsnClientConnectException, MqttsnQueueAcceptException {
    CountDownLatch published = new CountDownLatch(PUBLISH_COUNT);
    CountDownLatch received = new CountDownLatch(PUBLISH_COUNT);

    MqttSnClient client = new MqttSnClient( "localhost", 1884, version);
    client.connect(180, true);

    client.registerPublishListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> received.countDown());

    client.registerSentListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> published.countDown());

    //
    // Test Registered Topics
    //
    client.subscribe("mqttsn/test", qos);
    long count = published.getCount();
    for(int x=0;x<PUBLISH_COUNT;x++){
      client.publish("mqttsn/test", qos, "Hi There MQTT-SN test".getBytes());
      // At QoS == 0 there is NO response from the gateway, its a blind publish
      if(qos != 0) {
        long timeout = System.currentTimeMillis() + TIMEOUT;
        while (count == published.getCount()) {
          delay(100);
          Assertions.assertFalse(timeout < System.currentTimeMillis());
        }
      }
    }
    Assertions.assertTrue(received.await(30, TimeUnit.SECONDS));
    delay(1000);
    client.disconnect();
  }
}
