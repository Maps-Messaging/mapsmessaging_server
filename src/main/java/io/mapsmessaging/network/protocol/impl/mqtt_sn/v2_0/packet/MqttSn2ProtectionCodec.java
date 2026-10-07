/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * MQTT-SN 2.0 CSD01 Protection Encapsulation envelope parsing.
 *
 * <p>Authentication tag size is scheme/provider dependent. An envelope can
 * only be separated into protected bytes and tag when a provider supplies
 * that size. This class does not decrypt, authenticate or silently unwrap.</p>
 */
public final class MqttSn2ProtectionCodec {

  @FunctionalInterface
  public interface TagLengthResolver {
    int length(int scheme, int tagLengthCode) throws IOException;
  }

  public record Envelope(int scheme, int tagLengthCode, byte[] senderIdentifier,
                         byte[] random, byte[] cryptographicMaterial,
                         byte[] monotonicCounter, byte[] authenticatedPrefix,
                         byte[] protectedPacket, byte[] authenticationTag) {
    public Envelope {
      senderIdentifier = senderIdentifier.clone();
      random = random.clone();
      cryptographicMaterial = cryptographicMaterial.clone();
      monotonicCounter = monotonicCounter.clone();
      authenticatedPrefix = authenticatedPrefix.clone();
      protectedPacket = protectedPacket.clone();
      authenticationTag = authenticationTag.clone();
    }
    @Override public byte[] senderIdentifier() { return senderIdentifier.clone(); }
    @Override public byte[] random() { return random.clone(); }
    @Override public byte[] cryptographicMaterial() { return cryptographicMaterial.clone(); }
    @Override public byte[] monotonicCounter() { return monotonicCounter.clone(); }
    @Override public byte[] authenticatedPrefix() { return authenticatedPrefix.clone(); }
    @Override public byte[] protectedPacket() { return protectedPacket.clone(); }
    @Override public byte[] authenticationTag() { return authenticationTag.clone(); }
  }

  private MqttSn2ProtectionCodec() {}

  /**
   * Encode an already protected payload and authentication tag. The crypto
   * provider creates these bytes and is responsible for authentication.
   */
  public static ByteBuffer encode(Envelope envelope) {
    Objects.requireNonNull(envelope, "envelope");
    int counterLength = envelope.monotonicCounter().length;
    int counterCode = switch (counterLength) {
      case 0 -> 0; case 2 -> 1; case 4 -> 2;
      default -> throw new IllegalArgumentException("Invalid monotonic counter length");
    };
    int materialLength = envelope.cryptographicMaterial().length;
    int materialCode = switch (materialLength) {
      case 0 -> 0; case 2 -> 1; case 4 -> 2; case 12 -> 3;
      default -> throw new IllegalArgumentException("Invalid cryptographic material length");
    };
    if (envelope.senderIdentifier().length != 8 || envelope.random().length != 4
        || envelope.tagLengthCode() < 0 || envelope.tagLengthCode() > 15
        || envelope.tagLengthCode() == 2 || envelope.tagLengthCode() == 3
        || envelope.authenticationTag().length == 0
        || envelope.protectedPacket().length == 0
        || envelope.scheme() < 0 || envelope.scheme() > 255
        || (envelope.scheme() >= 5 && envelope.scheme() <= 0x3B)
        || (envelope.scheme() >= 0x4A && envelope.scheme() <= 0xEF)) {
      throw new IllegalArgumentException("Invalid Protection Encapsulation fields");
    }
    int flags = (envelope.tagLengthCode() << 4) | (materialCode << 2) | counterCode;
    ByteBuffer body = ByteBuffer.allocate(14 + materialLength + counterLength
        + envelope.protectedPacket().length + envelope.authenticationTag().length);
    body.put((byte) flags).put((byte) envelope.scheme())
        .put(envelope.senderIdentifier()).put(envelope.random())
        .put(envelope.cryptographicMaterial()).put(envelope.monotonicCounter())
        .put(envelope.protectedPacket()).put(envelope.authenticationTag()).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.PROTECTION_ENCAPSULATION, body);
  }

  public static Envelope decode(ByteBuffer wire, TagLengthResolver tagResolver)
      throws IOException {
    Objects.requireNonNull(wire, "wire");
    Objects.requireNonNull(tagResolver, "tagResolver");
    MqttSn2FrameCodec.Frame frame = MqttSn2FrameCodec.decode(wire);
    if (frame.type() != MqttSn2PacketType.PROTECTION_ENCAPSULATION) {
      throw new IOException("Expected MQTT-SN 2.0 Protection Encapsulation");
    }
    ByteBuffer body = frame.payload().asReadOnlyBuffer();
    if (body.remaining() < 14) throw new IOException("Truncated protection header");
    int flags = Byte.toUnsignedInt(body.get());
    int counterCode = flags & 3;
    int materialCode = (flags >>> 2) & 3;
    int tagCode = (flags >>> 4) & 15;
    if (counterCode == 3 || tagCode == 2 || tagCode == 3) {
      throw new IOException("Reserved protection length flags");
    }
    int scheme = Byte.toUnsignedInt(body.get());
    if ((scheme >= 5 && scheme <= 0x3B) || (scheme >= 0x4A && scheme <= 0xEF)) {
      throw new IOException("Reserved Protection Scheme");
    }
    byte[] sender = new byte[8];
    byte[] random = new byte[4];
    body.get(sender).get(random);
    int cryptoLength = switch (materialCode) {
      case 0 -> 0; case 1 -> 2; case 2 -> 4; default -> 12;
    };
    int counterLength = counterCode * 2;
    if (body.remaining() < cryptoLength + counterLength) {
      throw new IOException("Truncated protection optional fields");
    }
    byte[] material = new byte[cryptoLength];
    byte[] counter = new byte[counterLength];
    body.get(material).get(counter);
    int tagLength = tagResolver.length(scheme, tagCode);
    if (tagLength <= 0 || body.remaining() <= tagLength) {
      throw new IOException("Truncated protection payload or invalid authentication tag");
    }
    int protectedLength = body.remaining() - tagLength;
    byte[] protectedPacket = new byte[protectedLength];
    byte[] authenticationTag = new byte[tagLength];
    body.get(protectedPacket).get(authenticationTag);
    ByteBuffer prefix = wire.asReadOnlyBuffer();
    prefix.limit(prefix.position() + frame.packetLength() - protectedLength - tagLength);
    byte[] authenticatedPrefix = new byte[prefix.remaining()];
    prefix.get(authenticatedPrefix);
    return new Envelope(scheme, tagCode, sender, random, material, counter,
        authenticatedPrefix, protectedPacket, authenticationTag);
  }
}
