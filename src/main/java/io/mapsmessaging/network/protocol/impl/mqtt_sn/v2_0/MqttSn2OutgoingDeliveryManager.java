/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2AckCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PublishCodec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Tracks the server's single outstanding QoS 1/2 request as required by CSD01. */
public final class MqttSn2OutgoingDeliveryManager {

  private enum Stage { WAIT_PUBACK, WAIT_PUBREC, WAIT_PUBCOMP }

  private record Delivery(String topic, byte[] payload, int qos, boolean retain, Runnable completion) {
    private Delivery {
      payload = payload.clone();
    }
  }

  private static final long DEFAULT_RETRY_INTERVAL_MILLIS = 10_000;
  private static final int DEFAULT_MAXIMUM_RETRIES = 3;

  private final Deque<Delivery> pending = new ArrayDeque<>();
  private final long retryIntervalMillis;
  private final int maximumRetries;
  private int nextPacketIdentifier = 1;
  private InFlight inFlight;

  private static final class InFlight {
    private final Delivery delivery;
    private final int packetIdentifier;
    private Stage stage;
    private ByteBuffer lastFrame;
    private long lastSentAt;
    private int retries;
    private boolean retryExhausted;

    private InFlight(Delivery delivery, int packetIdentifier, long now) {
      this.delivery = delivery;
      this.packetIdentifier = packetIdentifier;
      stage = delivery.qos() == 1 ? Stage.WAIT_PUBACK : Stage.WAIT_PUBREC;
      lastSentAt = now;
      lastFrame = encodePublish(delivery, packetIdentifier);
    }
  }

  public MqttSn2OutgoingDeliveryManager() {
    this(DEFAULT_RETRY_INTERVAL_MILLIS, DEFAULT_MAXIMUM_RETRIES);
  }

  public MqttSn2OutgoingDeliveryManager(long retryIntervalMillis, int maximumRetries) {
    if (retryIntervalMillis < 1 || maximumRetries < 0) {
      throw new IllegalArgumentException("Invalid MQTT-SN retry configuration");
    }
    this.retryIntervalMillis = retryIntervalMillis;
    this.maximumRetries = maximumRetries;
  }

  /** Returns a PUBLISH frame when the new delivery can start immediately. */
  public synchronized ByteBuffer enqueue(String topic, byte[] payload, int qos, boolean retain,
      Runnable completion, long now) {
    if (qos < 1 || qos > 2) {
      throw new IllegalArgumentException("Only QoS 1 and 2 use the acknowledgement flow");
    }
    Delivery delivery = new Delivery(Objects.requireNonNull(topic, "topic"),
        Objects.requireNonNull(payload, "payload"), qos, retain, completion);
    if (inFlight != null) {
      pending.addLast(delivery);
      return null;
    }
    return start(delivery, now);
  }

  /** Applies a peer acknowledgement and starts the next queued delivery when possible. */
  public ByteBuffer acknowledge(MqttSn2AckCodec.Ack ack, long now) throws IOException {
    Runnable completion = null;
    ByteBuffer response;
    synchronized (this) {
      if (inFlight == null || ack.packetIdentifier() != inFlight.packetIdentifier) {
        throw new IOException("Unknown MQTT-SN 2.0 outgoing Packet Identifier");
      }
      if (inFlight.stage == Stage.WAIT_PUBACK && ack.type() == MqttSn2PacketType.PUBACK) {
        completion = finishCurrent();
        response = startNext(now);
      } else if (inFlight.stage == Stage.WAIT_PUBREC && ack.type() == MqttSn2PacketType.PUBREC) {
        if (ack.reasonCode() != null && ack.reasonCode() >= 0x80) {
          completion = finishCurrent();
          response = startNext(now);
        } else {
          inFlight.stage = Stage.WAIT_PUBCOMP;
          inFlight.lastFrame = MqttSn2AckCodec.encode(new MqttSn2AckCodec.Ack(
              MqttSn2PacketType.PUBREL, inFlight.packetIdentifier, null));
          inFlight.lastSentAt = now;
          inFlight.retries = 0;
          response = duplicate(inFlight.lastFrame);
        }
      } else if (inFlight.stage == Stage.WAIT_PUBCOMP && ack.type() == MqttSn2PacketType.PUBCOMP) {
        completion = finishCurrent();
        response = startNext(now);
      } else {
        throw new IOException("Unexpected MQTT-SN 2.0 acknowledgement " + ack.type());
      }
    }
    if (completion != null) completion.run();
    return response;
  }

  /** Returns the exact outstanding request for retransmission, or null when not due. */
  public synchronized ByteBuffer retryExpired(long now) {
    if (inFlight == null || now - inFlight.lastSentAt < retryIntervalMillis) {
      return null;
    }
    if (inFlight.retries >= maximumRetries) {
      inFlight.retryExhausted = true;
      return null;
    }
    inFlight.retries++;
    inFlight.lastSentAt = now;
    if (inFlight.stage == Stage.WAIT_PUBACK || inFlight.stage == Stage.WAIT_PUBREC) {
      return MqttSn2PublishCodec.encode(new MqttSn2PublishCodec.Publish(true,
          inFlight.delivery.qos(), false, inFlight.delivery.retain(), 3,
          inFlight.packetIdentifier, inFlight.delivery.topic(), 0, inFlight.delivery.payload()));
    }
    return duplicate(inFlight.lastFrame);
  }

  public synchronized boolean isRetryExhausted() {
    return inFlight != null && inFlight.retryExhausted;
  }

  public synchronized boolean hasInFlightDelivery() {
    return inFlight != null;
  }

  private ByteBuffer startNext(long now) {
    Delivery next = pending.pollFirst();
    return next == null ? null : start(next, now);
  }

  private ByteBuffer start(Delivery delivery, long now) {
    int identifier = nextPacketIdentifier;
    nextPacketIdentifier = nextPacketIdentifier == 65535 ? 1 : nextPacketIdentifier + 1;
    inFlight = new InFlight(delivery, identifier, now);
    return duplicate(inFlight.lastFrame);
  }

  private Runnable finishCurrent() {
    Runnable completion = inFlight.delivery.completion();
    inFlight = null;
    return completion;
  }

  private static ByteBuffer encodePublish(Delivery delivery, int packetIdentifier) {
    return MqttSn2PublishCodec.encode(new MqttSn2PublishCodec.Publish(false,
        delivery.qos(), false, delivery.retain(), 3, packetIdentifier,
        delivery.topic(), 0, delivery.payload()));
  }

  private static ByteBuffer duplicate(ByteBuffer frame) {
    return frame.asReadOnlyBuffer();
  }
}
