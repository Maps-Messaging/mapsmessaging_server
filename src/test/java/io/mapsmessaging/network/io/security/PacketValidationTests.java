package io.mapsmessaging.network.io.security;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.PrependerSignatureManager;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PacketValidationTests {

  @ParameterizedTest
  @MethodSource
  void testSimpleValidation(String algorithm, SignatureManager stamper) throws NoSuchAlgorithmException, InvalidKeyException {
    Random r = new Random(System.nanoTime());
    byte[] key = new byte[100];
    r.nextBytes(key);
    PacketIntegrity packetIntegrity = PacketIntegrityFactory.getInstance().getPacketIntegrity(algorithm, stamper, key);
    Packet p = new Packet(2048, false);
    for (int x = 0; x < 1024; x++) {
      p.put((byte) (x % 0xff));
    }
    p.flip();
    p = packetIntegrity.secure(p);
    Assertions.assertEquals(1024+packetIntegrity.size(), p.limit());
    Assertions.assertTrue(packetIntegrity.isSecure(p));
  }

  private static Stream<Arguments> testSimpleValidation() {
    List<Arguments> list = new ArrayList<>();
    for(String algorithm:PacketIntegrityFactory.getInstance().getAlgorithms()) {
      list.add(arguments(algorithm, new PrependerSignatureManager()));
      list.add(arguments(algorithm, new AppenderSignatureManager()));
    }
    return list.stream();
  }

}
