/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 */
package io.mapsmessaging.network.io.impl.memory;

import io.mapsmessaging.memory.MemoryConnectionConfig;
import io.mapsmessaging.memory.examples.paho.MemoryNetworkConfig;
import io.mapsmessaging.test.BaseTestConfig;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PahoOverShmIntegrationTest extends BaseTestConfig {

  private static final InetSocketAddress ENDPOINT = new InetSocketAddress("127.0.0.1", 1889);

  @Test
  void mqtt311PublishesAndSubscribesOverSharedMemory() throws Exception {
    String uri = "shm://mqtt311-" + UUID.randomUUID();
    String topic = "memory/paho/mqtt3/" + UUID.randomUUID();
    byte[] payload = "MQTT 3.1.1 over MapsMessaging SHM".getBytes(StandardCharsets.UTF_8);
    CountDownLatch arrived = new CountDownLatch(1);
    AtomicReference<byte[]> received = new AtomicReference<>();

    try (MemoryNetworkConfig.Registration registration = MemoryNetworkConfig.register(
        uri, MemoryConnectionConfig.sharedMemory(ENDPOINT))) {
      MqttClient client = new MqttClient(uri, "shm-3-" + UUID.randomUUID(), new MemoryPersistence());
      client.setTimeToWait(10_000);
      try {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setConnectionTimeout(10);
        client.connect(options);
        client.subscribe(topic, 1, (receivedTopic, message) -> {
          received.set(message.getPayload());
          arrived.countDown();
        });
        client.publish(topic, payload, 1, false);
        assertTrue(arrived.await(10, TimeUnit.SECONDS), "MQTT 3 message not delivered over SHM");
        assertArrayEquals(payload, received.get());
      } finally {
        if (client.isConnected()) {
          client.disconnectForcibly();
        }
        client.close();
      }
    }
  }

  @Test
  void mqtt5PublishesAndSubscribesOverSharedMemory() throws Exception {
    String uri = "shm://mqtt5-" + UUID.randomUUID();
    String topic = "memory/paho/mqtt5/" + UUID.randomUUID();
    byte[] payload = "MQTT 5 over MapsMessaging SHM".getBytes(StandardCharsets.UTF_8);
    CountDownLatch arrived = new CountDownLatch(1);
    AtomicReference<byte[]> received = new AtomicReference<>();

    try (MemoryNetworkConfig.Registration registration = MemoryNetworkConfig.register(
        uri, MemoryConnectionConfig.sharedMemory(ENDPOINT))) {
      org.eclipse.paho.mqttv5.client.MqttClient client = new org.eclipse.paho.mqttv5.client.MqttClient(
          uri, "shm-5-" + UUID.randomUUID(), new org.eclipse.paho.mqttv5.client.persist.MemoryPersistence());
      client.setTimeToWait(10_000);
      try {
        org.eclipse.paho.mqttv5.client.MqttConnectionOptions options =
            new org.eclipse.paho.mqttv5.client.MqttConnectionOptions();
        options.setConnectionTimeout(10);
        client.connect(options);
        client.subscribe(topic, 1, (receivedTopic, message) -> {
          received.set(message.getPayload());
          arrived.countDown();
        });
        client.publish(topic, payload, 1, false);
        assertTrue(arrived.await(10, TimeUnit.SECONDS), "MQTT 5 message not delivered over SHM");
        assertArrayEquals(payload, received.get());
      } finally {
        if (client.isConnected()) {
          client.disconnect();
        }
        client.close();
      }
    }
  }
}
