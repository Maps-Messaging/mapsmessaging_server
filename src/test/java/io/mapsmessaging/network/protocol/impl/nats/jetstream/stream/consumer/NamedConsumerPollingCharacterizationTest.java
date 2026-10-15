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

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.tasks.MessageResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NamedConsumerPollingCharacterizationTest {
  @Test
  void excluded_messages_retry_same_stream_before_rotating() throws Exception {
    var firstManager = mock(SubscribedEventManager.class);
    var secondManager = mock(SubscribedEventManager.class);
    var included = event(firstManager, 2);
    var second = event(secondManager, 3);
    // No subject metadata: filtering must use the subscription subject fallback.
    var firstStream = new StreamSubscriptionInfo(firstManager, new StreamInfo("orders/created", null));
    var secondStream = new StreamSubscriptionInfo(secondManager, new StreamInfo("orders/created", null));
    var excluded = new MessageEvent("STREAM/orders/deleted", firstManager,
        new MessageBuilder().setId(1).setMeta(new java.util.HashMap<>(java.util.Map.of("nats.subject", "orders.deleted")))
            .setOpaqueData(new byte[]{1}).build(), () -> {});
    when(firstManager.getNext()).thenReturn(completed(excluded), completed(included));
    when(secondManager.getNext()).thenReturn(completed(second));
    var config = new ConsumerConfig();
    config.setFilterSubject("orders.created");
    var consumer = new NamedConsumer("reader", "STREAM", config, List.of(firstStream, secondStream));
    try {
      assertSame(included, consumer.getNextMessage(true));
      verifyNoInteractions(secondManager);
      verify(firstManager).ackReceived(1);
      assertSame(second, consumer.getNextMessage(true));
    } finally {
      consumer.close();
    }
  }

  @Test
  void failed_poll_is_removed_and_same_stream_is_retried_on_next_pull() throws Exception {
    var manager = mock(SubscribedEventManager.class);
    var cause = new IOException("storage failed");
    var failed = new CompletableFuture<Response>();
    failed.completeExceptionally(cause);
    var expected = event(manager, 7);
    when(manager.getNext()).thenReturn(failed, completed(expected));
    var stream = new StreamSubscriptionInfo(manager, new StreamInfo("orders/created", null));
    var consumer = new NamedConsumer("reader", "STREAM", new ConsumerConfig(), List.of(stream));
    try {
      var failure = assertThrows(IOException.class, () -> consumer.getNextMessage(true));
      assertSame(cause, failure.getCause().getCause());
      assertSame(expected, consumer.getNextMessage(true));
    } finally {
      consumer.close();
    }
  }

  private static MessageEvent event(SubscribedEventManager manager, long id) {
    return new MessageEvent("STREAM/orders/created", manager,
        new MessageBuilder().setId(id).setOpaqueData(new byte[]{1}).build(), () -> {});
  }

  private static CompletableFuture<Response> completed(MessageEvent event) {
    return CompletableFuture.completedFuture(new MessageResponse(event));
  }
}
