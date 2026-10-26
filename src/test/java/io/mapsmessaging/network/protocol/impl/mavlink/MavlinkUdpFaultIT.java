/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mavlink;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.network.UdpFaultProxy;
import io.mapsmessaging.test.network.UdpFaultProxy.Direction;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Validate real MAVLink UDP delivery and isolation under deliberate faults. */
class MavlinkUdpFaultIT extends BaseTestConfig {
  private static final InetSocketAddress MAPS = new InetSocketAddress("127.0.0.1", 14550);

  @Test
  void droppedHeartbeatDoesNotCorruptNextValidHeartbeat() throws Exception {
    verifyRecovery(false);
  }

  @Test
  void corruptedHeartbeatDoesNotCorruptNextValidHeartbeat() throws Exception {
    verifyRecovery(true);
  }

  private void verifyRecovery(boolean corrupt) throws Exception {
    List<Message> messages = new CopyOnWriteArrayList<>();
    CountDownLatch delivered = new CountDownLatch(1);
    MessageListener listener = event -> {
      messages.add(event.getMessage());
      delivered.countDown();
      event.getCompletionTask().run();
    };
    Session session = createSession("mavlink-proxy-" + System.nanoTime(), 60, 60, false, listener);
    assertNotNull(session);
    SubscriptionContext subscription = new SubscriptionContextBuilder("/mavlink/#", ClientAcknowledgement.AUTO)
        .setQos(QualityOfService.AT_MOST_ONCE).setReceiveMaximum(100).build();
    try (UdpFaultProxy proxy = new UdpFaultProxy(0, MAPS);
         DatagramSocket sender = new DatagramSocket()) {
      session.addSubscription(subscription);
      if (corrupt) {
        proxy.reverseRangeNext(Direction.CLIENT_TO_SERVER, 1, 0, 4);
      } else {
        proxy.dropNext(Direction.CLIENT_TO_SERVER, 1);
      }
      send(sender, proxy, heartbeat(31, 41, 1));
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
      while (proxy.stats().clientToServerReceived() < 1 && System.nanoTime() < deadline) {
        Thread.sleep(5);
      }
      assertEquals(1, corrupt ? proxy.stats().modified() : proxy.stats().dropped(), "Fault must actually fire");
      send(sender, proxy, heartbeat(32, 41, 1));
      assertTrue(delivered.await(5, TimeUnit.SECONDS), "Valid heartbeat following network fault must be delivered");
      assertEquals(1, messages.size(), "Malformed or dropped datagram must not create a second heartbeat");
      JsonObject root = JsonParser.parseString(new String(messages.getFirst().getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();
      JsonObject mavlink = root.getAsJsonObject("mavlink");
      assertEquals(32, mavlink.get("sequence").getAsInt());
      assertEquals(41, mavlink.get("systemId").getAsInt());
      assertEquals(0, mavlink.get("messageId").getAsInt());
      assertEquals(0, proxy.stats().ioFailures());
      session.removeSubscription(subscription.getKey());
    } finally {
      close(session);
    }
  }

  private static void send(DatagramSocket socket, UdpFaultProxy proxy, byte[] data) throws Exception {
    socket.send(new DatagramPacket(data, data.length, proxy.getListenAddress()));
  }

  private static byte[] heartbeat(int sequence, int systemId, int componentId) {
    byte[] frame = new byte[17];
    frame[0] = (byte) 0xfe;
    frame[1] = 9;
    frame[2] = (byte) sequence;
    frame[3] = (byte) systemId;
    frame[4] = (byte) componentId;
    frame[5] = 0;
    frame[14] = 3;
    int crc = 0xffff;
    for (int index = 1; index < 15; index++) {
      crc = accumulate(frame[index], crc);
    }
    crc = accumulate((byte) 50, crc);
    frame[15] = (byte) crc;
    frame[16] = (byte) (crc >>> 8);
    return frame;
  }

  private static int accumulate(byte input, int crc) {
    int value = (input ^ (crc & 0xff)) & 0xff;
    value = (value ^ ((value << 4) & 0xff)) & 0xff;
    return ((crc >>> 8) ^ (value << 8) ^ (value << 3) ^ (value >>> 4)) & 0xffff;
  }
}
