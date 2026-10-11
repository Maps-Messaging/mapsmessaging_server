/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import java.io.IOException;
import java.nio.ByteBuffer;

/** Bidirectional endpoint policy shared by HMAC and AEAD protection. */
public interface MqttSn2ProtectionPolicy {
  boolean hasDurableReplayStore();
  ByteBuffer receive(ByteBuffer wire) throws IOException;

  /** Authenticated sender identity, available only after verification succeeds. */
  record Authenticated(ByteBuffer plaintext, byte[] senderIdentifier) {
    public Authenticated {
      senderIdentifier = senderIdentifier == null ? null : senderIdentifier.clone();
    }

    @Override
    public byte[] senderIdentifier() {
      return senderIdentifier == null ? null : senderIdentifier.clone();
    }
  }

  /** Compatibility hook for policies that do not expose authenticated sender IDs. */
  default Authenticated receiveAuthenticated(ByteBuffer wire) throws IOException {
    return new Authenticated(receive(wire), null);
  }
  ByteBuffer send(ByteBuffer inner) throws IOException;
}
