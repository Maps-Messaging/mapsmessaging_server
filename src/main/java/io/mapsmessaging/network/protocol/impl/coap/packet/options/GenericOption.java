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

package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import lombok.Getter;

public class GenericOption extends Option {

  @Getter
  byte[] value;

  public GenericOption(int id){
    super(id);
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    for(byte b:value){
      int val = b & 0xff;
      if(val < 16){
        sb.append(" 0x0");
      }
      else{
        sb.append(" 0x");
      }
      sb.append(Integer.toHexString(val));
    }
    return super.toString()+"["+sb+"] <"+new String(value)+">";
  }

  @Override
  public void update(byte[] value) {
    this.value = value;
  }

  @Override
  public byte[] pack() {
    return value;
  }
}
