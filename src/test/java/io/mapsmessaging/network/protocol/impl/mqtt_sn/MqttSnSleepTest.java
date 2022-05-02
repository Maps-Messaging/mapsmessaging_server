/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.Configuration.PUBLISH_COUNT;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.Configuration.TIMEOUT;

import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.spi.MqttsnException;

public class MqttSnSleepTest extends BaseTestConfig {

  private int connectionCounter = 0;


  @ParameterizedTest
  @ValueSource(ints = {1,2})
  public void sleepingClientTest(int version) throws IOException, MqttsnException, MqttsnClientConnectException, MqttsnQueueAcceptException, InterruptedException {
    AtomicLong publishCount = new AtomicLong(0);

    MqttSnClient client = new MqttSnClient("sleepingClientTest", "localhost", 1884, version);
    client.connect(120, true);

    MqttSnClient hyper = new MqttSnClient("sleepingClientTest-hyper", "localhost", 1884, version);
    hyper.connect(120, true);
    hyper.registerSentListener((iMqttsnContext, uuid, topicPath, bytes, iMqttsnMessage) -> publishCount.incrementAndGet());

    AtomicLong receiveCounter = new AtomicLong(0);
    client.registerPublishListener((iMqttsnContext, topicPath, bytes, iMqttsnMessage) -> receiveCounter.incrementAndGet());
    client.subscribe("/mqttsn/test", 1);


    delay(1000);
    client.sleep(120);// We sleep for 30 seconds
    for(int x=0;x<PUBLISH_COUNT;x++) {
      hyper.publish("/mqttsn/test",1, "These should be waiting for sleepy".getBytes());
    }
    long timer = System.currentTimeMillis() + 30000;
    while(publishCount.get() != PUBLISH_COUNT && timer > System.currentTimeMillis()){
      TimeUnit.MILLISECONDS.sleep(10);
    }
    //
    // At this point sleepy should have 0 publish events
    //
    Assertions.assertEquals(receiveCounter.get(), 0);

    // Ok, so sleepy seems to be sleeping, lets wait a couple of seconds to be sure
    delay(2000);
    Assertions.assertEquals(receiveCounter.get(), 0);

    // Ok, so sleepy seems to be in a coma, cool
    client.wake();

    // We should ask the server to send any outstanding events and then go back to sleep
    WaitForState.waitFor(TIMEOUT, TimeUnit.MILLISECONDS, ()->(receiveCounter.get() == PUBLISH_COUNT));
    Assertions.assertEquals(PUBLISH_COUNT, receiveCounter.get());

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
    delay(2000);
    Assertions.assertEquals(receiveCounter.get(), 0);

    // Brilliant, not only did sleepy wake up, receive events but went back to sleep...
    // final task, lets wake up, and stay awake...
    client.wake();

    WaitForState.waitFor(TIMEOUT, TimeUnit.MILLISECONDS, ()->(receiveCounter.get() == PUBLISH_COUNT));

    Assertions.assertEquals(receiveCounter.get(), PUBLISH_COUNT);

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
    client.disconnect();
  }
}
