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

import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.StreamManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.ConsumerManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.info.InfoManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions.TransactionManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class JetStreamApiManager {

  private final RequestHandler[] managers;

  public JetStreamApiManager() {
    this.managers = new RequestHandler[]{
        new InfoManager(),
        new StreamManager(),
        new ConsumerManager(),
        new TransactionManager()
    };

  }

  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    for (RequestHandler manager : managers) {
      if (subject.startsWith(manager.getType())) {
        return manager.process(subject, frame, sessionState);
      }
    }
    return new ErrFrame("Unknown JetStream API call: " + subject);
  }

}
