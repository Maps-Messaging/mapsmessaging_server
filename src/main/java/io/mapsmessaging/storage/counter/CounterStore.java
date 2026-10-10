/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import java.io.IOException;

/**
 * Protocol-independent, durable monotonic counters.
 * Strict and reserved-range operations require durable commit before success.
 * BATCHED operations can lose acknowledged updates after power failure.
 */
public interface CounterStore extends AutoCloseable {
  /**
   * Atomically accept a strictly increasing inbound counter.
   * False denotes an old/replayed value; errors fail closed.
   */
  boolean accept(String key, long value) throws IOException;

  /**
   * Reserve a contiguous range, durably advancing the high-water mark
   * before returning it. Unused reservations are never reused after restart.
   */
  Range reserve(String key, int count) throws IOException;

  /** Read the highest committed value, or zero for a new key. */
  long highWaterMark(String key) throws IOException;

  /** Force pending updates to durable storage. */
  void flush() throws IOException;

  record Range(long first, long last) {}

  @Override
  void close() throws IOException;
}
