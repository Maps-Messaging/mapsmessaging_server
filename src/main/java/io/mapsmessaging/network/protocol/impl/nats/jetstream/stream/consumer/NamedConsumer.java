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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.tasks.MessageResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import lombok.Getter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Getter
public class NamedConsumer {

  private final ConsumerConfig config;
  private final String name;
  private final String streamName;
  private final Instant created;
  private final List<StreamSubscriptionInfo> streams;
  private List<Event> events;
  private int index;
  private boolean closed;
  private final Map<StreamSubscriptionInfo, Future<Response>> inFlightPolls = new HashMap<>();

  public NamedConsumer(String name, String streamName, ConsumerConfig config, List<StreamSubscriptionInfo> streams) {
    this.name = name;
    this.streamName = streamName;
    this.config = config;
    this.streams = streams;
    created = Instant.now();
    events = new ArrayList<>();
    index = 0;
  }

  public synchronized void receive(Message event, String destinationName,  Runnable callback) {
    events.add(new Event(event, destinationName, callback));
  }

  public synchronized Event poll() {
    return events.remove(0);
  }

  public synchronized int size() {
    return events.size();
  }

  public synchronized boolean isEmpty() {
    return events.isEmpty();
  }

  public synchronized MessageEvent getNextMessage() throws IOException {
    return getNextMessage(false);
  }

  public synchronized MessageEvent getNextMessage(boolean noWait) throws IOException {
    if (closed || streams.isEmpty()) {
      return null;
    }
    for (int x = 0; x < streams.size(); x++) {
      StreamSubscriptionInfo stream = streams.get(index);
      Future<Response> response = inFlightPolls.get(stream);
      if (response == null) {
        response = stream.getSubscribedEventManager().getNext();
        inFlightPolls.put(stream, response);
      }
      try {
        if (noWait && !response.isDone()) {
          index = (index + 1) % streams.size();
          continue;
        }
        Response val = noWait ? response.get() : response.get(100, TimeUnit.MILLISECONDS);
        inFlightPolls.remove(stream);
        if(val instanceof MessageResponse messageResponse){
          MessageEvent msg = messageResponse.getResponse();
          if (msg != null) {
            index = (index + 1) % streams.size();
            return msg;
          }
        }
      } catch (TimeoutException e) {
        // No message was available within this poll window; try the next subscribed stream.
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException(e);
      } catch (Exception e) {
        inFlightPolls.remove(stream);
        throw new IOException(e);
      }
      index = (index + 1) % streams.size();
    }
    return null;
  }

  public synchronized void close() {
    closed = true;
    for (Future<Response> future : inFlightPolls.values()) {
      future.cancel(false);
    }
    inFlightPolls.clear();
    events.clear();
  }

  public int getAckPendingCount() {
    int pending = 0;
    for (StreamSubscriptionInfo stream : streams) {
      pending += Math.max(0, stream.getSubscribedEventManager().getPending());
    }
    return pending;
  }
}
