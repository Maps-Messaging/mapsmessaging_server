/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.io.security;

import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.PrependerSignatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;

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

  public PacketIntegrity createPacketIntegrity(ConfigurationProperties properties) {
    String hmacAlgorithm = properties.getProperty("HmacAlgorithm");
    if (hmacAlgorithm != null) {
      String managerName = properties.getProperty("HmacManager", "Appender");
      SignatureManager manager;
      if (managerName.equalsIgnoreCase("appender")) {
        manager = new AppenderSignatureManager();
      } else {
        manager = new PrependerSignatureManager();
      }
      String keyStr = properties.getProperty("HmacSharedKey");
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
