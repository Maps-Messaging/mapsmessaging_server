/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.ServerPublishPacket;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class MapsPublishFrame implements ServerPacket, ServerPublishPacket {

  private final String topic;
  private final int requestId;
  private final int flags;
  private final ByteBuffer[] messageBuffers;
  private Runnable callback;

  public MapsPublishFrame(String topic, Message message, int requestId, boolean ackRequired) throws IOException {
    this.topic = topic;
    this.requestId = requestId;
    flags = ackRequired ? MapsFrame.FLAG_ACK_REQUIRED : 0;
    messageBuffers = MessageFactory.getInstance().pack(message);
  }

  public MapsPublishFrame onComplete(Runnable callback) {
    this.callback = callback;
    return this;
  }

  @Override
  public Packet[] packAdvancedFrame(Packet packet) {
    byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
    int metadataLength = Integer.BYTES + topicBytes.length + Integer.BYTES + Integer.BYTES * messageBuffers.length;
    int bodyLength = metadataLength;
    for (ByteBuffer buffer : messageBuffers) bodyLength += buffer.remaining();

    ByteBuffer header = packet.getRawBuffer();
    MapsFrame.writeHeader(header, MapsFrame.VERSION_MAJOR, MapsFrame.VERSION_MINOR, MapsPacketType.PUBLISH, flags, bodyLength, requestId);
    header.putInt(topicBytes.length);
    header.put(topicBytes);
    header.putInt(messageBuffers.length);
    for (ByteBuffer buffer : messageBuffers) header.putInt(buffer.remaining());

    Packet[] packets = new Packet[messageBuffers.length + 1];
    packets[0] = packet;
    for (int i = 0; i < messageBuffers.length; i++) packets[i + 1] = new Packet(messageBuffers[i].duplicate());
    return packets;
  }

  @Override
  public int packFrame(Packet packet) {
    Packet[] parts = packAdvancedFrame(packet);
    int total = packet.position();
    for (int i = 1; i < parts.length; i++) {
      ByteBuffer source = parts[i].getRawBuffer().duplicate();
      total += source.remaining();
      packet.getRawBuffer().put(source);
    }
    return total;
  }

  @Override
  public void complete() {
    if (callback != null) callback.run();
  }

  @Override
  public SocketAddress getFromAddress() { return null; }
}
