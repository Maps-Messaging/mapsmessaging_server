/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.storage.counter.CounterDurability;
import io.mapsmessaging.storage.counter.GenerationCounterStore;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Objects;

/**
 * MQTT-SN protection adapter for the protocol-neutral indexed store.
 * Inbound replay checks are always durable; outbound reservations are durably
 * committed before returning the counter, including across JVM restarts.
 * Unused outbound reservations are deliberately skipped following restart.
 */
public final class MqttSn2IndexedCounterStore implements
    MqttSn2ProtectionVerifier.ReplayStore,
    MqttSn2HmacProtectionSession.CounterSource, AutoCloseable {

  private static final long MAX_WIRE_COUNTER = 0xffff_ffffL;
  private static final String TX = "tx.counter";
  private static final int RESERVATION_SIZE = 1024;
  private long nextOutbound;
  private long reservedThrough;
  private final GenerationCounterStore store;

  public MqttSn2IndexedCounterStore(Path directory) throws IOException {
    this.store = new GenerationCounterStore(Objects.requireNonNull(directory),
        CounterDurability.STRICT, 1, 0, 0.25);
  }

  @Override
  public synchronized boolean accept(byte[] senderIdentifier, int scheme, long counter)
      throws IOException {
    if (senderIdentifier == null || senderIdentifier.length != 8
        || scheme < 0 || scheme > 255 || counter <= 0 || counter > MAX_WIRE_COUNTER) {
      throw new IOException("Invalid MQTT-SN protection replay counter");
    }
    return store.accept("rx." + HexFormat.of().formatHex(senderIdentifier) + "." + scheme, counter);
  }

  @Override
  public synchronized byte[] nextCounter() throws IOException {
    if (nextOutbound == 0 || nextOutbound > reservedThrough) {
      long persisted = store.highWaterMark(TX);
      if (persisted >= MAX_WIRE_COUNTER) {
        throw new IOException("Outbound MQTT-SN protection counter exhausted");
      }
      int count = (int) Math.min(RESERVATION_SIZE, MAX_WIRE_COUNTER - persisted);
      var range = store.reserve(TX, count);
      nextOutbound = range.first();
      reservedThrough = range.last();
    }
    long next = nextOutbound++;
    return ByteBuffer.allocate(4).putInt((int) next).array();
  }

  @Override
  public void close() throws IOException {
    store.close();
  }
}
