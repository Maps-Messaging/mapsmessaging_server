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

package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;

@ToString
public class Block extends Option {

  @Getter
  @Setter
  private boolean more;

  @Getter
  @Setter
  private int number;


  @Getter
  @Setter
  private int sizeEx;

  public Block(int id){
    super(id);
  }

  public Block(int id, int number, boolean more, int sizeEx){
    super(id);
    this.number = number;
    this.more = more;
    this.sizeEx = sizeEx;
  }

  @Override
  public void update(byte[] packed) throws IOException{
    if(packed == null || packed.length == 0){
      sizeEx =0;
      number = 0;
      more = false;
    }
    else {
      byte last = packed[packed.length - 1]; // Get the flags from the end
      sizeEx = last & 0b111;
      more = (last & 0b01000) != 0;
      number = (last & 0xff) >> 4;
      for (int x = 1; x < packed.length; x++) {
        number += ((packed[packed.length - x - 1] & 0xff) << (x * 8 - 4));
      }
    }
  }

  @Override
  public byte[] pack() {
    int t = number << 4;
    if(more){
      t =  t | 0b1000;
    }
    t = t | (sizeEx & 0b111);

    byte[] buf;
    if(number < 16 ){
      buf = new byte[1];
    }
    else if(number < 4096 ){
      buf = new byte[2];
    }
    else{
      buf = new byte[3];
    }
    for(int x=buf.length-1;x>-1;x--){
      buf[x] = (byte)(t & 0xff);
      t = t >> 8;
    }
    return buf;
  }
}
