package io.mapsmessaging.api.message;

import io.mapsmessaging.storage.impl.streams.BufferObjectReader;
import io.mapsmessaging.storage.impl.streams.StreamObjectWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class TypedDataBranchCoverageTest {

  @Test
  void booleanWireEncodingPreservesBothValues() throws Exception {
    assertEquals(Boolean.TRUE, roundTrip(true).getData());
    assertEquals(Boolean.FALSE, roundTrip(false).getData());
  }

  @Test
  void scalarWireEncodingCoversNumericStringAndCharacterTypes() throws Exception {
    assertEquals((byte) 1, roundTrip((byte) 1).getData());
    assertEquals((short) 2, roundTrip((short) 2).getData());
    assertEquals(3, roundTrip(3).getData());
    assertEquals(4L, roundTrip(4L).getData());
    assertEquals(5.5f, (Float) roundTrip(5.5f).getData(), 0.0f);
    assertEquals(6.5d, (Double) roundTrip(6.5d).getData(), 0.0d);
    assertEquals("text", roundTrip("text").getData());
    assertEquals('x', roundTrip('x').getData());
  }

  @Test
  void arrayWireEncodingPreservesRepresentativePrimitiveAndStringArrays() throws Exception {
    assertArrayEquals(new byte[]{1, 2}, (byte[]) roundTrip(new byte[]{1, 2}).getData());
    assertArrayEquals(new int[]{3, 4}, (int[]) roundTrip(new int[]{3, 4}).getData());
    assertArrayEquals(new double[]{1.5, 2.5}, (double[]) roundTrip(new double[]{1.5, 2.5}).getData());
    assertArrayEquals(new String[]{"a", "b"}, (String[]) roundTrip(new String[]{"a", "b"}).getData());
    assertArrayEquals(new char[]{'a', 'b'}, (char[]) roundTrip(new char[]{'a', 'b'}).getData());
  }

  private static TypedData roundTrip(Object value) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    StreamObjectWriter writer = new StreamObjectWriter(output);
    new TypedData(value).write(writer);
    return new TypedData(new BufferObjectReader(ByteBuffer.wrap(output.toByteArray())));
  }
}