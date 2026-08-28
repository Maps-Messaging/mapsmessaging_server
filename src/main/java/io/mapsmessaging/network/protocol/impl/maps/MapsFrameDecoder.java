/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class MapsFrameDecoder {

  private final int maximumFrameSize;
  private final ByteBuffer header = ByteBuffer.allocate(MapsFrame.HEADER_SIZE);
  private ByteBuffer body;
  private byte major;
  private byte minor;
  private MapsPacketType type;
  private int flags;
  private int requestId;

  public MapsFrameDecoder(int maximumFrameSize) {
    if (maximumFrameSize < 1024) throw new IllegalArgumentException("maximumFrameSize must be at least 1024 bytes");
    this.maximumFrameSize = maximumFrameSize;
  }

  public List<MapsFrame> decode(ByteBuffer source) throws IOException {
    List<MapsFrame> frames = new ArrayList<>();
    while (source.hasRemaining()) {
      if (body == null) {
        transfer(source, header);
        if (header.hasRemaining()) break;
        initialiseBody(frames);
      }
      if (body != null) {
        transfer(source, body);
        if (!body.hasRemaining()) {
          body.flip();
          frames.add(new MapsFrame(major, minor, type, flags, requestId, body));
          body = null;
          header.clear();
        }
      }
    }
    return frames;
  }

  private void initialiseBody(List<MapsFrame> frames) throws IOException {
    header.flip();
    if (header.getInt() != MapsFrame.MAGIC) throw new IOException("Invalid MAPS protocol magic");
    major = header.get();
    minor = header.get();
    type = MapsPacketType.fromValue(Byte.toUnsignedInt(header.get()));
    flags = Byte.toUnsignedInt(header.get());
    int length = header.getInt();
    requestId = header.getInt();
    if (length < 0 || length > maximumFrameSize) throw new IOException("MAPS frame exceeds configured maximum: " + length);
    if (length == 0) {
      frames.add(new MapsFrame(major, minor, type, flags, requestId, ByteBuffer.allocate(0)));
      header.clear();
    } else {
      body = ByteBuffer.allocate(length);
    }
  }

  private static void transfer(ByteBuffer source, ByteBuffer destination) {
    int count = Math.min(source.remaining(), destination.remaining());
    if (count == 0) return;
    int oldLimit = source.limit();
    source.limit(source.position() + count);
    try {
      destination.put(source);
    } finally {
      source.limit(oldLimit);
    }
  }
}
