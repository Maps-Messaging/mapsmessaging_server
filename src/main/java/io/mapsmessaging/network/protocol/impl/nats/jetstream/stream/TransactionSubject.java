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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import lombok.Getter;
import lombok.ToString;

/**
 * The standard v1 JetStream acknowledgement subject contains only wire metadata.
 * MAPS resource identifiers and subscription ownership are deliberately kept in NamedConsumer.
 */
@Getter
@ToString
public class TransactionSubject {

  private final String stream;
  private final String consumer;
  private final long deliverySequence;
  private final long streamSequence;
  private final long consumerSequence;
  private final long timestamp;
  private final long pending;

  public TransactionSubject(String stream, String consumer, long deliverySequence, long streamSequence,
                            long consumerSequence, long timestamp, long pending) {
    if (stream == null || stream.isBlank() || consumer == null || consumer.isBlank()
        || deliverySequence <= 0 || streamSequence <= 0 || consumerSequence <= 0
        || timestamp < 0 || pending < 0) {
      throw new IllegalArgumentException("Invalid JetStream ACK metadata");
    }
    this.stream = stream;
    this.consumer = consumer;
    this.deliverySequence = deliverySequence;
    this.streamSequence = streamSequence;
    this.consumerSequence = consumerSequence;
    this.timestamp = timestamp;
    this.pending = pending;
  }

  public static TransactionSubject parse(String subject) {
    if (subject == null) {
      throw new IllegalArgumentException("Null ACK subject");
    }
    String[] tokens = subject.split("\\.", -1);
    if (tokens.length != 9 || !"$JS".equals(tokens[0]) || !"ACK".equals(tokens[1])) {
      throw new IllegalArgumentException("Invalid JetStream ACK subject: " + subject);
    }
    try {
      return new TransactionSubject(tokens[2], tokens[3], Long.parseLong(tokens[4]),
          Long.parseLong(tokens[5]), Long.parseLong(tokens[6]), Long.parseLong(tokens[7]),
          Long.parseLong(tokens[8]));
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Invalid numeric JetStream ACK metadata", exception);
    }
  }

  public String toSubject() {
    return "$JS.ACK." + stream + "." + consumer + "." + deliverySequence
        + "." + streamSequence + "." + consumerSequence + "." + timestamp + "." + pending;
  }
}
