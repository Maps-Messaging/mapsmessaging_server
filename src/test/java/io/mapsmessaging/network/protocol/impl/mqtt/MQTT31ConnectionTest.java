package io.mapsmessaging.network.protocol.impl.mqtt;

public class MQTT31ConnectionTest extends MQTTConnectionTest {

  @Override
  protected int getVersion() {
    return MQTT_3_1;
  }
}
