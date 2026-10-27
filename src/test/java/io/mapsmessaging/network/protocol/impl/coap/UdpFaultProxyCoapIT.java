/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.test.network.UdpFaultProxy;
import io.mapsmessaging.test.network.UdpFaultProxy.Direction;
import io.mapsmessaging.test.network.UdpFaultProxy.Stats;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end CoAP exchanges across the protocol-neutral UDP fault proxy. */
class UdpFaultProxyCoapIT extends BaseCoapTest {

  private static UdpFaultProxy proxy;

  @BeforeAll
  static void startProxy() throws SocketException {
    proxy = new UdpFaultProxy(0, new InetSocketAddress("127.0.0.1", 5683));
  }

  @BeforeEach
  void resetRules() {
    proxy.resetRules();
  }

  @AfterEach
  void clearRules() {
    proxy.resetRules();
  }

  @AfterAll
  static void stopProxy() {
    if (proxy != null) {
      proxy.close();
    }
  }

  @Test
  void transparentProxyPreservesCoapPutAndGetPayload() throws Exception {
    CoapClient client = createClient();
    try {
      byte[] payload = payload(197);
      CoapResponse putResponse = client.put(payload, 0);
      assertSuccessfulPut(putResponse);
      CoapResponse getResponse = client.get();
      assertNotNull(getResponse, "GET must receive a response through the proxy");
      assertArrayEquals(payload, getResponse.getPayload());
      assertTrue(proxy.stats().clientToServerForwarded() > 0);
      assertTrue(proxy.stats().serverToClientForwarded() > 0);
      assertEquals(0, proxy.stats().ioFailures());
    } finally {
      client.shutdown();
    }
  }

  @Test
  void droppedFirstPutRequestIsRetransmittedAndPreservesPayload() throws Exception {
    CoapClient client = createClient();
    try {
      byte[] payload = payload(193);
      Stats before = proxy.stats();
      proxy.dropNext(Direction.CLIENT_TO_SERVER, 1);
      CoapResponse response = client.put(payload, 0);
      assertSuccessfulPut(response);
      Stats afterPut = proxy.stats();
      assertEquals(1L, afterPut.dropped() - before.dropped(), "The requested packet loss must actually occur");
      assertTrue(afterPut.clientToServerReceived() - before.clientToServerReceived() >= 2,
          "The client must retransmit the missing confirmable PUT");
      assertEquals(0, afterPut.pendingRules());
      CoapResponse get = client.get();
      assertNotNull(get, "GET must succeed after PUT retransmission");
      assertArrayEquals(payload, get.getPayload());
    } finally {
      client.shutdown();
    }
  }

  @Test
  void droppedBlock1ContinueIsRetransmittedWithoutPayloadCorruption() throws Exception {
    CoapClient client = createClient();
    try {
      byte[] payload = payload(1_024);
      int blockSize = 256;
      Stats before = proxy.stats();
      proxy.dropNext(Direction.SERVER_TO_CLIENT, 1);
      for (int blockNumber = 0; blockNumber < payload.length / blockSize; blockNumber++) {
        Request put = Request.newPut();
        byte[] blockPayload = new byte[blockSize];
        System.arraycopy(payload, blockNumber * blockSize, blockPayload, 0, blockSize);
        put.setPayload(blockPayload);
        boolean more = blockNumber != (payload.length / blockSize) - 1;
        put.getOptions().setBlock1(4, more, blockNumber);
        CoapResponse response = client.advanced(put);
        assertNotNull(response, "Block1 exchange " + blockNumber + " must complete");
        if (more) {
          assertEquals(CoAP.ResponseCode.CONTINUE, response.getCode(), "Block1 intermediate response");
        } else {
          assertSuccessfulPut(response);
        }
      }
      Stats afterPut = proxy.stats();
      assertEquals(1L, afterPut.dropped() - before.dropped(), "The first server response must be dropped");
      assertTrue(afterPut.clientToServerReceived() - before.clientToServerReceived() >= 5,
          "Lost Continue must cause a retransmission in addition to the four Block1 requests");
      assertEquals(0, afterPut.pendingRules());
      CoapResponse get = client.get();
      assertNotNull(get, "GET must succeed after a retransmitted Block1 exchange");
      assertArrayEquals(payload, get.getPayload(), "Retransmission must not duplicate or reorder stored blocks");
    } finally {
      client.shutdown();
    }
  }

