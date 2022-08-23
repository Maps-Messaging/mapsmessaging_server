package io.mapsmessaging.network.protocol.impl.coap.blockwise;

import java.util.ArrayList;
import java.util.List;

public class SendPacket {

  private final int blockSize;
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
}
