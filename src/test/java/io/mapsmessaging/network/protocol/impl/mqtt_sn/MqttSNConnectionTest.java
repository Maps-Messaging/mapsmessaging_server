/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.model.MqttsnWillData;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

public class MqttSNConnectionTest extends BaseMqttSnConfig {



  @ParameterizedTest
  @ValueSource(ints = {1,2})
  public void connectWithOutFlags(int version) throws MqttsnException, MqttsnClientConnectException {
    MqttSnClient client = new MqttSnClient("connectWithOutFlags", "localhost", 1884, version);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    delay(500);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  public void connectWithWillFlags(int version) throws MqttsnClientConnectException, MqttsnException, InterruptedException {
    TopicPath tp = new TopicPath("willTopic");
    MqttsnWillData details = new MqttsnWillData(tp, "This is my last will and stuff".getBytes(StandardCharsets.UTF_8), 1, true);
    MqttSnClient client = new MqttSnClient("connectWithFlags"+version, "localhost", 1884, version);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    client.setWillData(details);
    TimeUnit.SECONDS.sleep(2);

    client.disconnect();
    delay(500);

  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  public void connectWaitForKeepalive(int version) throws MqttsnException, MqttsnClientConnectException, InterruptedException {
    MqttSnClient client = new MqttSnClient("connectWaitForKeepalive", "localhost", 1884, version);
    client.connect(10, true);
    Assertions.assertTrue(client.isConnected());
    TimeUnit.SECONDS.sleep(15);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    delay(500);
  }
}
