/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol.impl.coap.blockwise;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReceivePacket {

  private final List<byte[]> blocks;
  private final int blockSize;

  public ReceivePacket(int blockSize){
    blocks = new ArrayList<>();
    this.blockSize = 1 << ( blockSize +4);
  }

  public void add(int index, byte[] block){
    while(index > blocks.size()){
      blocks.add(new byte[0]);
    }
    blocks.add(index, block);
  }

  public byte[] getFull(){
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(blocks.size() * blockSize);
    for(byte[] block:blocks){
      try {
        byteArrayOutputStream.write(block);
      } catch (IOException e) {
        // we should be able to ignore this
      }
    }
    return byteArrayOutputStream.toByteArray();
  }
}
