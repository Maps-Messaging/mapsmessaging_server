package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageQueueUnpackerBranchCoverageTest {

  @Test
  void invalidCompressedDataIsReportedAsIoFailure() {
    IOException failure = assertThrows(
        IOException.class,
        () -> MessageQueueUnpacker.unpack(new byte[]{1, 2, 3, 4, 5}, true, null));

    assertNotNull(failure.getCause());
  }

  @Test
  void emptyUncompressedInputFailsClosed() throws Exception {
    assertTrue(MessageQueueUnpacker.unpack(new byte[0], false, null).isEmpty());
  }

  @Test
  void encryptedInputIsDecryptedBeforeFrameValidation() throws Exception {
    CipherManager cipher = new CipherManager("key".getBytes());
    byte[] encrypted = cipher.encrypt(new byte[]{1, 2, 3});

    assertTrue(
        MessageQueueUnpacker.unpack(encrypted, false, cipher).isEmpty(),
        "decrypted malformed frames must fail closed without leaking runtime exceptions");
  }
}