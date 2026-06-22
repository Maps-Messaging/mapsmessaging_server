/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
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

package io.mapsmessaging.state.stanag.messages.core;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public class MessageHeaderBuilder {

  private static final String VERSION = "0.3.0";

  private final Clock clock;

  public MessageHeader build(MessageType messageType, UUID sourceIdentifier, Instant timestamp) {
    Objects.requireNonNull(messageType, "messageType cannot be null");
    Objects.requireNonNull(sourceIdentifier, "sourceIdentifier cannot be null");

    Instant resolvedTimestamp = timestamp != null ? timestamp : now();
    return new MessageHeader(messageType, sourceIdentifier.toString(), truncate(resolvedTimestamp), VERSION);
  }

  private Instant now() {
    return Instant.now(clock);
  }

  private Instant truncate(Instant timestamp) {
    return timestamp.truncatedTo(ChronoUnit.MILLIS);
  }
}