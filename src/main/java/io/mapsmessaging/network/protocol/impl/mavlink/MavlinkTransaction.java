/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import com.google.gson.JsonObject;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

final class MavlinkTransaction {
  final String id;
  final String topic;
  final String operation;
  final String destination;
  final List<JsonObject> messages;
  final int system;
  final int component;
  final int missionType;
  final int localSystem;
  final int localComponent;
  final BitSet requested = new BitSet();
  ScheduledFuture<?> future;
  long generation;
  int lastIndex;
  int attempts;
  int retries;
  String fingerprint;
  boolean commandInProgress;
  boolean commandAccepted;
  boolean missionCurrentSeen;
  boolean requestedMessageSeen;
  int expectedMissionSequence = -1;
  int requestedMessageId = -1;
  String status = "QUEUED";

  MavlinkTransaction(String id, String topic, String operation, String destination, List<JsonObject> messages, int system, int component,
      int missionType, int localSystem, int localComponent) {
    this.id = id;
    this.topic = topic;
    this.operation = operation;
    this.destination = destination;
    this.messages = messages;
    this.system = system;
    this.component = component;
    this.missionType = missionType;
    this.localSystem = localSystem;
    this.localComponent = localComponent;
  }
}
