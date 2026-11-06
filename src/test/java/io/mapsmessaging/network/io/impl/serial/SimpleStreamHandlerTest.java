package io.mapsmessaging.network.io.impl.serial;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class SimpleStreamHandlerTest {

  @Test
  void inputIsReadInChunksAndAppendsToExistingPacketContents() throws Exception {
    SimpleStreamHandler handler = new SimpleStreamHandler(4);
    Packet packet = new Packet(32, false);
    packet.put(new byte[]{99, 98});

    int count = handler.parseInput(
        new ByteArrayInputStream(new byte[]{1,2,3,4,5,6,7,8,9}),
        packet
    );

    assertEquals(9, count);
    assertEquals(11, packet.position());
    packet.flip();
    byte[] actual = new byte[11];
    packet.get(actual);
    assertArrayEquals(new byte[]{99,98,1,2,3,4,5,6,7,8,9}, actual);
  }

  @Test
  void closedStreamReportedDuringAvailableReadRaisesIOException() {
    SimpleStreamHandler handler = new SimpleStreamHandler(4);
    InputStream closed = new InputStream() {
      @Override
      public int available() {
        return 1;
      }

      @Override
      public int read() {
        return -1;
      }

      @Override
      public int read(byte[] b, int off, int len) {
        return -1;
      }
    };

    IOException failure = assertThrows(
        IOException.class,
        () -> handler.parseInput(closed, new Packet(8, false))
    );
    assertTrue(failure.getMessage().contains("closed"));
  }

  @Test
  void outputLargerThanInternalBufferIsChunkedWithoutDataLoss() throws Exception {
    SimpleStreamHandler handler = new SimpleStreamHandler(4);
    byte[] payload = new byte[]{1,2,3,4,5,6,7,8,9,10};
    Packet packet = new Packet(ByteBuffer.wrap(payload));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    int written = handler.parseOutput(output, packet);

    assertEquals(payload.length, written);
    assertArrayEquals(payload, output.toByteArray());
    assertEquals(0, packet.available());
  }

  @Test
  void closeIsSafeAndEmptyOutputWritesNothing() throws Exception {
    SimpleStreamHandler handler = new SimpleStreamHandler(4);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertDoesNotThrow(handler::close);
    assertEquals(0, handler.parseOutput(output, new Packet(0, false)));
    assertEquals(0, output.size());
  }
}
