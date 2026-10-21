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

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class MQTTReplayAcknowledgementBarrierTest {

  @Test
  @Timeout(15)
  void qos2_replay_callback_must_not_allow_disconnect_before_pubcomp() throws Exception {
    PacketIdManager packetIds = new PacketIdManager();
    int packetId = packetIds.nextPacketIdentifier(null, 1L);
    CountDownLatch messageArrived = new CountDownLatch(1);
    CountDownLatch pubRecReceived = new CountDownLatch(1);
    CountDownLatch sendPubRel = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      MqttClient client = new MqttClient("tcp://localhost:" + server.getLocalPort(),
          "replay-ack-barrier", new MemoryPersistence());
      try {
        Future<?> peer = executor.submit(() -> {
          try (Socket socket = server.accept()) {
            socket.setSoTimeout(5000);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            OutputStream output = socket.getOutputStream();
            assertEquals(1, readPacket(input)); // CONNECT
            output.write(new byte[] {0x20, 3, 0, 0, 0}); // CONNACK, no properties
            // A replayed QoS 2 PUBLISH, topic "t", no properties, payload "x".
            output.write(new byte[] {0x34, 7, 0, 1, 't', (byte) (packetId >>> 8),
                (byte) packetId, 0, 'x'});
            output.flush();
            assertEquals(5, readPacket(input)); // PUBREC
            pubRecReceived.countDown();
            assertTrue(sendPubRel.await(5, TimeUnit.SECONDS));
            output.write(new byte[] {0x62, 2, (byte) (packetId >>> 8), (byte) packetId});
            output.flush();
            assertEquals(7, readPacket(input), "PUBCOMP must precede teardown");
            packetIds.completePacketId(packetId);
            assertEquals(14, readPacket(input)); // DISCONNECT
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
          } catch (IOException e) {
            throw new AssertionError(e);
          }
        });
        client.setCallback(new MqttCallback() {
          @Override public void disconnected(MqttDisconnectResponse response) { }
          @Override public void mqttErrorOccurred(MqttException exception) { }
          @Override public void messageArrived(String topic, MqttMessage message) {
            assertEquals("x", new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8));
            messageArrived.countDown();
          }
          @Override public void deliveryComplete(IMqttToken token) { }
          @Override public void connectComplete(boolean reconnect, String serverUri) { }
          @Override public void authPacketArrived(int reasonCode, MqttProperties properties) { }
        });
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setKeepAliveInterval(0);
        client.connect(options);
        assertTrue(messageArrived.await(5, TimeUnit.SECONDS),
            "Paho delivers the QoS 2 callback before the peer sends PUBREL");
        assertTrue(pubRecReceived.await(5, TimeUnit.SECONDS));
        assertEquals(1, packetIds.size(), "Callback delivery leaves QoS 2 acknowledgement outstanding");
        CountDownLatch teardownStarted = new CountDownLatch(1);
        Future<?> teardown = executor.submit(() -> {
          teardownStarted.countDown();
          try {
            MQTTPersistentSessionReplayTest.awaitReplayAcknowledgements(packetIds);
            assertTrue(client.isConnected());
            client.disconnect();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        });
        assertTrue(teardownStarted.await(5, TimeUnit.SECONDS));
        try {
          assertThrows(TimeoutException.class, () -> teardown.get(100, TimeUnit.MILLISECONDS),
              "The replay callback must not release teardown while PUBCOMP is outstanding");
        } finally {
          sendPubRel.countDown();
        }
        teardown.get(5, TimeUnit.SECONDS);
        peer.get(5, TimeUnit.SECONDS);
        assertEquals(0, packetIds.size());
      } finally {
        if (client.isConnected()) {
          client.disconnectForcibly(0, 0, false);
        }
        client.close();
      }
    } finally {
      sendPubRel.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private static int readPacket(DataInputStream input) throws IOException {
    int header = input.readUnsignedByte();
    int length = 0;
    int multiplier = 1;
    int encoded;
    do {
      encoded = input.readUnsignedByte();
      length += (encoded & 127) * multiplier;
      multiplier *= 128;
    } while ((encoded & 128) != 0);
    input.readFully(new byte[length]);
    return header >>> 4;
  }
}
