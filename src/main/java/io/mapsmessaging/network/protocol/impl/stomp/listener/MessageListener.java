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

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Event;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class MessageListener extends EventListener {

  protected void processEvent(SessionState engine, Event event, Message message) throws IOException {
    String lookup = engine.getMapping(event.getDestination());
    CompletableFuture<Destination> future = engine.getSession().findDestination(lookup, DestinationType.TOPIC);
    if (future != null) {
      future.thenApply(destination -> {
        try {
          if (destination != null) {
            handleMessageStoreToDestination(destination, engine, event, message);
          }
        } catch (IOException e) {
          future.completeExceptionally(e);
          throw new RuntimeException(e);
        }
        return destination;
      });
    }
  }
}
