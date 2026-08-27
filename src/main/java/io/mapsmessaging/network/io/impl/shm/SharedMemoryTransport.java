/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 */

package io.mapsmessaging.network.io.impl.shm;

import io.mapsmessaging.network.io.impl.memory.MemoryRing;
import io.mapsmessaging.network.io.impl.memory.MemoryTransport;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public final class SharedMemoryTransport implements MemoryTransport {

  private static final int MAGIC = 0x4d415053;
  private static final int VERSION = 1;
  private static final long HEADER_SIZE = 128;
  private static final long MAGIC_OFFSET = 0;
  private static final long VERSION_OFFSET = 4;
  private static final long SLOT_SIZE_OFFSET = 8;
  private static final long SLOT_COUNT_OFFSET = 12;
  private static final long A_PRODUCER_OFFSET = 16;
  private static final long A_CONSUMER_OFFSET = 24;
  private static final long B_PRODUCER_OFFSET = 32;
  private static final long B_CONSUMER_OFFSET = 40;

  private final Arena arena;
  private final FileChannel channel;
  private final MemorySegment memory;
  private final MemoryRing transmitRing;
  private final MemoryRing receiveRing;
  private final Path path;

  public SharedMemoryTransport(String name, boolean sideA, int slotSize, int slotCount) throws IOException {
    if (slotSize < 256) {
      throw new IllegalArgumentException("slotSize must be at least 256 bytes");
    }
    if (slotCount < 2) {
      throw new IllegalArgumentException("slotCount must be at least 2");
    }

    path = resolvePath(name);
    Files.createDirectories(path.getParent());
    long ringSize = (long) slotSize * slotCount;
    long regionSize = HEADER_SIZE + ringSize * 2;

    channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    initialise(channel, regionSize, slotSize, slotCount);
    arena = Arena.ofShared();
    memory = channel.map(FileChannel.MapMode.READ_WRITE, 0, regionSize, arena);
    validate(memory, slotSize, slotCount);

    long aDataOffset = HEADER_SIZE;
    long bDataOffset = HEADER_SIZE + ringSize;
    MemoryRing aToB = new MemoryRing(memory, A_PRODUCER_OFFSET, A_CONSUMER_OFFSET, aDataOffset, slotSize, slotCount);
    MemoryRing bToA = new MemoryRing(memory, B_PRODUCER_OFFSET, B_CONSUMER_OFFSET, bDataOffset, slotSize, slotCount);
    transmitRing = sideA ? aToB : bToA;
    receiveRing = sideA ? bToA : aToB;
  }

  @Override
  public int write(ByteBuffer source) {
    return transmitRing.write(source);
  }

  @Override
  public int read(ByteBuffer destination) {
    return receiveRing.read(destination);
  }

  @Override
  public boolean hasData() {
    return receiveRing.hasData();
  }

  @Override
  public String remoteAddress() {
    return "shm:" + path;
  }

  @Override
  public void close() throws IOException {
    arena.close();
    channel.close();
  }

  private static void initialise(FileChannel channel, long regionSize, int slotSize, int slotCount) throws IOException {
    try (var ignored = channel.lock()) {
      if (channel.size() != regionSize) {
        channel.truncate(0);
        channel.position(regionSize - 1);
        channel.write(ByteBuffer.wrap(new byte[] {0}));
      }
      try (Arena initArena = Arena.ofConfined()) {
        MemorySegment segment = channel.map(FileChannel.MapMode.READ_WRITE, 0, regionSize, initArena);
        int magic = segment.get(ValueLayout.JAVA_INT, MAGIC_OFFSET);
        if (magic != MAGIC) {
          segment.fill((byte) 0);
          segment.set(ValueLayout.JAVA_INT, MAGIC_OFFSET, MAGIC);
          segment.set(ValueLayout.JAVA_INT, VERSION_OFFSET, VERSION);
          segment.set(ValueLayout.JAVA_INT, SLOT_SIZE_OFFSET, slotSize);
          segment.set(ValueLayout.JAVA_INT, SLOT_COUNT_OFFSET, slotCount);
          segment.force();
        }
      }
    }
  }

  private static void validate(MemorySegment segment, int slotSize, int slotCount) throws IOException {
    int magic = segment.get(ValueLayout.JAVA_INT, MAGIC_OFFSET);
    int version = segment.get(ValueLayout.JAVA_INT, VERSION_OFFSET);
    int configuredSlotSize = segment.get(ValueLayout.JAVA_INT, SLOT_SIZE_OFFSET);
    int configuredSlotCount = segment.get(ValueLayout.JAVA_INT, SLOT_COUNT_OFFSET);
    if (magic != MAGIC || version != VERSION || configuredSlotSize != slotSize || configuredSlotCount != slotCount) {
      throw new IOException("Shared memory region layout does not match requested configuration");
    }
  }

  private static Path resolvePath(String name) {
    String safeName = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    Path sharedMemory = Path.of("/dev/shm");
    Path base = Files.isDirectory(sharedMemory) ? sharedMemory.resolve("mapsmessaging") : Path.of(System.getProperty("java.io.tmpdir"), "mapsmessaging-shm");
    return base.resolve(safeName + ".shm");
  }
}
