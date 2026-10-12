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

package io.mapsmessaging.engine.destination.subscription.state;

/**
 * NATS JetStream-only entry point for seeding a new, ordinary MAPS subscription.
 *
 * Both managers keep independent priority queues. The source archive must be a
 * synchronous subscription that never pulls messages: its at-rest state is the
 * retained JetStream history. Ordinary MAPS subscription creation, registration,
 * delivery and acknowledgement behaviour are not changed.
 */
public final class NatsArchiveStateCopy {

  private NatsArchiveStateCopy() {
  }

  public static int copyAtRest(MessageStateManager archiveState, MessageStateManager consumerState) {
    if (!(archiveState instanceof MessageStateManagerImpl archive)
        || !(consumerState instanceof MessageStateManagerImpl consumer)
        || archive == consumer) {
      throw new IllegalArgumentException("JetStream history requires distinct MAPS priority state managers");
    }

    // A destination subscription task serialises this copy with live message
    // registrations. A publication already registered in the new consumer is
    // naturally deduplicated by the priority-backed bitset queues.
    synchronized (archive) {
      synchronized (consumer) {
        int before = consumer.pending();
        consumer.messagesAtRest.addAll(archive.messagesAtRest);
        return consumer.pending() - before;
      }
    }
  }
}
