/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.PrependerSignatureManager;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class PacketIntegrityFactory {

  private static final PacketIntegrityFactory instance = new PacketIntegrityFactory();

  public static PacketIntegrityFactory getInstance() {
    return instance;
  }

  public List<String> getAlgorithms() {
    return new ArrayList<>(implementations.keySet());
  }

  private final Map<String, PacketIntegrity> implementations;

  public PacketIntegrity getPacketIntegrity(String algoritm, SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    return implementations.get(algoritm).initialise(stamper, key);
  }

  public PacketIntegrity createPacketIntegrity(HmacConfigDTO node) {
    String hmacAlgorithm = node.getHmacAlgorithm();
    if (hmacAlgorithm != null) {
      String managerName = node.getHmacManager();
      SignatureManager manager;
      if (managerName.equalsIgnoreCase("appender")) {
        manager = new AppenderSignatureManager();
      } else {
        manager = new PrependerSignatureManager();
      }
      String keyStr = node.getHmacSharedKey();
      byte[] key = SharedKeyHelper.convertKey(keyStr);
      try {
        return PacketIntegrityFactory.getInstance().getPacketIntegrity(hmacAlgorithm, manager, key);
      } catch (NoSuchAlgorithmException | InvalidKeyException e) {
        // ToDo: Log Message Here
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
