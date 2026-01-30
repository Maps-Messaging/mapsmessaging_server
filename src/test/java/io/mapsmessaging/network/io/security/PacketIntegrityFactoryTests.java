/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.security;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.PrependerSignatureManager;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PacketIntegrityFactoryTests {

  @Test
  void getAlgorithms_returnsNonNullList() {
    List<String> algorithms = PacketIntegrityFactory.getInstance().getAlgorithms();
    Assertions.assertNotNull(algorithms);
  }

  @Test
  void getPacketIntegrity_throwsNoSuchAlgorithm_forNullAlgorithm() {
    SignatureManager signatureManager = new PrependerSignatureManager();
    byte[] key = new byte[32];

    Assertions.assertThrows(
        NoSuchAlgorithmException.class,
        () -> PacketIntegrityFactory.getInstance().getPacketIntegrity(null, signatureManager, key)
    );
  }

  @Test
  void getPacketIntegrity_throwsNoSuchAlgorithm_forBlankAlgorithm() {
    SignatureManager signatureManager = new PrependerSignatureManager();
    byte[] key = new byte[32];

    Assertions.assertThrows(
        NoSuchAlgorithmException.class,
        () -> PacketIntegrityFactory.getInstance().getPacketIntegrity("   ", signatureManager, key)
    );
  }

  @Test
  void getPacketIntegrity_throwsIllegalArgument_forNullSignatureManager() {
    byte[] key = new byte[32];

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> PacketIntegrityFactory.getInstance().getPacketIntegrity("HmacSHA256", null, key)
    );
  }

  @Test
  void getPacketIntegrity_throwsInvalidKey_forNullKey() {
    SignatureManager signatureManager = new PrependerSignatureManager();

    Assertions.assertThrows(
        InvalidKeyException.class,
        () -> PacketIntegrityFactory.getInstance().getPacketIntegrity("HmacSHA256", signatureManager, null)
    );
  }

  @Test
  void getPacketIntegrity_throwsNoSuchAlgorithm_forUnknownAlgorithm() {
    SignatureManager signatureManager = new PrependerSignatureManager();
    byte[] key = new byte[32];

    Assertions.assertThrows(
        NoSuchAlgorithmException.class,
        () -> PacketIntegrityFactory.getInstance().getPacketIntegrity("DefinitelyNotARealAlgorithm", signatureManager, key)
    );
  }

  @Test
  void getPacketIntegrity_happyPath_returnsInitialisedIntegrity_andCanSecure() throws Exception {
    List<String> algorithms = PacketIntegrityFactory.getInstance().getAlgorithms();
    Assertions.assertNotNull(algorithms);
    Assertions.assertFalse(algorithms.isEmpty(), "No PacketIntegrity implementations discovered via ServiceLoader");

    String algorithm = algorithms.get(0);

    Random random = new Random(1234567L);
    byte[] key = new byte[64];
    random.nextBytes(key);

    PacketIntegrityFactory factory = PacketIntegrityFactory.getInstance();

    PacketIntegrity prependerIntegrity = factory.getPacketIntegrity(algorithm, new PrependerSignatureManager(), key);
    Assertions.assertNotNull(prependerIntegrity);

    PacketIntegrity appenderIntegrity = factory.getPacketIntegrity(algorithm, new AppenderSignatureManager(), key);
    Assertions.assertNotNull(appenderIntegrity);

    Packet packet = new Packet(2048, false);
    for (int index = 0; index < 256; index++) {
      packet.put((byte) (index & 0xFF));
    }
    packet.flip();

    Packet securedPrepended = prependerIntegrity.secure(packet);
    Assertions.assertNotNull(securedPrepended);
    Assertions.assertTrue(prependerIntegrity.isSecure(securedPrepended));

    Packet packet2 = new Packet(2048, false);
    for (int index = 0; index < 256; index++) {
      packet2.put((byte) (index & 0xFF));
    }
    packet2.flip();

    Packet securedAppended = appenderIntegrity.secure(packet2);
    Assertions.assertNotNull(securedAppended);
    Assertions.assertTrue(appenderIntegrity.isSecure(securedAppended));
  }
}
