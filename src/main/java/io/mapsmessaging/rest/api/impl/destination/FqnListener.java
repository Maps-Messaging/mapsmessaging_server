package io.mapsmessaging.rest.api.impl.destination;

public interface FqnListener<T> {
  void onChange(String path, T value, ChangeType type);
}
