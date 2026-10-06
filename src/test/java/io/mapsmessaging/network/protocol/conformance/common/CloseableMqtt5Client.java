package io.mapsmessaging.network.protocol.conformance.common;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;

/**
 * Test-only AutoCloseable wrapper around the Paho MQTT 5 synchronous client.
 */
public final class CloseableMqtt5Client extends MqttClient implements AutoCloseable {

  public CloseableMqtt5Client(String serverURI, String clientId) throws MqttException {
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
