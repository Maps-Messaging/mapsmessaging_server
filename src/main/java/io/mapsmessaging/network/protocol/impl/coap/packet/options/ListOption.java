/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class ListOption extends Option{

  @Getter
  private final List<byte[]> list;

  public ListOption(int id) {
    super(id);
    list = new ArrayList<>();
  }

  @Override
  public void update(byte[] value) {
    list.add(value);
  }

  @Override
  public byte[] pack() {
    return new byte[0];
  }
}
