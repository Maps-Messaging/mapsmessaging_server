/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.network.io.security;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Timeoutable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface PacketIntegrity extends Timeoutable {

  PacketIntegrity initialise(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException;

  String getName();

  boolean isSecure(Packet packet);

  boolean isSecure(Packet packet, int offset, int length);

  Packet secure(Packet packet);

  Packet secure(Packet packet, int offset, int length);

  int size();

  void reset();

  @Override
  default void close() {
  }

}
