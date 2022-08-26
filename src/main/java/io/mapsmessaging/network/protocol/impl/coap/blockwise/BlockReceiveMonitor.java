package io.mapsmessaging.network.protocol.impl.coap.blockwise;

import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BlockReceiveMonitor {

  private final Map<String, BlockReceiveState> blockBasedPackets = new LinkedHashMap<>();

  public BlockReceiveMonitor() {

  }

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
