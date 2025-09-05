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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.model.session.impl.WillDataImpl;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

class MqttSNConnectionTest extends BaseMqttSnConfig {

  @ParameterizedTest
  @MethodSource("createVersionStream")
  void connectWithOutFlags(int version) throws MqttsnException, MqttsnClientConnectException {
    MqttSnClient client = new MqttSnClient( "localhost", 1884, version);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    delay(500);
  }

  @ParameterizedTest
  @MethodSource("createVersionStream")
  void connectWithWillFlags(int version) throws MqttsnClientConnectException, MqttsnException, InterruptedException {
    TopicPath tp = new TopicPath("willTopic");
    WillDataImpl details = new WillDataImpl(tp, "This is my last will and stuff".getBytes(StandardCharsets.UTF_8), 1, true);
    MqttSnClient client = new MqttSnClient("localhost", 1884, version);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    client.setWillData(details);
    TimeUnit.SECONDS.sleep(2);

    client.disconnect();
    delay(500);

  }

  @ParameterizedTest
  @MethodSource("createVersionStream")
  void connectWaitForKeepalive(int version) throws MqttsnException, MqttsnClientConnectException, InterruptedException {
    MqttSnClient client = new MqttSnClient("localhost", 1884, version);
    client.connect(10, true);
    long end = System.currentTimeMillis() + 20000;
    while (end > System.currentTimeMillis()) {
      TimeUnit.SECONDS.sleep(10);
      client.ping();
    }
    TimeUnit.SECONDS.sleep(1);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    delay(500);
  }
}
