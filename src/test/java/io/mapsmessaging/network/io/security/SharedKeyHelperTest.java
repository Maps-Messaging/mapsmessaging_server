package io.mapsmessaging.network.io.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SharedKeyHelperTest {

  @Test
  void plainTextKeysAreTrimmedAndReturnedAsBytes() {
    assertArrayEquals(
        "maps-secret".getBytes(StandardCharsets.US_ASCII),
        SharedKeyHelper.convertKey("  maps-secret  ")
    );
  }

  @Test
  void commaSeparatedHexAndDecimalBytesAreDecoded() {
    assertArrayEquals(
        new byte[]{0x01, 0x0f, (byte) 0xff},
        SharedKeyHelper.convertKey("0x01, 15, 0xFF")
    );
  }

  @Test
  void malformedNumericKeyIsRejected() {
    assertThrows(
        NumberFormatException.class,
        () -> SharedKeyHelper.convertKey("0xGG")
    );
  }
}
