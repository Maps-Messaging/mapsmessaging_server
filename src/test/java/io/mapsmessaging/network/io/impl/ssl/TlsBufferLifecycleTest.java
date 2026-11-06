/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.io.impl.ssl;

import io.mapsmessaging.network.io.BufferedReadEndPoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class TlsBufferLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void completedHandshakePreservesApplicationBytesForProtocolHandoff() {
      ByteBuffer applicationBytes = ByteBuffer.allocate(16);
      applicationBytes.put(new byte[]{1, 2, 3, 4});

      SSLHandshakeManagerFinished finished = new SSLHandshakeManagerFinished(applicationBytes);

      ByteBuffer retained = finished.getHandshakeBufferIn();
      assertEquals(0, retained.position());
      assertEquals(4, retained.remaining());
      assertEquals(1, retained.get() & 0xff);
      assertEquals(2, retained.get() & 0xff);
      assertEquals(3, retained.get() & 0xff);
      assertEquals(4, retained.get() & 0xff);
    }

    @Test
    void emptyHandshakeApplicationBufferRemainsEmptyAfterHandoff() {
      ByteBuffer applicationBytes = ByteBuffer.allocate(16);

      SSLHandshakeManagerFinished finished = new SSLHandshakeManagerFinished(applicationBytes);

      assertFalse(finished.getHandshakeBufferIn().hasRemaining());
    }
  }

  @Nested
  class SadPath {

    @Test
    void sslEndpointMustAdvertiseBufferedReadCapability() {
      assertTrue(
          BufferedReadEndPoint.class.isAssignableFrom(SSLEndPoint.class),
          "SSLEndPoint can retain encrypted/application bytes and must advertise them to ReadTask");
    }
  }

  @Nested
  class Murphy {

    @Test
    void handshakeHandoffRetainsBytesAtExactApplicationBufferCapacity() {
      ByteBuffer applicationBytes = ByteBuffer.allocate(8);
      for (int i = 0; i < applicationBytes.capacity(); i++) {
        applicationBytes.put((byte) i);
      }

      SSLHandshakeManagerFinished finished = new SSLHandshakeManagerFinished(applicationBytes);

      ByteBuffer retained = finished.getHandshakeBufferIn();
      assertEquals(8, retained.remaining());
      for (int i = 0; i < 8; i++) {
        assertEquals(i, retained.get() & 0xff);
      }
    }

    @Test
    void repeatedInspectionOfRetainedHandshakeDataDoesNotMutateTheBuffer() {
      ByteBuffer applicationBytes = ByteBuffer.allocate(8);
      applicationBytes.put(new byte[]{9, 8, 7, 6});

      SSLHandshakeManagerFinished finished = new SSLHandshakeManagerFinished(applicationBytes);
      ByteBuffer retained = finished.getHandshakeBufferIn();

      int position = retained.position();
      int remaining = retained.remaining();
      assertEquals(9, retained.get(position) & 0xff);
      assertEquals(9, retained.get(position) & 0xff);
      assertEquals(position, retained.position());
      assertEquals(remaining, retained.remaining());
    }
  }
}
