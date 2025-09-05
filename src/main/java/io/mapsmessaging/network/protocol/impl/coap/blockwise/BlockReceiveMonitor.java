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

package io.mapsmessaging.network.protocol.impl.coap.blockwise;

import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BlockReceiveMonitor {

  private final Map<String, BlockReceiveState> blockBasedPackets = new LinkedHashMap<>();

  public synchronized BlockReceiveState registerOrGet(Block block, String path) {
    BlockReceiveState state = blockBasedPackets.computeIfAbsent(path, k -> new BlockReceiveState(new ReceivePacket(block.getSizeEx())));
    state.setLastAccess(System.currentTimeMillis());
    return state;
  }

  public synchronized void complete(String path) {
    blockBasedPackets.remove(path);
  }

  public void scanForIdle() {
    long expired = System.currentTimeMillis() + 30_000;
    List<String> expiredList = new ArrayList<>();
    for (Entry<String, BlockReceiveState> entry : blockBasedPackets.entrySet()) {
      if (entry.getValue().getLastAccess() < expired) {
        expiredList.add(entry.getKey());
      }
    }
    for (String key : expiredList) {
      blockBasedPackets.remove(key);
    }
  }


}
