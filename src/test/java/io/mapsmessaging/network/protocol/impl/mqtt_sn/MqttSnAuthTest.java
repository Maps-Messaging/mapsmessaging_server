package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.network.protocol.impl.mqtt5.ClientCallbackHandler;
import java.util.HashMap;
import java.util.Map;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.spi.MqttsnSaslAuthHandler;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.spi.IMqttsnAuthHandler;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnPublishReceivedListener;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

class MqttSnAuthTest extends BaseMqttSnConfig {

  @Test
  void simpleAuthValidation() throws MqttsnException, MqttsnClientConnectException, SaslException, MqttsnQueueAcceptException {
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String mechanisms = "SCRAM-BCRYPT-SHA-512";
    ClientCallbackHandler clientHandler = new ClientCallbackHandler("test3", "This is an bcrypt password", "servername");

    IMqttsnAuthHandler authHandler = new MqttsnSaslAuthHandler(mechanisms, "Authorized", "localhost", props, clientHandler);
    MqttSnClient client = new MqttSnClient("connectWithOutFlags", "localhost", 1887, 2, authHandler);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    client.registerPublishListener(new IMqttsnPublishReceivedListener() {
      @Override
      public void receive(IMqttsnContext iMqttsnContext, TopicPath topicPath, int i, boolean b, byte[] bytes, IMqttsnMessage iMqttsnMessage) {
        System.err.println("Received event");
      }
    });
    client.subscribe("/topic", 0);
    client.publish("/topic", 2, "This is a message".getBytes());
    delay(100);
    client.disconnect();
    delay(500);
  }
}
