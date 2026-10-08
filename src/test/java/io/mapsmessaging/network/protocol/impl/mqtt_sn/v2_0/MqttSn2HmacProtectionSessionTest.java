/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** CSD01 Protection Encapsulation, bidirectional HMAC and fail-closed counter policy. */
class MqttSn2HmacProtectionSessionTest {

  @Test
  void outgoingProtectionIsAcceptedOnceByIndependentReceiver() throws Exception {
    byte[] key = new byte[32];
    byte[] serverId = {0,0,0,0,0,0,0,7};
    AtomicInteger count = new AtomicInteger();
    MqttSn2ProtectionVerifier sender = new MqttSn2ProtectionVerifier((id, scheme) -> key.clone());
    MqttSn2ProtectionVerifier receiver = new MqttSn2ProtectionVerifier((id, scheme) -> key.clone());
    MqttSn2HmacProtectionSession session = new MqttSn2HmacProtectionSession(sender,
        () -> new byte[]{0, (byte)count.incrementAndGet()}, serverId, 0);

    ByteBuffer frame = session.send(ByteBuffer.wrap(new byte[]{4,12,0,9}));
    assertArrayEquals(new byte[]{4,12,0,9}, read(receiver.verify(frame)));
    assertThrows(IOException.class, () -> receiver.verify(frame));
    assertArrayEquals(new byte[]{4,12,0,9},
        read(receiver.verify(session.send(ByteBuffer.wrap(new byte[]{4,12,0,9})))));
    assertEquals(2, count.get());
  }

  @Test
  void counterSourceFailureNeverReturnsPlaintext() throws Exception {
    byte[] serverId = {0,0,0,0,0,0,0,7};
    MqttSn2ProtectionVerifier verifier = new MqttSn2ProtectionVerifier((id, scheme) -> new byte[32]);
    MqttSn2HmacProtectionSession session = new MqttSn2HmacProtectionSession(verifier,
        () -> { throw new IOException("No durable counter"); }, serverId, 0);
    assertThrows(IOException.class, () -> session.send(ByteBuffer.wrap(new byte[]{4,12,0,9})));
  }

  private static byte[] read(ByteBuffer frame) {
    byte[] bytes = new byte[frame.remaining()];
    frame.get(bytes);
    return bytes;
  }
}
