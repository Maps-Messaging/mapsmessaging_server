/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** MQTT-SN 2.0 CSD01 section 3.17 HMAC verification and replay constraints. */
class MqttSn2ProtectionVerifierTest {
  private static final byte[] KEY = new byte[32];

  @Test
  void verifiedHmacFrameIsUnwrappedOnceAndReplayIsRejected() throws Exception {
    byte[] inner = {4, 12, 0, 9};
    byte[] wire = protect(inner, 1);
    MqttSn2ProtectionVerifier verifier = new MqttSn2ProtectionVerifier((sender, scheme) -> KEY.clone());
    ByteBuffer decoded = verifier.verify(ByteBuffer.wrap(wire));
    byte[] result = new byte[decoded.remaining()];
    decoded.get(result);
    assertArrayEquals(inner, result);
    assertThrows(IOException.class, () -> verifier.verify(ByteBuffer.wrap(wire)));
    assertArrayEquals(inner, bytes(verifier.verify(ByteBuffer.wrap(protect(inner, 2)))));
  }

  @Test
  void tamperingNeverAdvancesReplayCounter() throws Exception {
    byte[] inner = {4, 12, 0, 9};
    byte[] wire = protect(inner, 10);
    MqttSn2ProtectionVerifier verifier = new MqttSn2ProtectionVerifier((sender, scheme) -> KEY.clone());
    wire[wire.length - 1] ^= 1;
    assertThrows(IOException.class, () -> verifier.verify(ByteBuffer.wrap(wire)));
    assertArrayEquals(inner, bytes(verifier.verify(ByteBuffer.wrap(protect(inner, 10)))));
  }

  @Test
  void missingKeyIsRejectedBeforeDispatch() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    MqttSn2ProtectionVerifier verifier = new MqttSn2ProtectionVerifier((sender, scheme) -> {
      calls.incrementAndGet();
      return null;
    });
    assertThrows(IOException.class, () -> verifier.verify(ByteBuffer.wrap(protect(new byte[]{4,12,0,9}, 1))));
    assertEquals(1, calls.get());
  }

  @Test
  void outboundHmacRoundTripsThroughIndependentVerifier() throws Exception {
    MqttSn2ProtectionVerifier sender = new MqttSn2ProtectionVerifier((id, scheme) -> KEY.clone());
    MqttSn2ProtectionVerifier receiver = new MqttSn2ProtectionVerifier((id, scheme) -> KEY.clone());
    byte[] inner = {4, 12, 0, 9};
    ByteBuffer wire = sender.protect(ByteBuffer.wrap(inner), 0,
        new byte[]{0,0,0,0,0,0,0,7}, new byte[]{1,2,3,4}, new byte[]{0,1});
    assertArrayEquals(inner, bytes(receiver.verify(wire)));
    assertThrows(IOException.class, () -> receiver.verify(wire));
    assertThrows(IOException.class, () -> sender.protect(ByteBuffer.wrap(inner), 0,
        new byte[]{0,0,0,0,0,0,0,7}, new byte[]{1,2,3,4}, new byte[0]));
  }

  private static byte[] protect(byte[] inner, int counter) throws Exception {
    // Short MQTT-SN frame plus CSD01 Protection Encapsulation fixed fields.
    ByteBuffer prefix = ByteBuffer.allocate(18);
    prefix.put((byte)(18 + inner.length + 32)).put((byte)0xff)
        .put((byte)0x11).put((byte)0);
    prefix.put(new byte[]{0,0,0,0,0,0,0,7});
    prefix.put(new byte[]{1,2,3,4});
    prefix.putShort((short)counter);
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
    mac.update(prefix.array());
    byte[] tag = mac.doFinal(inner);
    ByteBuffer wire = ByteBuffer.allocate(prefix.capacity() + inner.length + tag.length);
    wire.put(prefix.array()).put(inner).put(tag);
    return wire.array();
  }

  private static byte[] bytes(ByteBuffer source) {
    byte[] result = new byte[source.remaining()];
    source.get(result);
    return result;
  }
}
