package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.BLOCK2;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.SendController;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;

public class BlockWiseSend extends BasePacket {

  private int blockSize;
  private int sizePower;
  private SendController sendController;
  private int index;

  public BlockWiseSend(int id, TYPE type, Code code, int version, int messageId, byte[] token) {
    super(id, type, code, version, messageId, token);
  }

  public void setBlockSize(int size){
    blockSize = size;
    sizePower = -1;
    int power = 4;
    while (sizePower== -1 && power < 11){
      int t = 1<< power;
      if(size == t){
        sizePower = power -4;
      }
      power++;
    }
  }

  @Override
  public int packFrame(Packet packet) {
    OptionSet optionSet = getOptions();
    if(sendController == null){
      sendController = new SendController(getPayload(), blockSize);
      optionSet.removeOption(BLOCK2);
    }
    else{
      getOptions().clearAll(); // Just send the block
    }
    setPayload(sendController.get());
    index = sendController.getBlockNumber();
    getOptions().add(new Block(BLOCK2, index, !sendController.isLast(), sizePower));
    return super.packFrame(packet);
  }

  @Override
  public void sent(){
    sendController.ack(index);
  }

  @Override
  public boolean isComplete(){
    return !sendController.isComplete();
  }

}
