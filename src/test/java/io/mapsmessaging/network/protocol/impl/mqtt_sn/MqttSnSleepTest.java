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

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.BaseMqttSnConfig.createQoSVersionStream;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.Configuration.PUBLISH_COUNT;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.Configuration.TIMEOUT;

import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.spi.MqttsnException;

class MqttSnSleepTest extends BaseTestConfig {

  private static Stream<Arguments> sleepingClientTest() {
    return createQoSVersionStream();
  }
  @ParameterizedTest
  @MethodSource
  void sleepingClientTest(int qos, int version) throws IOException, MqttsnException, MqttsnClientConnectException, MqttsnQueueAcceptException, InterruptedException {
    int expectedCount = qos != 0 ? PUBLISH_COUNT : 1;

    AtomicLong publishCount = new AtomicLong(0);
    AtomicLong receiveCounter = new AtomicLong(0);

    MqttSnClient sleepy = new MqttSnClient( "localhost", 1884, version);
    MqttSnClient hyper = new MqttSnClient("localhost", 1884, version);

    hyper.connect(120, true);
    sleepy.connect(120, true);

    hyper.registerSentListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> publishCount.incrementAndGet());
    sleepy.registerPublishListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> receiveCounter.incrementAndGet());
    sleepy.subscribe("/mqttsn/test", qos);

    sleepy.sleep(120);// We sleep for 30 seconds
    for(int x=0;x<PUBLISH_COUNT;x++) {
      hyper.publish("/mqttsn/test",qos, "These should be waiting for sleepy".getBytes());
    }
    long timer = System.currentTimeMillis() + 30000;
    while(publishCount.get() != PUBLISH_COUNT && timer > System.currentTimeMillis()){
      TimeUnit.MILLISECONDS.sleep(10);
    }
    //
    // At this point sleepy should have 0 publish events
    //
    // Ok, so sleepy seems to be sleeping, lets wait a couple of seconds to be sure
    Assertions.assertEquals(receiveCounter.get(), 0);
    delay(2000);
    Assertions.assertEquals(receiveCounter.get(), 0);

    // Ok, so sleepy seems to be in a coma, cool
    sleepy.wake();

    // We should ask the server to send any outstanding events and then go back to sleep
    WaitForState.waitFor(TIMEOUT, TimeUnit.MILLISECONDS, ()->(receiveCounter.get() == expectedCount));
    Assertions.assertEquals(expectedCount, receiveCounter.get());

    //
    // So far so good, but is sleepy asleep again
    //
    receiveCounter.set(0);
    publishCount.set(0);
    for(int x=0;x<PUBLISH_COUNT;x++) {
      hyper.publish("/mqttsn/test", 1, "These should be waiting for sleepy".getBytes());
    }

    timer = System.currentTimeMillis() + 30000;
    while(publishCount.get() != PUBLISH_COUNT && timer > System.currentTimeMillis()){
      TimeUnit.MILLISECONDS.sleep(10);
    }
    //
    // At this point sleepy should have 0 publish events
    //
    Assertions.assertEquals(receiveCounter.get(), 0);

    // Ok, so sleepy seems to be sleeping, lets wait a couple of seconds to be sure
    // Brilliant, not only did sleepy wake up, receive events but went back to sleep...
    // final task, lets wake up, and stay awake...
    sleepy.connect(120, false);
    WaitForState.waitFor(TIMEOUT, TimeUnit.MILLISECONDS, ()->(receiveCounter.get() == expectedCount));

    //
    // Final test, if I publish then sleepy should receive directly
    //
    receiveCounter.set(0);
    for(int x=0;x<PUBLISH_COUNT;x++) {
      hyper.publish("/mqttsn/test", 1, "These should be waiting for sleepy".getBytes());
    }

    timer = System.currentTimeMillis() + 30000;
    while(publishCount.get() != PUBLISH_COUNT && timer > System.currentTimeMillis()){
      TimeUnit.MILLISECONDS.sleep(10);
    }

    WaitForState.waitFor(TIMEOUT, TimeUnit.MILLISECONDS, ()->(receiveCounter.get() == PUBLISH_COUNT));
    hyper.disconnect();
    sleepy.disconnect();
  }

  private static Stream<Arguments> expiredEventTest() {
    return createQoSVersionStream();
  }
  @ParameterizedTest
  @MethodSource
  void expiredEventTest(int qos, int version) throws MqttsnException, MqttsnClientConnectException, MqttsnQueueAcceptException, InterruptedException, IOException {
    AtomicLong publishCount = new AtomicLong(0);
    AtomicLong receiveCounter = new AtomicLong(0);

    MqttSnClient sleepy = new MqttSnClient("localhost", 1884, version);
    MqttSnClient hyper = new MqttSnClient("localhost", 1884, version);

    hyper.connect(120, true);
    sleepy.connect(120, true);

    hyper.registerSentListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> publishCount.incrementAndGet());
    sleepy.registerPublishListener((iMqttsnContext, topicPath, i, b, bytes, iMqttsnMessage) -> receiveCounter.incrementAndGet());
    sleepy.subscribe("/mqttsn/test", qos);

    sleepy.sleep(120);// We sleep for 120 seconds

    for (int x = 0; x < PUBLISH_COUNT; x++) {
      hyper.publish("/mqttsn/test", qos, "These should be waiting for sleepy".getBytes());
    }
    long timer = System.currentTimeMillis() + 30000;
    while(publishCount.get() != PUBLISH_COUNT && timer > System.currentTimeMillis()){
      TimeUnit.MILLISECONDS.sleep(10);
    }
    //
    // At this point sleepy should have 0 publish events
    //
    // Ok, so sleepy seems to be sleeping, lets wait a couple of seconds to be sure
    Assertions.assertEquals(receiveCounter.get(), 0);
    delay(20000); // Time out events
    Assertions.assertEquals(receiveCounter.get(), 0);

    sleepy.wake();

    // We should ask the server to send any outstanding events and then go back to sleep
    WaitForState.waitFor(TIMEOUT, TimeUnit.MILLISECONDS, ()->(receiveCounter.get() == 0));
    Assertions.assertEquals(0, receiveCounter.get());
    sleepy.connect(120, false);
    sleepy.disconnect();
    hyper.disconnect();

  }

}
