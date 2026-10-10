/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2ProtectionCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection.MqttSn2CryptoProvider;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection.MqttSn2CryptoProviders;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection.MqttSn2NonceDeriver;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import javax.crypto.SecretKey;

/**
 * Common HMAC/AEAD protection path. All accepted frames must authenticate and
 * have valid inner framing before their durable replay counters advance.
 */
public final class MqttSn2ProtectionSession implements MqttSn2ProtectionPolicy {
  @FunctionalInterface
  public interface SecretKeyResolver {
    SecretKey resolveSecretKey(byte[] senderId, int scheme) throws IOException;
  }

  private final MqttSn2CryptoProviders providers;
  private final SecretKeyResolver keys;
  private final MqttSn2IndexedCounterStore counters;
  private final Set<Integer> accepted;
  private final int outboundScheme;
  private final byte[] sender;
  private final SecureRandom random = new SecureRandom();

  public MqttSn2ProtectionSession(MqttSn2CryptoProviders providers,
      SecretKeyResolver keys, MqttSn2IndexedCounterStore counters,
      Set<Integer> accepted, int outboundScheme, byte[] sender) {
    this.providers = Objects.requireNonNull(providers);
    this.keys = Objects.requireNonNull(keys);
    this.counters = Objects.requireNonNull(counters);
    this.accepted = Set.copyOf(accepted);
    if (sender == null || sender.length != 8 || !this.accepted.contains(outboundScheme)) {
      throw new IllegalArgumentException("Invalid outbound protection policy");
    }
    providers.require(outboundScheme);
    for (int scheme : accepted) providers.require(scheme);
    this.outboundScheme = outboundScheme;
    this.sender = sender.clone();
  }

  @Override public boolean hasDurableReplayStore() { return true; }

  @Override
  public synchronized ByteBuffer receive(ByteBuffer wire) throws IOException {
    MqttSn2ProtectionCodec.Envelope envelope = MqttSn2ProtectionCodec.decode(
        wire, this::tagLength);
    int scheme = envelope.scheme();
    if (!accepted.contains(scheme)) throw new IOException("Disallowed protection scheme");
    if (envelope.monotonicCounter().length != 4) {
      throw new IOException("Protection requires four-byte monotonic counter");
    }
    MqttSn2CryptoProvider provider = providers.require(scheme);
    byte[] prefix = envelope.authenticatedPrefix();
    byte[] nonce = nonce(scheme, prefix);
    byte[] clear;
    try {
      clear = provider.unprotect(scheme,
          keys.resolveSecretKey(envelope.senderIdentifier(), scheme),
          nonce, prefix,
          new MqttSn2CryptoProvider.ProtectedPayload(
              envelope.protectedPacket(), envelope.authenticationTag()));
    } catch (GeneralSecurityException error) {
      throw new IOException("MQTT-SN protection authentication failed", error);
    }
    MqttSn2FrameCodec.Frame inner = MqttSn2FrameCodec.decode(ByteBuffer.wrap(clear));
    if (inner.packetLength() != clear.length
        || inner.type() == MqttSn2PacketType.FORWARDER_ENCAPSULATION
        || inner.type() == MqttSn2PacketType.PROTECTION_ENCAPSULATION) {
      throw new IOException("Invalid authenticated MQTT-SN inner packet");
    }
    byte[] counter = envelope.monotonicCounter();
    long value = 0;
    for (byte octet : counter) value = (value << 8) | (octet & 0xffL);
    if (!counters.accept(envelope.senderIdentifier(), scheme, value)) {
      throw new IOException("MQTT-SN protection replay detected");
    }
    return ByteBuffer.wrap(clear).asReadOnlyBuffer();
  }

  @Override
  public synchronized ByteBuffer send(ByteBuffer innerPacket) throws IOException {
    ByteBuffer source = innerPacket.asReadOnlyBuffer();
    MqttSn2FrameCodec.Frame frame = MqttSn2FrameCodec.decode(source.asReadOnlyBuffer());
    if (frame.packetLength() != source.remaining()
        || frame.type() == MqttSn2PacketType.FORWARDER_ENCAPSULATION
        || frame.type() == MqttSn2PacketType.PROTECTION_ENCAPSULATION) {
      throw new IOException("Invalid outbound MQTT-SN inner packet");
    }
    byte[] plaintext = new byte[source.remaining()];
    source.get(plaintext);
    MqttSn2CryptoProvider provider = providers.require(outboundScheme);
    byte[] counter = counters.nextCounter();
    byte[] salt = new byte[4];
    random.nextBytes(salt);
    int tagSize = provider.tagLength(outboundScheme);
    // CSD01 §3.17.2.3: AEAD MUST use nominal tag length code 0x1.
    // HMAC uses 0x1 for the nominal tag as well.
    int tagCode = 1;
    if (tagSize != 32 && tagSize != 16 && tagSize != 8) {
      throw new IOException("Unsupported MQTT-SN protection tag size");
    }
    // Encode once to derive the exact on-wire associated-data prefix, including
    // the short/extended packet header. Ciphertext has the same length as plaintext.
    byte[] placeholderTag = new byte[tagSize];
    byte[] placeholderPacket = new byte[plaintext.length];
    var template = new MqttSn2ProtectionCodec.Envelope(outboundScheme, tagCode,
        sender, salt, new byte[0], counter, new byte[0], placeholderPacket, placeholderTag);
    ByteBuffer templateWire = MqttSn2ProtectionCodec.encode(template, this::tagLength);
    byte[] prefix = MqttSn2ProtectionCodec.decode(templateWire, this::tagLength)
        .authenticatedPrefix();
    MqttSn2CryptoProvider.ProtectedPayload protectedPayload;
    try {
      protectedPayload = provider.protect(outboundScheme,
          keys.resolveSecretKey(sender, outboundScheme), nonce(outboundScheme, prefix),
          prefix, plaintext);
    } catch (GeneralSecurityException error) {
      throw new IOException("Unable to protect outgoing MQTT-SN packet", error);
    }
    if (protectedPayload.protectedBytes().length != plaintext.length) {
      throw new IOException("Protection provider changed ciphertext size");
    }
    var envelope = new MqttSn2ProtectionCodec.Envelope(outboundScheme, tagCode,
        sender, salt, new byte[0], counter, prefix,
        protectedPayload.protectedBytes(), protectedPayload.tag());
    return MqttSn2ProtectionCodec.encode(envelope, this::tagLength).asReadOnlyBuffer();
  }

  private int tagLength(int scheme, int code) throws IOException {
    if (!accepted.contains(scheme)) throw new IOException("Disallowed protection scheme");
    int expected = providers.require(scheme).tagLength(scheme);
    if (scheme >= 0x40 && scheme <= 0x49) {
      if (code == 1) return expected;
      throw new IOException("AEAD requires nominal authentication-tag code 0x1");
    }
    if ((expected == 32 && (code == 0 || code == 1))
        || (code >= 4 && code <= 15 && code * 2 == expected)) {
      return expected;
    }
    throw new IOException("Invalid protection authentication-tag length");
  }

  private static byte[] nonce(int scheme, byte[] prefix) {
    if (scheme >= 0x40 && scheme <= 0x45) return MqttSn2NonceDeriver.derive(prefix, 13);
    if (scheme >= 0x46 && scheme <= 0x49) return MqttSn2NonceDeriver.derive(prefix, 12);
    return new byte[0];
  }
}
