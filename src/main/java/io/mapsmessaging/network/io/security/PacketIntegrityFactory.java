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

import io.mapsmessaging.dto.rest.config.network.HmacConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.PrependerSignatureManager;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static io.mapsmessaging.logging.ServerLogMessages.PACKET_SECURITY_INTERNAL_ERROR;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class PacketIntegrityFactory {

  private static class Holder {
    static final PacketIntegrityFactory INSTANCE = new PacketIntegrityFactory();
  }

  public static PacketIntegrityFactory getInstance() {
    return Holder.INSTANCE;
  }

  private final Logger logger = LoggerFactory.getLogger(PacketIntegrityFactory.class);

  private final Map<String, PacketIntegrity> implementations;

  public List<String> getAlgorithms() {
    List<String> names = new ArrayList<>(implementations.keySet());
    Collections.sort(names);
    return names;
  }

  public PacketIntegrity getPacketIntegrity(String algorithm, SignatureManager stamper, byte[] key)
      throws NoSuchAlgorithmException, InvalidKeyException {

    if (algorithm == null || algorithm.isBlank()) {
      throw new NoSuchAlgorithmException("Algorithm is null/blank");
    }
    if (stamper == null) {
      throw new IllegalArgumentException("SignatureManager is null");
    }
    if (key == null) {
      throw new InvalidKeyException("Key is null");
    }

    PacketIntegrity prototype = implementations.get(algorithm);
    if (prototype == null) {
      throw new NoSuchAlgorithmException("Unsupported algorithm: " + algorithm);
    }

    return prototype.initialise(stamper, key);
  }

  public PacketIntegrity createPacketIntegrity(HmacConfigDTO node) {
    String hmacAlgorithm = node.getHmacAlgorithm();
    if (hmacAlgorithm != null) {
      String managerName = node.getHmacManager();
      SignatureManager manager;
      if (managerName != null && managerName.equalsIgnoreCase("appender")) {
        manager = new AppenderSignatureManager();
      } else {
        manager = new PrependerSignatureManager();
      }

      String keyStr = node.getHmacSharedKey();
      byte[] key = SharedKeyHelper.convertKey(keyStr);
      try {
        return PacketIntegrityFactory.getInstance().getPacketIntegrity(hmacAlgorithm, manager, key);
      } catch (NoSuchAlgorithmException | InvalidKeyException e) {
        logger.log(PACKET_SECURITY_INTERNAL_ERROR, hmacAlgorithm, e.getMessage());
      }
    }
    return null;
  }

  private PacketIntegrityFactory() {
    implementations = new LinkedHashMap<>();
    ServiceLoader<PacketIntegrity> instanceList = ServiceLoader.load(PacketIntegrity.class);
    for (PacketIntegrity packetIntegrity : instanceList) {
      implementations.put(packetIntegrity.getName(), packetIntegrity);
    }
  }
}
