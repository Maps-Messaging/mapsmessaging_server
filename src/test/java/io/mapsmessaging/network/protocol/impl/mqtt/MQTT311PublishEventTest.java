package io.mapsmessaging.network.protocol.impl.mqtt;

public class MQTT311PublishEventTest extends MQTTPublishEventTest {

  @Override
  int getVersion() {
    return MQTT_3_1_1;
  }
}