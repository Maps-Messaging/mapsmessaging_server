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

package io.mapsmessaging.network.protocol.impl.mqtt5;

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
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MQTTPersistentSessionReplayTest extends MQTTBaseTest {

  private static final int MESSAGE_COUNT = 10;
  private static final long SESSION_EXPIRY_INTERVAL_SECONDS = 3600L;
  private static final long REPLAY_TIMEOUT_SECONDS = 10L;

  @ParameterizedTest
  @MethodSource("mqttReplayTestParameters")
  @DisplayName("Test persistent session replay without resubscribe")
  void testPersistentSessionReplayWithoutResubscribe(
      int version,
      String protocol,
      boolean auth,
      int qos) throws Exception {

    String brokerUrl = getUrl(protocol, auth);
    String topicName = getTopicName();
    String subscriberClientId = "persistent-replay-subscriber-" + UuidGenerator.getInstance().generate();
    String publisherClientId = "persistent-replay-publisher-" + UuidGenerator.getInstance().generate();
    Path persistenceDirectory = Files.createTempDirectory("mqtt5-persistent-replay-");

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

      MqttConnectionOptions initialOptions = createPersistentSessionOptions(auth);
      subscriber.connect(initialOptions);

      MqttSubscription[] subscriptions = new MqttSubscription[1];
      subscriptions[0] = new MqttSubscription(topicName, qos);

      IMqttMessageListener[] listeners = new IMqttMessageListener[1];
      listeners[0] = (receivedTopicName, mqttMessage) -> {
      };

      subscriber.subscribe(subscriptions, listeners);
      Assertions.assertTrue(subscriber.isConnected());

      subscriber.disconnect();
      subscriber.close();
      subscriber = null;

      publisher = new MqttClient(
          brokerUrl,
          publisherClientId,
          new MemoryPersistence());

      MqttConnectionOptions publisherOptions = getOptions(auth);
      publisherOptions.setCleanStart(true);
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

      MqttConnectionOptions restoreOptions = createPersistentSessionOptions(auth);
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
    String[] connectionTypes = {"tcp", "ssl", "ws", "wss"};
    boolean[] authOptions = {false, true};
    int[] qosLevels = {1, 2};

    for (String connectionType : connectionTypes) {
      for (boolean auth : authOptions) {
        for (int qos : qosLevels) {
          argumentsList.add(Arguments.of(MQTT_5_0, connectionType, auth, qos));
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

  private MqttConnectionOptions createPersistentSessionOptions(boolean auth) throws IOException {
    MqttConnectionOptions options = getOptions(auth);
    options.setCleanStart(false);
    options.setSessionExpiryInterval(SESSION_EXPIRY_INTERVAL_SECONDS);
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
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
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
    public void deliveryComplete(IMqttToken token) {
    }

    @Override
    public void connectComplete(
        boolean reconnect,
        String serverURI) {
    }

    @Override
    public void authPacketArrived(
        int reasonCode,
        MqttProperties properties) {
    }
  }
}