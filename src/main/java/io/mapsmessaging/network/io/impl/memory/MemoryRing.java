/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 */

package io.mapsmessaging.network.io.impl.memory;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;

public final class MemoryRing {

  private static final int SLOT_HEADER_SIZE = Integer.BYTES;
  private static final VarHandle LONG_HANDLE = ValueLayout.JAVA_LONG.varHandle();

  private final MemorySegment memory;
  private final long producerOffset;
  private final long consumerOffset;
  private final long dataOffset;
  private final int slotSize;
  private final int slotCount;
  private final int payloadSize;

  public MemoryRing(MemorySegment memory, long producerOffset, long consumerOffset, long dataOffset, int slotSize, int slotCount) {
    if (slotSize <= SLOT_HEADER_SIZE) {
      throw new IllegalArgumentException("slotSize must be greater than " + SLOT_HEADER_SIZE);
    }
    if (slotCount < 2) {
      throw new IllegalArgumentException("slotCount must be at least 2");
    }
    this.memory = memory;
    this.producerOffset = producerOffset;
    this.consumerOffset = consumerOffset;
    this.dataOffset = dataOffset;
    this.slotSize = slotSize;
    this.slotCount = slotCount;
    payloadSize = slotSize - SLOT_HEADER_SIZE;
  }

  public boolean hasData() {
    return producerAcquire() != consumerAcquire();
  }

  public int write(ByteBuffer source) {
    int written = 0;
    while (source.hasRemaining()) {
      long producer = producerAcquire();
      long consumer = consumerAcquire();
      if (producer - consumer >= slotCount) {
        break;
      }

      int length = Math.min(payloadSize, source.remaining());
      long slotOffset = slotOffset(producer);
      copyFromByteBuffer(source, memory.asSlice(slotOffset + SLOT_HEADER_SIZE, length).asByteBuffer(), length);
      memory.set(ValueLayout.JAVA_INT, slotOffset, length);
      producerRelease(producer + 1);
      written += length;
    }
    return written;
  }

  public int read(ByteBuffer destination) {
    int read = 0;
    while (destination.hasRemaining()) {
      long consumer = consumerAcquire();
      long producer = producerAcquire();
      if (consumer == producer) {
        break;
      }

      long slotOffset = slotOffset(consumer);
      int length = memory.get(ValueLayout.JAVA_INT, slotOffset);
      if (length < 0 || length > payloadSize) {
        throw new IllegalStateException("Invalid shared memory slot length " + length);
      }
      if (length > destination.remaining()) {
        break;
      }

      copyToByteBuffer(memory.asSlice(slotOffset + SLOT_HEADER_SIZE, length).asByteBuffer(), destination, length);
      consumerRelease(consumer + 1);
      read += length;
    }
    return read;
  }

  public long availableSlots() {
    return slotCount - (producerAcquire() - consumerAcquire());
  }

  private long slotOffset(long sequence) {
    return dataOffset + (sequence % slotCount) * slotSize;
  }

  private long producerAcquire() {
    return (long) LONG_HANDLE.getAcquire(memory, producerOffset);
  }

  private long consumerAcquire() {
    return (long) LONG_HANDLE.getAcquire(memory, consumerOffset);
  }

  private void producerRelease(long value) {
    LONG_HANDLE.setRelease(memory, producerOffset, value);
  }

  private void consumerRelease(long value) {
    LONG_HANDLE.setRelease(memory, consumerOffset, value);
  }

  private static void copyFromByteBuffer(ByteBuffer source, ByteBuffer destination, int length) {
    int originalLimit = source.limit();
    source.limit(source.position() + length);
    try {
      destination.put(source);
    } finally {
      source.limit(originalLimit);
    }
  }

  private static void copyToByteBuffer(ByteBuffer source, ByteBuffer destination, int length) {
    int originalLimit = source.limit();
    source.limit(source.position() + length);
    try {
      destination.put(source);
    } finally {
      source.limit(originalLimit);
    }
  }
}
