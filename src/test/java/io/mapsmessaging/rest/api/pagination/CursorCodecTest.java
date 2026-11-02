package io.mapsmessaging.rest.api.pagination;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class CursorCodecTest {
  @Test
  void rejectsExpiredCursor() {
    byte[] key = new byte[32];
    CursorCodec issuing = new CursorCodec(key, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    CursorCodec expired = new CursorCodec(key, Clock.fixed(Instant.parse("2026-01-01T01:00:00Z"), ZoneOffset.UTC));
    assertThrows(IllegalArgumentException.class, () -> expired.decode(issuing.encode("abc", "scope"), "scope"));
  }

  @Test
  void restoresKeyAndRejectsAnotherScope() {
    CursorCodec codec = new CursorCodec();
    String cursor = codec.encode("schema/1", "schemas:filter");
    assertEquals("schema/1", codec.decode(cursor, "schemas:filter"));
    assertThrows(IllegalArgumentException.class, () -> codec.decode(cursor, "schemas:other"));
  }
}
