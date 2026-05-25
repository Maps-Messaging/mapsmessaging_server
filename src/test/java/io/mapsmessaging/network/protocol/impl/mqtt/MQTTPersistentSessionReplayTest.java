/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.security.uuid.UuidGenerator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MQTTPersistentSessionReplayTest extends MQTTBaseTest {

  private static final int MESSAGE_COUNT = 10;
  private static final long REPLAY_TIMEOUT_SECONDS = 10L;

  @ParameterizedTest
  @MethodSource("mqttReplayTestParameters")
  @DisplayName("Test MQTT persistent session replay without resubscribe")
  void testPersistentSessionReplayWithoutResubscribe(
      int version,
      String protocol,
      boolean auth,
      int qos) throws Exception {

    String brokerUrl = getUrl(protocol, auth);
    String topicName = getTopicName();
    String subscriberClientId = getClientId(
        "persistent-replay-subscriber-" + UuidGenerator.getInstance().generate(),
        version);
    String publisherClientId = getClientId(
        "persistent-replay-publisher-" + UuidGenerator.getInstance().generate(),
        version);
    Path persistenceDirectory = Files.createTempDirectory("mqtt-persistent-replay-");

    CountDownLatch replayLatch = new CountDownLatch(MESSAGE_COUNT);
    List<String> replayedPayloads = new CopyOnWriteArrayList<>();

    MqttClient subscriber = null;
    MqttClient publisher = null;
    MqttClient restoredSubscriber = null;

    try {
      subscriber = createPersistentSubscriberClient(
          brokerUrl,
          subscriberClientId,
          persistenceDirectory);

      subscriber.setCallback(new ReplayCallback(null, null));

      MqttConnectOptions initialOptions = createPersistentSessionOptions(auth, version);
      subscriber.connect(initialOptions);

      subscriber.subscribe(topicName, qos);
      Assertions.assertTrue(subscriber.isConnected());

      subscriber.disconnect();
      subscriber.close();
      subscriber = null;

      publisher = new MqttClient(
          brokerUrl,
          publisherClientId,
          new MemoryPersistence());

      MqttConnectOptions publisherOptions = getOptions(auth, version);
      publisherOptions.setCleanSession(true);
      publisher.connect(publisherOptions);

      for (int messageIndex = 0; messageIndex < MESSAGE_COUNT; messageIndex++) {
        String payload = "offline-replay-message-" + messageIndex;

        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(qos);
        message.setRetained(false);

        publisher.publish(topicName, message);
      }

      publisher.disconnect();
      publisher.close();
      publisher = null;

      restoredSubscriber = createPersistentSubscriberClient(
          brokerUrl,
          subscriberClientId,
          persistenceDirectory);

      restoredSubscriber.setCallback(new ReplayCallback(replayLatch, replayedPayloads));

      MqttConnectOptions restoreOptions = createPersistentSessionOptions(auth, version);
      restoredSubscriber.connect(restoreOptions);

      Assertions.assertTrue(restoredSubscriber.isConnected());

      boolean replayCompleted = replayLatch.await(REPLAY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      Assertions.assertTrue(
          replayCompleted,
          "Expected queued messages to be replayed after reconnect without a new subscribe");

      Assertions.assertEquals(MESSAGE_COUNT, replayedPayloads.size());

      for (int messageIndex = 0; messageIndex < MESSAGE_COUNT; messageIndex++) {
        Assertions.assertTrue(
            replayedPayloads.contains("offline-replay-message-" + messageIndex),
            "Missing replayed payload offline-replay-message-" + messageIndex);
      }

      restoredSubscriber.disconnect();
      restoredSubscriber.close();
      restoredSubscriber = null;
    } finally {
      closeQuietly(restoredSubscriber);
      closeQuietly(publisher);
      closeQuietly(subscriber);
      deleteDirectory(persistenceDirectory);
    }
  }

  static Stream<Arguments> mqttReplayTestParameters() {
    List<Arguments> argumentsList = new java.util.ArrayList<>();
    int[] protocols = {MQTT_3_1, MQTT_3_1_1};
    String[] connectionTypes = {"tcp", "ssl", "ws", "wss"};
    boolean[] authOptions = {false, true};
    int[] qosLevels = {1, 2};

    for (int mqttVersion : protocols) {
      for (String connectionType : connectionTypes) {
        for (boolean auth : authOptions) {
          for (int qos : qosLevels) {
            argumentsList.add(Arguments.of(mqttVersion, connectionType, auth, qos));
          }
        }
      }
    }

    return argumentsList.stream();
  }

  private MqttClient createPersistentSubscriberClient(
      String brokerUrl,
      String clientId,
      Path persistenceDirectory) throws MqttException {

    MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(
        persistenceDirectory.toAbsolutePath().toString());

    return new MqttClient(
        brokerUrl,
        clientId,
        persistence);
  }

  private MqttConnectOptions createPersistentSessionOptions(
      boolean auth,
      int version) throws IOException {

    MqttConnectOptions options = getOptions(auth, version);
    options.setCleanSession(false);
    options.setAutomaticReconnect(false);
    return options;
  }

  private void closeQuietly(MqttClient client) {
    if (client == null) {
      return;
    }

    try {
      if (client.isConnected()) {
        client.disconnect();
      }
    } catch (Exception ignored) {
    }

    try {
      client.close();
    } catch (Exception ignored) {
    }
  }

  private void deleteDirectory(Path directory) {
    if (directory == null) {
      return;
    }

    try (Stream<Path> paths = Files.walk(directory)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
          });
    } catch (IOException ignored) {
    }
  }

  private static class ReplayCallback implements MqttCallback {

    private final CountDownLatch replayLatch;
    private final List<String> replayedPayloads;

    ReplayCallback(
        CountDownLatch replayLatch,
        List<String> replayedPayloads) {

      this.replayLatch = replayLatch;
      this.replayedPayloads = replayedPayloads;
    }

    @Override
    public void connectionLost(Throwable throwable) {
    }

    @Override
    public void messageArrived(
        String topicName,
        MqttMessage mqttMessage) {

      if (replayedPayloads != null) {
        replayedPayloads.add(new String(mqttMessage.getPayload(), StandardCharsets.UTF_8));
      }

      if (replayLatch != null) {
        replayLatch.countDown();
      }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
  }
}