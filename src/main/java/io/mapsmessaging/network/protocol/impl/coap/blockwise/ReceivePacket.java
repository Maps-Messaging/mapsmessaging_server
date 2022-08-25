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
