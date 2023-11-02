/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

  public void setBlockNumber(int blockNumber){
    index = blockNumber;
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

  public void resize(int newSize) {
    sendPacket.resize(newSize, index);
  }
}
