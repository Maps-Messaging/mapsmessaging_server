/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.common;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Test-only MQTT 3.1.1 client that performs a graceful disconnect before close.
 */
public final class CloseableMqtt311Client extends MqttClient {

  public CloseableMqtt311Client(String serverURI, String clientId) throws MqttException {
    super(serverURI, clientId, new MemoryPersistence());
  }

  @Override
  public void close() throws MqttException {
    if (isConnected()) {
      disconnect();
    }
    super.close();
  }
}
