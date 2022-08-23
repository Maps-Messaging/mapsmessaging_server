package io.mapsmessaging.network.protocol.impl.coap.blockwise;

public class SendController {


  private final SendPacket sendPacket;
  private int index;

  public SendController(byte[] buffer, int blockSize){
    sendPacket = new SendPacket(buffer, blockSize);
    index = 0;
  }

  public byte[] get(){
    return sendPacket.getBlock(index);
  }

  public int getBlockNumber(){
    return index;
  }

  public void ack(int block){
    if(block == index)  index++;
  }

  public boolean isLast(){
    return index+1 == sendPacket.getSize();
  }

  public boolean isComplete(){
    return index == sendPacket.getSize();
  }

}
