/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class TransactionSubject {
  private final String stream;
  private final String consumer;
  private final int deliverySequence;
  private final int destinationIndex;
  private final long messageId;
  private final long timestamp;
  private final String token;

  public TransactionSubject(NamedConsumer consumer, String destinationName, long messageId) {
    long hashCode = 0xffffffffffffffffL & destinationName.hashCode();
    this.stream = consumer.getStreamName();
    this.consumer = consumer.getName();
    this.deliverySequence = 1;
    this.destinationIndex = findDestinationIndex(consumer, destinationName);
    this.messageId = messageId;
    this.timestamp = System.nanoTime();
    this.token = ""+hashCode;
  }

  public static TransactionSubject parse(String subject) {
    String[] parts = subject.split("\\.");
    if (parts.length != 9 || !"$JS".equals(parts[0]) || !"ACK".equals(parts[1])) {
      throw new IllegalArgumentException("Invalid ACK subject: " + subject);
    }
    return new TransactionSubject(
        parts[2],
        parts[3],
        Integer.parseInt(parts[4]),
        Integer.parseInt(parts[5]),
        Long.parseLong(parts[6]),
        Long.parseLong(parts[7]),
        parts[8]
    );
  }

  public String toSubject() {
    return String.join(".",
        "$JS",
        "ACK",
        stream,
        consumer,
        String.valueOf(deliverySequence),
        String.valueOf(destinationIndex),
        String.valueOf(messageId),
        String.valueOf(timestamp),
        token
    );
  }

  private int findDestinationIndex(NamedConsumer consumer, String destinationName) {
    String lookup = consumer.getStreamName()+"/"+destinationName;
    for(int x=0;x<consumer.getStreams().size();x++){
      if(consumer.getStreams().get(x).getDestination().getFullyQualifiedNamespace().equals(lookup)){
        return x;
      }
    }
    return Integer.MAX_VALUE;
  }

}