  @Test
  void reorderedConfirmablePingsReceiveDistinctResetsInReversedOrder() throws Exception {
    try (DatagramSocket client = rawClient()) {
      Stats before = proxy.stats();
      proxy.reverseNext(Direction.CLIENT_TO_SERVER, 2, Duration.ofSeconds(2));
      sendPing(client, 0x2101);
      sendPing(client, 0x2102);
      assertEquals(0x2102, receiveResetMessageId(client));
      assertEquals(0x2101, receiveResetMessageId(client));
      Stats after = proxy.stats();
      assertEquals(2L, after.reversed() - before.reversed());
      assertEquals(0L, after.partialReverseFlushes() - before.partialReverseFlushes());
      assertEquals(0, after.pendingRules());
    }
  }

  @Test
  void incompleteReverseBatchFlushesSoStopAndWaitClientCanProgress() throws Exception {
    try (DatagramSocket client = rawClient()) {
      Stats before = proxy.stats();
      proxy.reverseNext(Direction.CLIENT_TO_SERVER, 2, Duration.ofMillis(150));
      sendPing(client, 0x2201);
      assertEquals(0x2201, receiveResetMessageId(client));
      Stats after = proxy.stats();
      assertEquals(1L, after.partialReverseFlushes() - before.partialReverseFlushes());
      assertEquals(0L, after.reversed() - before.reversed());
      assertEquals(0, after.pendingRules());
    }
  }

  @Test
  void corruptedPingDoesNotPreventSubsequentValidCoapExchange() throws Exception {
    try (DatagramSocket client = rawClient()) {
      Stats before = proxy.stats();
      proxy.reverseRangeNext(Direction.CLIENT_TO_SERVER, 1, 0, 4);
      sendPing(client, 0x2301);
      sendPing(client, 0x2302);
      assertEquals(0x2302, receiveResetMessageId(client));
      Stats after = proxy.stats();
      assertEquals(1L, after.modified() - before.modified());
      assertEquals(2L, after.clientToServerForwarded() - before.clientToServerForwarded());
      assertEquals(0, after.pendingRules());
    }
  }

  private static CoapClient createClient() {
    String original = getUri();
    String throughProxy = original.replace(":5683/", ":" + proxy.getListenPort() + "/");
    CoapClient client = new CoapClient(throughProxy);
    client.setTimeout(15_000L);
    return client;
  }

  private static DatagramSocket rawClient() throws SocketException {
    DatagramSocket client = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
    client.setSoTimeout(5_000);
    return client;
  }

  private static void sendPing(DatagramSocket client, int messageId) throws IOException {
    byte[] message = new byte[]{0x40, 0x00, (byte) (messageId >>> 8), (byte) messageId};
    client.send(new DatagramPacket(message, message.length, proxy.getListenAddress()));
  }

  private static int receiveResetMessageId(DatagramSocket client) throws IOException {
    byte[] buffer = new byte[1_024];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    client.receive(packet);
    assertEquals(4, packet.getLength(), "CoAP empty Reset must have exactly four bytes");
    assertEquals(0x70, buffer[0] & 0xff, "Confirmable CoAP ping must receive Reset (RST)");
    assertEquals(0, buffer[1] & 0xff, "Reset must have empty code");
    return (buffer[2] & 0xff) << 8 | (buffer[3] & 0xff);
  }

  private static void assertSuccessfulPut(CoapResponse response) {
    assertNotNull(response, "PUT must receive a response");
    assertTrue(response.getCode() == CoAP.ResponseCode.CREATED || response.getCode() == CoAP.ResponseCode.CHANGED,
        "Unexpected PUT response: " + response.getCode());
  }

  private static byte[] payload(int length) {
    byte[] bytes = new byte[length];
    for (int index = 0; index < bytes.length; index++) {
      bytes[index] = (byte) ((index * 37) & 0xff);
    }
    return bytes;
  }
}
