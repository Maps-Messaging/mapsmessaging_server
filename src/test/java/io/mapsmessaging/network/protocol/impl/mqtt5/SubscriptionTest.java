package io.mapsmessaging.network.protocol.impl.mqtt5;

import java.util.UUID;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SubscriptionTest extends MQTTBaseTest  {


  @Test
  void testSubscription() throws MqttException {
    MqttClient client = new MqttClient("tcp://localhost:1883", UUID.randomUUID().toString(), new MemoryPersistence());
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setUserName("user1");
    options.setPassword("password1".getBytes());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    IMqttToken token = client.subscribe("/test/topic", 0, (topic, message) -> messageArrived(topic, message));
    token.waitForCompletion(2000);
    Assertions.assertTrue(token.isComplete());
    client.unsubscribe("/test/topic");
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  public void messageArrived(String topic, MqttMessage message) throws Exception {

  }
}
