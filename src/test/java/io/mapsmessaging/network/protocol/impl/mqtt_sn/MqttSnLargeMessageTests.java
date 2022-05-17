package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnPublishReceivedListener;
import org.slj.mqtt.sn.spi.IMqttsnPublishSentListener;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

public class MqttSnLargeMessageTests extends BaseMqttSnConfig {


  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  public void subscribeWithLargeTopicName(int version) throws MqttsnException, MqttsnClientConnectException, MqttsnQueueAcceptException {
    MqttSnClient client = new MqttSnClient("connectWithOutFlags", "localhost", 1884, version);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    CountDownLatch published = new CountDownLatch(1);
    client.registerSentListener((iMqttsnContext, uuid, topicPath, i, b, bytes, iMqttsnMessage) -> published.countDown());
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
    Assertions.assertEquals(receiveCount.get(), 1);
    client.disconnect();
    delay(500);
  }


  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  public void publishWithLargeData(int version) throws MqttsnException, MqttsnClientConnectException, MqttsnQueueAcceptException {
    MqttSnClient client = new MqttSnClient("connectWithOutFlags", "localhost", 1884, version);
    client.connect(50, true);
    Assertions.assertTrue(client.isConnected());
    CountDownLatch published = new CountDownLatch(1);
    client.registerSentListener((iMqttsnContext, uuid, topicPath, i, b, bytes, iMqttsnMessage) -> published.countDown());

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
    Assertions.assertEquals(receiveCount.get(), 1);
    client.disconnect();
    delay(500);
  }

}
