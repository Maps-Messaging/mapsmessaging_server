/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 * https://commonsclause.com/
 */
package io.mapsmessaging.network.protocol.impl.mqtt5;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MQTT5RepeatedClientIdIT extends MQTTBaseTest {

  @Test
  void repeatedCleanStartWithSameClientIdSurvivesAsynchronousClosure() throws Exception {
    String clientId = "same-client-" + UUID.randomUUID();
    String topic = "regression/same-client/" + clientId;
    String url = getUrl("tcp", false);

    for (int iteration = 0; iteration < 10; iteration++) {
      MqttClient client = new MqttClient(url, clientId, new MemoryPersistence());
      try {
        MqttConnectionOptions options = getOptions(false);
        options.setCleanStart(true);
        client.connect(options);
        assertTrue(client.isConnected(), "CONNECT iteration " + iteration);
        client.subscribe(topic, 1);
        client.publish(topic, ("iteration-" + iteration).getBytes(StandardCharsets.UTF_8), 1, false);
        client.disconnect();
      } catch (Exception failure) {
        fail("MQTT 5 same-client-ID reconnect failed at iteration " + iteration + " (clientId=" + clientId + ")", failure);
      } finally {
        if (client.isConnected()) {
          try {
            client.disconnect();
          } catch (Exception ignored) {
            // Keep the original failure if the peer has already closed.
          }
        }
        client.close();
      }
    }
  }
}
