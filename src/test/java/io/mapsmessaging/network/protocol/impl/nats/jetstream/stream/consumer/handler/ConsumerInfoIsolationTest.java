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

import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumerInfoIsolationTest {
  @Test
  void consumerInfoRejectsNameFromAnotherStream() throws Exception {
    NamedConsumer consumer = mock(NamedConsumer.class);
    SessionState session = mock(SessionState.class);
    JetStreamRequestManager manager = mock(JetStreamRequestManager.class);
    PayloadFrame request = mock(PayloadFrame.class);

    when(request.getSubject()).thenReturn("$JS.API.CONSUMER.INFO.other.consumer");
    when(request.getReplyTo()).thenReturn("_INBOX.info");
    when(session.getNamedConsumers()).thenReturn(Map.of("consumer", consumer));
    when(session.getJetStreamRequestManager()).thenReturn(manager);
    when(consumer.getStreamName()).thenReturn("stream");

    PayloadFrame response = (PayloadFrame) new InfoHandler().handle(request, null, session);
    assertNotNull(response);
    assertTrue(new String(response.getPayload(), StandardCharsets.UTF_8).contains("not found"));
    verify(consumer, never()).getConfig();
  }
}
