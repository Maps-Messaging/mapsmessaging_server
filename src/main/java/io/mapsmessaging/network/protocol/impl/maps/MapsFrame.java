/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class MapsFrame implements ServerPacket {

  public static final int MAGIC = 0x4d415053;
  public static final int HEADER_SIZE = 16;
  public static final byte VERSION_MAJOR = 1;
  public static final byte VERSION_MINOR = 0;
  public static final int FLAG_ACK_REQUIRED = 0x01;
  public static final int FLAG_DUPLICATE = 0x02;

  private final byte major;
  private final byte minor;
  private final MapsPacketType type;
  private final int flags;
  private final int requestId;
  private final ByteBuffer body;
  private Runnable callback;

  public MapsFrame(MapsPacketType type, int flags, int requestId, ByteBuffer body) {
    this(VERSION_MAJOR, VERSION_MINOR, type, flags, requestId, body);
  }

  public MapsFrame(byte major, byte minor, MapsPacketType type, int flags, int requestId, ByteBuffer body) {
    this.major = major;
    this.minor = minor;
    this.type = type;
    this.flags = flags;
    this.requestId = requestId;
    this.body = body == null ? ByteBuffer.allocate(0) : body.asReadOnlyBuffer();
  }

  public byte major() { return major; }
  public byte minor() { return minor; }
  public MapsPacketType type() { return type; }
  public int flags() { return flags; }
  public int requestId() { return requestId; }
  public ByteBuffer body() { return body.asReadOnlyBuffer(); }
  public boolean ackRequired() { return (flags & FLAG_ACK_REQUIRED) != 0; }

  public MapsFrame onComplete(Runnable callback) {
    this.callback = callback;
    return this;
  }

  @Override
  public int packFrame(Packet packet) {
    ByteBuffer raw = packet.getRawBuffer();
    ByteBuffer payload = body();
    int start = raw.position();
    writeHeader(raw, major, minor, type, flags, payload.remaining(), requestId);
    raw.put(payload);
    return raw.position() - start;
  }

  public static void writeHeader(ByteBuffer target, byte major, byte minor, MapsPacketType type, int flags, int bodyLength, int requestId) {
    target.putInt(MAGIC);
    target.put(major);
    target.put(minor);
    target.put((byte) type.value());
    target.put((byte) flags);
    target.putInt(bodyLength);
    target.putInt(requestId);
  }

  @Override
  public void complete() {
    if (callback != null) callback.run();
  }

  @Override
  public SocketAddress getFromAddress() { return null; }
}
