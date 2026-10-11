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

import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageHandlerTest {
  @Test
  void sixPartPullSubjectProducesErrorFrameRatherThanIndexFailure() throws Exception {
    PayloadFrame request = mock(PayloadFrame.class);
    SessionState session = mock(SessionState.class);
    JetStreamRequestManager manager = mock(JetStreamRequestManager.class);
    when(request.getSubject()).thenReturn("$JS.API.CONSUMER.MSG.NEXT.stream");
    when(request.getReplyTo()).thenReturn("_INBOX.test");
    when(session.getJetStreamRequestManager()).thenReturn(manager);
    when(manager.getSid("_INBOX.test")).thenReturn("1");

    NatsFrame response = new MessageHandler().handle(request, null, session);

    assertNotNull(response);
    verify(session, never()).getNamedConsumers();
  }

  @Test
  void emptyPullRequestUsesDefaultBatchWithoutThrowing() throws Exception {
    PayloadFrame request = mock(PayloadFrame.class);
    SessionState session = mock(SessionState.class);
    io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer consumer =
        mock(io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer.class);
    when(request.getSubject()).thenReturn("$JS.API.CONSUMER.MSG.NEXT.stream.consumer");
    when(request.getReplyTo()).thenReturn("_INBOX.test");
    when(session.getNamedConsumers()).thenReturn(java.util.Map.of("consumer", consumer));
    when(consumer.getStreamName()).thenReturn("stream");

    assertNull(new MessageHandler().handle(request, null, session));
    verify(consumer).getNextMessage();
  }

  @Test
  void zeroBatchIsRejectedBeforeFetching() throws Exception {
    PayloadFrame request = mock(PayloadFrame.class);
    SessionState session = mock(SessionState.class);
    JetStreamRequestManager manager = mock(JetStreamRequestManager.class);
    io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer consumer =
        mock(io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer.class);
    when(request.getSubject()).thenReturn("$JS.API.CONSUMER.MSG.NEXT.stream.consumer");
    when(request.getReplyTo()).thenReturn("_INBOX.test");
    when(session.getNamedConsumers()).thenReturn(java.util.Map.of("consumer", consumer));
    when(consumer.getStreamName()).thenReturn("stream");
    when(session.getJetStreamRequestManager()).thenReturn(manager);
    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
    json.addProperty("batch", 0);

    assertNotNull(new MessageHandler().handle(request, json, session));
    verify(consumer, never()).getNextMessage();
  }
}
