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

package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.SendController;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.BLOCK2;

public class BlockWiseSend extends BasePacket {

  private int blockSize;
  private int sizePower;
  private SendController sendController;
  private int index;

  public BlockWiseSend(BasePacket rhs) {
    super(rhs.id, rhs.type, rhs.code, rhs.version, rhs.messageId, rhs.token);
  }

  public void setBlockNumber(int blockNumber){
    if(sendController == null) {
      sendController = new SendController(getPayload(), blockSize);
    }
    sendController.setBlockNumber(blockNumber);
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
    return true;
  }

}
