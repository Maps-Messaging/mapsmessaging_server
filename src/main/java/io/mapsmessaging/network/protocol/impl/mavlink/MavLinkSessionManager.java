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

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.network.io.Timeoutable;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MavLinkSessionManager <T extends Timeoutable> {

  private final long timeout;

  private final Map<MavlinkDeviceKey, UDPSessionState<T>> sessionStateMap;
  private final ScheduledFuture<?> monitor;

  public MavLinkSessionManager(long timeout) {
    sessionStateMap = new ConcurrentHashMap<>();
    this.timeout = TimeUnit.SECONDS.toMillis(timeout);
    monitor = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this::scanForTimeouts, 10, 10, TimeUnit.SECONDS);
  }

  public void close() {
    monitor.cancel(false);
    List<UDPSessionState<T>> closingSessions = new ArrayList<>(sessionStateMap.values());
    for (UDPSessionState<T> state : closingSessions) {
      if (state.getContext() != null) {
        try {
          state.getContext().close();
        } catch (IOException e) {
          //
        }
      }
    }
    sessionStateMap.clear();
  }

  public void addState(@NotNull @NonNull MavlinkDeviceKey key, @NotNull @NonNull UDPSessionState<T> state) {
    sessionStateMap.put(key, state);
    state.updateTimeout();
  }

  public @Nullable UDPSessionState<T> getState(@NotNull @NonNull MavlinkDeviceKey key) {
    UDPSessionState<T> response = sessionStateMap.get(key);
    if (response != null) {
      response.updateTimeout();
    }
    return response;
  }

  public void deleteState(@NotNull @NonNull MavlinkDeviceKey key) {
    sessionStateMap.remove(key);
  }

  protected void scanForTimeouts() {
    if (!sessionStateMap.isEmpty()) {
      List<MavlinkDeviceKey> expiredKeys = new ArrayList<>();
      createExpiredList(expiredKeys);
      processExpiredSessions(expiredKeys);
    }
  }

  private void createExpiredList(List<MavlinkDeviceKey> expiredKeys){
    long now = System.currentTimeMillis();
    for (Map.Entry<MavlinkDeviceKey, UDPSessionState<T>> entry : sessionStateMap.entrySet()) {
      long expiry = now - timeout;
      if (entry.getValue().getContext().getTimeOut() != 0) {
        expiry = now - entry.getValue().getContext().getTimeOut();
      }
      if (entry.getValue().getGetLastAccess() < expiry) {
        expiredKeys.add(entry.getKey());
      }
    }

  }

  private void processExpiredSessions( List<MavlinkDeviceKey> expiredKeys){
    for (MavlinkDeviceKey key : expiredKeys) {
      UDPSessionState<T> state = sessionStateMap.get(key);
      sessionStateMap.remove(key);
      if (state.getContext() != null) {
        try {
          state.getContext().close();
        } catch (IOException e) {
          //
        }
      }
    }

  }
}
