package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.test.BaseTestConfig;

public class MQTTBaseTest extends BaseTestConfig {

  public static final int MQTT_5_0 = 5;

  String getClientId(String proposed, int version) {
    return proposed;
  }
}
