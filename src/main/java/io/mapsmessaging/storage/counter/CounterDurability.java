/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

/**
 * Durability policy for counter writes.
 *
 * STRICT: force each accepted update before returning.
 * BATCHED: acknowledge in-memory/file-cache writes before a durable barrier;
 *          recent accepted counters may roll back after power loss and MUST
 *          NOT be used for strict replay protection.
 * RESERVED_RANGE: reserve a durable high-water mark before issuing counters.
 */
public enum CounterDurability {
  STRICT,
  BATCHED,
  RESERVED_RANGE
}
