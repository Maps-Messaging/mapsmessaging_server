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

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.spi.MqttsnException;

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
  public void connectWithFlags(int version) throws MqttsnClientConnectException, MqttsnException {
    MqttSnClient client = new MqttSnClient("connectWithFlags", "localhost", 1884, version);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    delay(500);

//    client.connect("simpleConnection", true, (short)50, "willTopic", 0, "This is my last will and stuff", false);

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
