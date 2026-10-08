/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.MqttSnVersionDetector.Version.*;

class MqttSnVersionDetectorTest {

  @Test
  void distinguishesV1AndV2ConnectFrames() {
    assertEquals(V1_2, detect(7, 4, 4, 1, 0, 30, 'a'));
    assertEquals(V2_0, detect(11, 1, 1, 0, 17, 2, 0, 30, 0, 20, 'a'));
  }

  @Test
  void recognizesCsd01ConnectWithWillAndOptionalSessionFields() {
    assertEquals(V2_0, detect(20, 1, 0x1B, 0x17, 0, 23, 2,
        0, 60, 0, 20, 0, 0, 0, 1, 5, 'c', 'l', 'i', 'd'));
  }

  @Test
  void doesNotMisclassifyDiscoveryOrPublishAsConnect() {
    assertEquals(UNKNOWN, detect(3, 1, 5));
    assertEquals(UNKNOWN, detect(5, 3, 0, 0, 1));
    assertEquals(UNKNOWN, detect(3, 23, 5));
  }

  @Test
  void rejectsInvalidLengthAndVersion() {
    assertEquals(UNKNOWN, detect(8, 4, 4, 1, 0, 30, 'a'));
    assertEquals(UNKNOWN, detect(11, 1, 1, 0, 0, 2, 0, 30, 0, 20, 'a'));
    assertEquals(UNKNOWN, detect(11, 1, 1, 0, 17, 1, 0, 30, 0, 20, 'a'));
    assertEquals(UNKNOWN, detect(1, 0, 7, 4, 4, 1));
  }

  @Test
  void handlesExtendedLengthFramingAndPreservesPosition() {
    ByteBuffer wire = ByteBuffer.wrap(new byte[]{99, 1, 0, 13, 1, 1, 0, 5, 2, 0, 30, 0, 20, 'a'});
    wire.position(1);
    assertEquals(V2_0, MqttSnVersionDetector.detect(wire));
    assertEquals(1, wire.position());
  }

  private static MqttSnVersionDetector.Version detect(int... values) {
    ByteBuffer wire = ByteBuffer.allocate(values.length);
    for (int value : values) {
      wire.put((byte) value);
    }
    wire.flip();
    return MqttSnVersionDetector.detect(wire);
  }
}
