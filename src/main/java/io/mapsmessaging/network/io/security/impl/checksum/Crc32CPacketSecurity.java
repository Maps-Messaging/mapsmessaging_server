/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.io.security.impl.checksum;

import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

public class Crc32CPacketSecurity extends ChecksumPacketSecurity {

  public Crc32CPacketSecurity() {
  }

  protected Crc32CPacketSecurity(SignatureManager stamper) {
    super(stamper);
  }

  @Override
  public PacketIntegrity initialise(SignatureManager stamper) {
    return new Crc32CPacketSecurity(stamper);
  }


  @Override
  public String getName() {
    return "CRC32C";
  }

  protected Checksum getChecksum() {
    return new CRC32C();
  }

}
