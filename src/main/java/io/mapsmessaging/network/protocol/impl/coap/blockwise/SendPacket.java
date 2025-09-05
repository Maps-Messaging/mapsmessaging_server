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

package io.mapsmessaging.network.protocol.impl.coap.blockwise;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SendPacket {

  private int blockSize;
  private final List<byte[]> blocks;

  public SendPacket(byte[] full, int blockSize){
    this.blockSize = blockSize;
    blocks = new ArrayList<>();
    packList(full);
  }

  public byte[] getBlock(int index){
    if(index >= blocks.size()){
      throw new IndexOutOfBoundsException("Block number exceeded size");
    }
    return blocks.get(index);
  }

  public int getSize(){
    return blocks.size();
  }

  private void packList(byte[] data){
    int pos =0;
    while(pos < data.length){
      byte[] block;
      if(pos+blockSize < data.length){
        block = new byte[blockSize];
      }
      else{
        block = new byte[(data.length - pos)];
      }
      System.arraycopy(data, pos, block, 0, block.length);
      blocks.add(block);
      pos += block.length;
    }
  }

  // Remote client has requested a change to the buffer size, so we need to
  // a) Rebuild the complete byte[]
  // b) update the block size
  // c) Save the buffers we HAVE sent, so we can add them to the end
  // d) rebuild the blocks FROM the start of the next index
  // e) pad the start of the list with the buffers already sent to match what we have already sent
  public void resize(int newSize, int index) {
    //a)
    ByteArrayOutputStream tmp = new ByteArrayOutputStream(1024);
    for(byte[] buf:blocks){
      try {
        tmp.write(buf);
      } catch (IOException e) {
        //
      }
    }
    byte[] full = tmp.toByteArray();
    int offset = blockSize * index;

    //b)
    blockSize = newSize;

    //c)
    List<byte[]> sentBlocks = new ArrayList<>();
    for(int x=0;x<index;x++){
      sentBlocks.add(blocks.get(x));
    }

    //d)
    byte[] remaining = new byte[(full.length - offset)];
    System.arraycopy(full, offset, remaining, 0, remaining.length);
    packList(remaining);

    // e)
    for(int x=0;x<index;x++){
      blocks.add(sentBlocks.get(x));
    }
  }
}
