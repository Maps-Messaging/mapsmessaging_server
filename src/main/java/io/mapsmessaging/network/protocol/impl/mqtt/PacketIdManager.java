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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.SubscribedEventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PacketIdManager {

  private final Map<Integer, PacketIdentifierMap> outstandingPacketId;

  private int packetId;

  public PacketIdManager() {
    outstandingPacketId = new TreeMap<>();
  }

  public void close() {
    outstandingPacketId.clear();
  }

  public synchronized int nextPacketIdentifier(SubscribedEventManager subscription, long messageId) {
    int retVal = nextPacketIdentifier();
    PacketIdentifierMap state = new PacketIdentifierMap(retVal, subscription, messageId);
    outstandingPacketId.put(retVal, state);
    return retVal;
  }

  public synchronized int nextPacketIdentifier() {
    int retVal = 0;
    while (retVal == 0) {
      retVal = (packetId++) & 0xffff;
      if (outstandingPacketId.containsKey(retVal)) {
        retVal = 0;
      }
    }
    return retVal;
  }

  public synchronized PacketIdentifierMap receivedPacket(int id) {
    return outstandingPacketId.get(id);
  }

  public synchronized PacketIdentifierMap completePacketId(int id) {
    return outstandingPacketId.remove(id);
  }

  public synchronized int size() {
    return outstandingPacketId.size();
  }

  public boolean scanForTimeOut() {
    long timeout = System.currentTimeMillis() - 20000;
    List<PacketIdentifierMap> tmp = new ArrayList<>(outstandingPacketId.values());
    return tmp.stream().anyMatch(map -> map.getTime() < timeout);
  }
}
