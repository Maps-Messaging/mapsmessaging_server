package io.mapsmessaging.network.io.impl.udp.session;

import io.mapsmessaging.network.io.Timeoutable;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
