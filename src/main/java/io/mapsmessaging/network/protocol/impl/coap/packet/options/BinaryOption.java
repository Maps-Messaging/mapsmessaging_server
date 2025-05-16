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

import java.io.IOException;

public class BinaryOption extends Option {

  @Getter
  @Setter
  protected long value = 0;

  public BinaryOption(int id) {
    super(id);
  }

  @Override
  public void update(byte[] data) throws IOException {
    value = 0; // reset it
    for (int x = 0; x < data.length; x++) {
      value += (long) (data[(data.length - x) - 1] & 0xFF) << (x * 8);
    }
  }

  @Override
  public byte[] pack() {
    long t = value;
    int x = 0;
    byte[] buffer = new byte[8];
    int idx=7;
    while(t != 0){
      buffer[idx-x] = (byte)(t & 0xff);
      t = t >> 8;
      x++;
    }
    byte[] response = new byte[x];
    System.arraycopy(buffer, 8-x, response, 0, x);
    return response;
  }
}
