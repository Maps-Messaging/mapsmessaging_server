package io.mapsmessaging.rest.api.impl.destination;

import java.util.*;
import java.util.concurrent.*;

public class FqnListenerRegistry<T> {
  private final Map<String, CopyOnWriteArrayList<FqnListener<T>>> listeners = new ConcurrentHashMap<>();

  public void subscribe(String path, FqnListener<T> listener) {
    listeners.computeIfAbsent(path, k -> new CopyOnWriteArrayList<>()).add(listener);
  }

  public void unsubscribe(String path, FqnListener<T> listener) {
    List<FqnListener<T>> list = listeners.get(path);
    if (list != null) {
      list.remove(listener);
      if (list.isEmpty()) {
        listeners.remove(path);
      }
    }
  }

  public void notifyIfRegistered(String path, T value, ChangeType type) {
    List<FqnListener<T>> list = listeners.get(path);
    if (list != null) {
      for (FqnListener<T> listener : list) {
        listener.onChange(path, value, type);
      }
    }
  }
}

