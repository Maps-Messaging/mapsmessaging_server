package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CipherManagerTest {

  @Test
  void encryptDecryptRoundTripPreservesPayloadWithoutExposingPlaintext() throws Exception {
    CipherManager cipher = new CipherManager("shared-secret".getBytes(StandardCharsets.UTF_8));
    byte[] plaintext = "satellite-payload".getBytes(StandardCharsets.UTF_8);

    byte[] encrypted = cipher.encrypt(plaintext);

    assertTrue(encrypted.length > plaintext.length);
    assertFalse(Arrays.equals(plaintext, encrypted));
    assertArrayEquals(plaintext, cipher.decrypt(encrypted));
  }

  @Test
  void tamperedCiphertextIsRejected() throws Exception {
    CipherManager cipher = new CipherManager(new byte[]{1, 2, 3, 4});
    byte[] encrypted = cipher.encrypt(new byte[]{10, 20, 30});
    encrypted[encrypted.length - 1] ^= 0x01;

    assertThrows(IOException.class, () -> cipher.decrypt(encrypted));
  }

  @Test
  void ciphertextShorterThanIvAndTagIsRejectedBeforeDecrypt() throws Exception {
    CipherManager cipher = new CipherManager(new byte[]{9});

    assertThrows(IllegalArgumentException.class, () -> cipher.decrypt(new byte[27]));
  }

  @Test
  void differentSecretCannotDecryptCiphertext() throws Exception {
    CipherManager sender = new CipherManager("sender".getBytes(StandardCharsets.UTF_8));
    CipherManager receiver = new CipherManager("receiver".getBytes(StandardCharsets.UTF_8));

    byte[] encrypted = sender.encrypt(new byte[]{1, 2, 3});

    assertThrows(IOException.class, () -> receiver.decrypt(encrypted));
  }
}