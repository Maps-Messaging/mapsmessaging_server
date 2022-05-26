package io.mapsmessaging.network.io.impl.udp.session;

import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.io.Closeable;
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

public class UDPSessionManager<T extends Closeable> {

  private final long timeout;

  private final Map<SocketAddress, UDPSessionState<T>> sessionStateMap;
  private final ScheduledFuture<?> monitor;

  public UDPSessionManager(long timeout){
    sessionStateMap = new ConcurrentHashMap<>();
    this.timeout = timeout;
    monitor = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new StateTimeoutMonitor<T>(this), timeout, timeout, TimeUnit.SECONDS);
  }

  public void close(){
    monitor.cancel(false);
    List<UDPSessionState<T>> closingSessions = new ArrayList<>(sessionStateMap.values());
    for(UDPSessionState<T> state:closingSessions){
      if(state.getContext() != null){
        try {
          state.getContext().close();
        } catch (IOException e) {
          //
        }
      }
    }
    sessionStateMap.clear();
  }

  public void addState(@NotNull @NonNull SocketAddress address,@NotNull @NonNull UDPSessionState<T> state){
    sessionStateMap.put(address, state);
    state.updateTimeout();
  }

  public @Nullable UDPSessionState<T> getState(@NotNull @NonNull SocketAddress address){
    UDPSessionState<T> response = sessionStateMap.get(address);
    if(response != null){
      response.updateTimeout();
    }
    return response;
  }

  public void deleteState(@NotNull @NonNull SocketAddress address){
    sessionStateMap.remove(address);
  }

  protected void scanForTimeouts(){
    List<SocketAddress> expiredKeys = new ArrayList<>();
    if(!sessionStateMap.isEmpty()) {
      long expiryTime = System.currentTimeMillis() - timeout;
      for (Entry<SocketAddress, UDPSessionState<T>> entry : sessionStateMap.entrySet()) {
        if (entry.getValue().getTimeout() < expiryTime) {
          expiredKeys.add(entry.getKey());
        }
      }
      for (SocketAddress key : expiredKeys) {
        UDPSessionState<T> state = sessionStateMap.get(key);
        sessionStateMap.remove(key);
        if(state.getContext() != null){
          try {
            state.getContext().close();
          } catch (IOException e) {
            //
          }
        }
      }
    }
  }
}
