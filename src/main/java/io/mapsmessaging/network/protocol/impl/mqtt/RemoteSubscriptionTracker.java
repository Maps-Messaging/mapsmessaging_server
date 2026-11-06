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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Tracks the broker's result for every remote subscription in one connection attempt. */
public class RemoteSubscriptionTracker {
  private static final Logger logger = LoggerFactory.getLogger(RemoteSubscriptionTracker.class);
  private final PacketIdManager packetIds;
  private final Map<Integer, List<String>> pending = new HashMap<>();
  private final CompletableFuture<Void> completion = new CompletableFuture<>();
  private boolean sealed;

  public RemoteSubscriptionTracker(PacketIdManager packetIds) {
    this.packetIds = packetIds;
  }

  public synchronized int register(List<String> topics) {
    if (sealed) {
      throw new IllegalStateException("Remote subscription attempt is complete");
    }
    int packetId = packetIds.nextControlPacketIdentifier();
    pending.put(packetId, List.copyOf(topics));
    return packetId;
  }

  public synchronized void acknowledge(int packetId, int[] codes) {
    List<String> topics = pending.remove(packetId);
    if (topics == null) {
      return;
    }
    packetIds.completePacketId(packetId);
    if (codes.length != topics.size()) {
      completion.completeExceptionally(new IOException("SUBACK result count mismatch for packet " + packetId + " topics " + topics));
      return;
    }
    for (int index = 0; index < codes.length; index++) {
      int code = codes[index];
      if (code < 0 || code > 2) {
        String reason = String.format("0x%02X", code);
        logger.log(ServerLogMessages.MQTT_REMOTE_SUBSCRIPTION_REJECTED, topics.get(index), reason, packetId);
        completion.completeExceptionally(new IOException("SUBACK rejected topic " + topics.get(index) + " with code " + reason + " (packet " + packetId + ")"));
      }
    }
    if (sealed && pending.isEmpty()) {
      completion.complete(null);
    }
  }

  public synchronized CompletableFuture<Void> awaitAcknowledgements() {
    sealed = true;
    if (pending.isEmpty()) {
      completion.complete(null);
    }
    return completion;
  }

  public synchronized void close() {
    completion.completeExceptionally(new IOException("Connection closed before SUBACK completion for " + pending.values()));
    for (int packetId : pending.keySet()) {
      packetIds.completePacketId(packetId);
    }
    pending.clear();
  }
}
