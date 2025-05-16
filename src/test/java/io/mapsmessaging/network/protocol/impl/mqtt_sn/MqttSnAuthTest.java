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

import io.mapsmessaging.network.protocol.impl.mqtt5.ClientCallbackHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.security.sasl.Sasl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.spi.SaslAuthHandler;
import org.slj.mqtt.sn.model.IAuthHandler;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnPublishReceivedListener;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

class MqttSnAuthTest extends BaseMqttSnConfig {

  @Test
  void simpleAuthValidation() throws MqttsnException, MqttsnClientConnectException, IOException, MqttsnQueueAcceptException {
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String mechanisms = "SCRAM-SHA-512";
    ClientCallbackHandler clientHandler = new ClientCallbackHandler("admin", getPassword("admin"), "servername");

    IAuthHandler authHandler = new SaslAuthHandler(mechanisms, "Authorized", "localhost", props, clientHandler);
    MqttSnClient client = new MqttSnClient( "localhost", 1887, 2, authHandler);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    AtomicBoolean receivedEvent = new AtomicBoolean(false);
    client.registerPublishListener(new IMqttsnPublishReceivedListener() {
      @Override
      public void receive(IClientIdentifierContext iMqttsnContext, TopicPath topicPath, int i, boolean b, byte[] bytes, IMqttsnMessage iMqttsnMessage) {
        receivedEvent.set(true);
        System.err.println("Received event");
      }
    });
    client.subscribe("/topic", 0);
    client.publish("/topic", 2, "This is a message".getBytes());
    long timeout = System.currentTimeMillis()+10000;
    while(!receivedEvent.get() && timeout > System.currentTimeMillis()){
      delay(10);
    }
    Assertions.assertTrue(receivedEvent.get(), "Should have received the event");
    client.disconnect();
    delay(500);
  }
}
