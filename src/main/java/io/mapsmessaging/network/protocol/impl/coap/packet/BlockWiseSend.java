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

  public BlockWiseSend(BasePacket rhs) {
    super(rhs.id, rhs.type, rhs.code, rhs.version, rhs.messageId, rhs.token);
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
    }
    optionSet.removeOption(BLOCK2);
    setPayload(sendController.get());
    index = sendController.getBlockNumber();
    getOptions().add(new Block(BLOCK2, index, !sendController.isLast(), sizePower));
    return super.packFrame(packet);
  }

  @Override
  public void sent(BasePacket ackResponse){
    sendController.ack(index);
    if(ackResponse.getOptions().hasOption(BLOCK2)){
      Block block = (Block) ackResponse.getOptions().getOption(BLOCK2);
      if(block.getSizeEx() != sizePower){
        sizePower = block.getSizeEx();
        sendController.resize(1<<(sizePower+4));
      }
    }
  }

  @Override
  public boolean isComplete(){
    if(sendController == null){
      return false;
    }
    return sendController.isComplete();
  }

}
