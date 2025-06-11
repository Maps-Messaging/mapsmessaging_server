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

package io.mapsmessaging.network.io.impl.udp.session;

import io.mapsmessaging.network.io.Timeoutable;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class UDPSessionManager<T extends Timeoutable> {

  private final long timeout;

  private final Map<SocketAddress, UDPSessionState<T>> sessionStateMap;
  private final ScheduledFuture<?> monitor;

  public UDPSessionManager(long timeout) {
    sessionStateMap = new ConcurrentHashMap<>();
    this.timeout = TimeUnit.SECONDS.toMillis(timeout);
    monitor = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new StateTimeoutMonitor<T>(this), 10, 10, TimeUnit.SECONDS);
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

  public void addState(@NotNull @NonNull SocketAddress address, @NotNull @NonNull UDPSessionState<T> state) {
    sessionStateMap.put(address, state);
    state.updateTimeout();
  }

  public @Nullable UDPSessionState<T> getState(@NotNull @NonNull SocketAddress address) {
    UDPSessionState<T> response = sessionStateMap.get(address);
    if (response != null) {
      response.updateTimeout();
    }
    return response;
  }

  public UDPSessionState<T> findAndUpdate(String clientId, SocketAddress updatedAddress, boolean enableAddressChange){
    for(Entry<SocketAddress, UDPSessionState<T>> entry:sessionStateMap.entrySet()){
      String lookupId = entry.getValue().getClientIdentifier();
      if(lookupId != null && lookupId.equals(clientId)){
        // We have found a matching client ID, but lets see if the address changes and we allow this
        SocketAddress socketAddress = entry.getKey();
        boolean allowChange = false;
        if(socketAddress instanceof InetSocketAddress && updatedAddress instanceof InetSocketAddress){
          InetSocketAddress inetSocketAddress = (InetSocketAddress )socketAddress;
          InetSocketAddress inetUpdateAddress = (InetSocketAddress )updatedAddress;
          allowChange = enableAddressChange || inetSocketAddress.getHostName().equals(inetUpdateAddress.getHostName());
        }
        if(allowChange) {
          sessionStateMap.remove(entry.getKey());
          sessionStateMap.put(updatedAddress, entry.getValue());
          return entry.getValue();
        }
        break;
      }
    }
    return null;
  }

  public void deleteState(@NotNull @NonNull SocketAddress address) {
    sessionStateMap.remove(address);
  }

  protected void scanForTimeouts() {
    if (!sessionStateMap.isEmpty()) {
      List<SocketAddress> expiredKeys = new ArrayList<>();
      createExpiredList(expiredKeys);
      processExpiredSessions(expiredKeys);
    }
  }

  private void createExpiredList(List<SocketAddress> expiredKeys){
    long now = System.currentTimeMillis();
    for (Entry<SocketAddress, UDPSessionState<T>> entry : sessionStateMap.entrySet()) {
      long expiry = now - timeout;
      if (entry.getValue().getContext().getTimeOut() != 0) {
        expiry = now - entry.getValue().getContext().getTimeOut();
      }
      if (entry.getValue().getGetLastAccess() < expiry) {
        expiredKeys.add(entry.getKey());
      }
    }

  }

  private void processExpiredSessions( List<SocketAddress> expiredKeys){
    for (SocketAddress key : expiredKeys) {
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
