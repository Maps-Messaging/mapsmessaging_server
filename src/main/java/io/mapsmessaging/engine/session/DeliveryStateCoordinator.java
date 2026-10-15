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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.storage.delivery.PacketIdStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Serializes a session's journal ownership across offline cleanup and reconnect. */
public final class DeliveryStateCoordinator {
  @FunctionalInterface
  public interface StoreOpener {
    PacketIdStore open() throws IOException;
  }

  private Path path;
  private PacketIdStore liveStore;
  private int maximum = 65535;
  private ClientConnection clientConnection;

  public synchronized void setPath(Path path) {
    this.path = path;
  }

  public PacketIdStore attach(int maximum, StoreOpener opener) throws IOException {
    return attach(maximum, opener, null);
  }

  public synchronized PacketIdStore attach(int maximum, StoreOpener opener, ClientConnection connection) throws IOException {
    if (liveStore == null) {
      liveStore = opener.open();
      this.maximum = maximum;
      clientConnection = connection;
    }
    return liveStore;
  }

  public synchronized void close(PacketIdStore store) throws IOException {
    if (store == liveStore) {
      try {
        store.close();
      } finally {
        liveStore = null;
        clientConnection = null;
      }
    }
  }

  public synchronized boolean clearPending(String key, long messageId) throws IOException {
    return mutate(key, messageId);
  }

  public synchronized boolean clearAll(String key) throws IOException {
    return mutate(key, null);
  }

  public void retirePending(String key, long messageId) throws IOException {
    ClientConnection connection;
    boolean changed;
    synchronized (this) {
      changed = mutate(key, messageId);
      connection = clientConnection;
    }
    if (changed && connection != null) {
      connection.deliveryRetired(key, messageId);
    }
  }

  public void retireAll(String key) throws IOException {
    ClientConnection connection;
    List<PacketIdStore.Delivery> retired = new ArrayList<>();
    synchronized (this) {
      PacketIdStore store = liveStore;
      boolean temporary = store == null;
      if (temporary) {
        if (path == null || !Files.isRegularFile(path)) {
          return;
        }
        store = PacketIdStore.open(path, maximum, PacketIdStore.Durability.BATCHED);
      }
      try {
        synchronized (store) {
          for (PacketIdStore.Delivery delivery : store.outstanding().values()) {
            if (delivery.topic().equals(key)) {
              retired.add(delivery);
            }
          }
          if (store.clearDeliveries(key) > 0) {
            store.flush();
          }
        }
        connection = clientConnection;
      } finally {
        if (temporary) {
          store.close();
        }
      }
    }
    if (connection != null) {
      for (PacketIdStore.Delivery delivery : retired) {
        connection.deliveryRetired(delivery.topic(), delivery.messageId());
      }
    }
  }

  private boolean mutate(String key, Long messageId) throws IOException {
    PacketIdStore store = liveStore;
    boolean temporary = store == null;
    if (temporary) {
      if (path == null || !Files.isRegularFile(path)) {
        return false;
      }
      store = PacketIdStore.open(path, maximum, PacketIdStore.Durability.BATCHED);
    }
    try {
      boolean changed = messageId == null ? store.clearDeliveries(key) > 0 : store.clearPendingDelivery(key, messageId);
      if (changed) {
        store.flush();
      }
      return changed;
    } finally {
      if (temporary) {
          store.close();
        }
    }
  }
}
