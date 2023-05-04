package io.mapsmessaging.engine.resources;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.storage.Statistics;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

public interface Resource extends AutoCloseable {

  @Override
  void close() throws IOException;

  void add(Message message) throws IOException;

  void keepOnly(List<Long> validKeys) throws IOException;

  void checkLoaded();

  long getNextIdentifier();

  void remove(long key) throws IOException;

  void delete() throws IOException;

  boolean isEmpty();

  Message get(long key) throws IOException;

  boolean contains(Long id) throws IOException;

  List<Long> getKeys() throws IOException;

  long size() throws IOException;

  @Nullable Statistics getStatistics();

  @SneakyThrows
  default <T> T getFromFuture(Future<T> future) {
    return future.get();
  }

  String getName();

  boolean isPersistent();

  ResourceProperties getResourceProperties();
}
