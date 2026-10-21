/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * Explicit bidirectional MQTT-SN 2.0 HMAC security policy.
 *
 * <p>Requires a caller-provided monotonic outbound counter source backed by
 * durable state. Do not reuse a fresh counter starting at zero after restart.
 * This policy is deliberately not inferred from legacy transport HMAC config.</p>
 */
public final class MqttSn2HmacProtectionSession implements MqttSn2ProtectionPolicy {

  @FunctionalInterface
  public interface CounterSource {
    byte[] nextCounter() throws IOException;
  }

  private final MqttSn2ProtectionVerifier verifier;
  private final CounterSource counterSource;
  private final byte[] senderIdentifier;
  private final int scheme;
  private final SecureRandom random = new SecureRandom();
  private long lastOutgoingCounter = -1;

  public MqttSn2HmacProtectionSession(MqttSn2ProtectionVerifier verifier,
      CounterSource counterSource, byte[] senderIdentifier, int scheme) {
    this.verifier = Objects.requireNonNull(verifier, "verifier");
    this.counterSource = Objects.requireNonNull(counterSource, "counterSource");
    if (senderIdentifier == null || senderIdentifier.length != 8) {
      throw new IllegalArgumentException("Protection sender identifier must contain 8 octets");
    }
    if (scheme != 0 && scheme != 1) {
      throw new IllegalArgumentException("Only standard HMAC protection schemes are supported");
    }
    this.senderIdentifier = senderIdentifier.clone();
    this.scheme = scheme;
  }

  public boolean hasDurableReplayStore() {
    return verifier.hasDurableReplayStore();
  }

  public ByteBuffer receive(ByteBuffer protectedFrame) throws IOException {
    return verifier.verify(protectedFrame);
  }

  @Override
  public Authenticated receiveAuthenticated(ByteBuffer protectedFrame) throws IOException {
    return verifier.verifyAuthenticated(protectedFrame);
  }

  public synchronized ByteBuffer send(ByteBuffer inner) throws IOException {
    byte[] next = counterSource.nextCounter();
    if (next == null || (next.length != 2 && next.length != 4)) {
      throw new IOException("Protection counter source did not return a valid counter");
    }
    long value = 0;
    for (byte octet : next) value = (value << 8) | (octet & 0xffL);
    if (value <= lastOutgoingCounter) {
      throw new IOException("Protection counter source supplied a repeated or decreasing value");
    }
    byte[] nonce = new byte[4];
    random.nextBytes(nonce);
    ByteBuffer protectedFrame = verifier.protect(inner, scheme, senderIdentifier, nonce, next);
    lastOutgoingCounter = value;
    return protectedFrame;
  }
}
