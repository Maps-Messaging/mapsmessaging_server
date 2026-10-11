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
