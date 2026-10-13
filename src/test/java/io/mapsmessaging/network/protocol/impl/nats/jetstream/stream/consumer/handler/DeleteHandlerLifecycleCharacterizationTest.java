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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.NatsJetStreamSubscriptions;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.DurableConsumerSession;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteHandlerLifecycleCharacterizationTest {
  @Test
  void deletes_owned_subscriptions_and_waits_for_session_close_under_state_lock() throws Exception {
    var state = mock(SessionState.class);
    var namespaces = mock(NamespaceManager.class);
    when(namespaces.getStream("STREAM")).thenReturn(new StreamInfoList("STREAM"));
    when(state.getJetStreamRequestManager()).thenReturn(mock(JetStreamRequestManager.class));
    var owned = mock(Session.class);
    var manager = mock(SubscribedEventManager.class);
    var ignoredManager = mock(SubscribedEventManager.class);
    var subscription = new StreamSubscriptionInfo(manager, new StreamInfo("orders/created", null), "owned");
    var ignored = new StreamSubscriptionInfo(ignoredManager, new StreamInfo("orders/deleted", null));
    var consumer = new NamedConsumer("reader", "STREAM", new ConsumerConfig(), List.of(subscription, ignored));
    consumer.attachMapsSession(owned);
    var consumers = new HashMap<String, NamedConsumer>();
    consumers.put("reader", consumer);
    when(state.getNamedConsumers()).thenReturn(consumers);
    var request = new MsgFrame(0);
    request.setSubject("$JS.API.CONSUMER.DELETE.STREAM.reader");
    request.setReplyTo("_INBOX.delete");
    try (var namespaceAccess = mockStatic(NamespaceManager.class);
         var subscriptions = mockStatic(NatsJetStreamSubscriptions.class);
         var sessions = mockStatic(DurableConsumerSession.class)) {
      namespaceAccess.when(NamespaceManager::getInstance).thenReturn(namespaces);
      subscriptions.when(() -> NatsJetStreamSubscriptions.remove(owned, manager)).thenAnswer(invocation -> {
        assertTrue(Thread.holdsLock(state));
        assertFalse(consumers.containsKey("reader"));
        return null;
      });
      sessions.when(() -> DurableConsumerSession.close(state, owned)).thenAnswer(invocation -> {
        assertTrue(Thread.holdsLock(state));
        assertNull(consumer.getNextMessage(true));
        return CompletableFuture.completedFuture(owned);
      });
      var response = assertInstanceOf(PayloadFrame.class, new DeleteHandler().handle(request, new JsonObject(), state));
      var body = JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
      assertTrue(body.get("success").getAsBoolean());
      subscriptions.verify(() -> NatsJetStreamSubscriptions.remove(owned, manager));
      subscriptions.verify(() -> NatsJetStreamSubscriptions.remove(owned, ignoredManager), never());
      sessions.verify(() -> DurableConsumerSession.close(state, owned));
      verify(state, never()).removeSubscription(anyString());
    }
  }
}
