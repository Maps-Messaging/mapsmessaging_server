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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PackedOption extends Option {

  @Getter
  private final List<String> path;
  private final String delimiter;


  public PackedOption(int id, String delimiter) {
    super(id);
    path = new ArrayList<>();
    this.delimiter = delimiter;
  }

  public void add(String part) {
    path.add(part);
  }

  public void setPath(String completePath) {
    path.clear();
    path.addAll(Arrays.asList(completePath.split(delimiter)));
  }

  public void update(byte[] data) {
    path.add(new String(data));
  }

  @Override
  public byte[] pack() {
    return new byte[0];
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String uri : path) {
      if (!first) {
        sb.append(delimiter);
      }
      first = false;
      sb.append(uri);
    }
    return sb.toString();
  }
}

