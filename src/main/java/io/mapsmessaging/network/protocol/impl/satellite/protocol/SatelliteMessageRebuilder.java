/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SatelliteMessageRebuilder {

  private static final long TTL_MILLIS = 60_000; // adjust if needed

  private final Map<Integer, FragmentBuffer> buffers = new HashMap<>();

  public synchronized void clear() {
    buffers.clear();
  }

  public synchronized SatelliteMessage rebuild(SatelliteMessage message) {

    if(message == null ||
        message.getTotalPackets() == 0 ||
        message.getPacketNumber() < 0 ||
        message.getPacketNumber() >= message.getTotalPackets())
    {
      return null; // invalid fragment
    }

    if (message.isRaw() || message.getTotalPackets() <= 1) {
      return message;
    }

    purgeExpired();

    FragmentBuffer buffer = buffers.computeIfAbsent(
        message.getStreamNumber(),
        k -> new FragmentBuffer(message.getTotalPackets())
    );

    // sanity check: totalPackets mismatch means protocol corruption
    if (buffer.totalPackets != message.getTotalPackets()) {
      buffers.remove(message.getStreamNumber());
      return null;
    }

    // ignore duplicates
    buffer.fragments.putIfAbsent(message.getPacketNumber(), message);

    if (buffer.fragments.size() == buffer.totalPackets) {
      buffers.remove(message.getStreamNumber());
      return SatelliteMessageFactory.reconstructMessage(new ArrayList<>(buffer.fragments.values()));
    }

    return null;
  }

  private void purgeExpired() {
    long now = System.currentTimeMillis();
    buffers.entrySet().removeIf(e ->
        now - e.getValue().created > TTL_MILLIS
    );
  }

  private static class FragmentBuffer {
    final int totalPackets;
    final Map<Integer, SatelliteMessage> fragments = new HashMap<>();
    final long created = System.currentTimeMillis();

    FragmentBuffer(int totalPackets) {
      this.totalPackets = totalPackets;
    }
  }
}


