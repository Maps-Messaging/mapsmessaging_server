package io.mapsmessaging.auth.registry;

import org.junit.jupiter.api.Test;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UUIDSerializerCoverageSweepTest {
  @Test
  void serializerWritesAndReadsUuidBitHalvesInOrder() throws Exception {
    UUID value = UUID.randomUUID();
    DataOutput2 output = mock(DataOutput2.class);
    UUIDSerializer serializer = new UUIDSerializer();

    serializer.serialize(output, value);

    verify(output).writeLong(value.getMostSignificantBits());
    verify(output).writeLong(value.getLeastSignificantBits());

    DataInput2 input = mock(DataInput2.class);
    when(input.readLong())
        .thenReturn(value.getMostSignificantBits(), value.getLeastSignificantBits());

    assertEquals(value, serializer.deserialize(input, 16));
  }
}
