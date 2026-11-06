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
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DurableCreateHandlerTest {

  @Test
  void durableApiCreatesConsumerThroughSameAuthenticatedCreatePath() throws Exception {
    CreateHandler delegate = mock(CreateHandler.class);
    SessionState state = mock(SessionState.class);
    MsgFrame request = new MsgFrame(1024);
    request.setSubject("$JS.API.CONSUMER.DURABLE.CREATE.EVENTS.reader");
    request.setReplyTo("_INBOX.request");
    JsonObject payload = JsonParser.parseString("{\"config\":{\"ack_wait\":30000000000}}").getAsJsonObject();
    PayloadFrame expected = new MsgFrame(0);

    when(delegate.handle(same(request), any(JsonObject.class), same(state))).thenAnswer(invocation -> {
      JsonObject mapped = invocation.getArgument(1);
      assertEquals("$JS.API.CONSUMER.CREATE.EVENTS.reader", request.getSubject());
      assertEquals("reader", mapped.getAsJsonObject("config").get("durable_name").getAsString());
      assertEquals("reader", mapped.getAsJsonObject("config").get("name").getAsString());
      return expected;
    });

    NatsFrame response = new DurableCreateHandler(delegate).handle(request, payload, state);

    assertSame(expected, response);
    assertEquals("$JS.API.CONSUMER.DURABLE.CREATE.EVENTS.reader", request.getSubject());
  }

  @Test
  void rejectsMismatchedDurableNamesWithoutInvokingCreation() throws Exception {
    CreateHandler delegate = mock(CreateHandler.class);
    SessionState state = mock(SessionState.class);
    JetStreamRequestManager requests = mock(JetStreamRequestManager.class);
    when(state.getJetStreamRequestManager()).thenReturn(requests);
    MsgFrame request = new MsgFrame(1024);
    request.setSubject("$JS.API.CONSUMER.DURABLE.CREATE.EVENTS.reader");
    request.setReplyTo("_INBOX.request");
    JsonObject payload = JsonParser.parseString("{\"config\":{\"durable_name\":\"other\"}}").getAsJsonObject();

    PayloadFrame response = assertInstanceOf(PayloadFrame.class,
        new DurableCreateHandler(delegate).handle(request, payload, state));

    assertTrue(new String(response.getPayload(), StandardCharsets.UTF_8).contains("Durable name does not match"));
    verifyNoInteractions(delegate);
  }
}
