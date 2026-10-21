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

import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestHandlerValidationTest {

  @Test
  void malformedJsonReturnsStructuredErrorToRequestReplyInbox() throws Exception {
    RequestHandler handler = new RequestHandler(new JetStreamFrameHandler[0]) {
      @Override
      public String getType() {
        return "$JS.API.TEST";
      }
    };
    PayloadFrame frame = mock(PayloadFrame.class);
    SessionState session = mock(SessionState.class);
    JetStreamRequestManager requests = mock(JetStreamRequestManager.class);
    when(session.getJetStreamRequestManager()).thenReturn(requests);
    when(requests.getSid("_INBOX.request")).thenReturn("sid");
    when(frame.getReplyTo()).thenReturn("_INBOX.request");
    when(frame.getPayload()).thenReturn("{invalid".getBytes(StandardCharsets.UTF_8));

    PayloadFrame response = (PayloadFrame) handler.process("$JS.API.TEST", frame, session);

    assertEquals("_INBOX.request", response.getSubject());
    assertEquals("sid", response.getSubscriptionId());
    assertTrue(new String(response.getPayload(), StandardCharsets.UTF_8).contains("Invalid JetStream request JSON"));
  }

  @Test
  void scalarJsonCannotCrashRequestParser() throws Exception {
    RequestHandler handler = new RequestHandler(new JetStreamFrameHandler[0]) {
      @Override
      public String getType() {
        return "$JS.API.TEST";
      }
    };
    PayloadFrame frame = mock(PayloadFrame.class);
    SessionState session = mock(SessionState.class);
    JetStreamRequestManager requests = mock(JetStreamRequestManager.class);
    when(session.getJetStreamRequestManager()).thenReturn(requests);
    when(frame.getReplyTo()).thenReturn("_INBOX.request");
    when(frame.getPayload()).thenReturn("123".getBytes(StandardCharsets.UTF_8));

    PayloadFrame response = (PayloadFrame) handler.process("$JS.API.TEST", frame, session);

    assertEquals("_INBOX.request", response.getSubject());
    assertTrue(new String(response.getPayload(), StandardCharsets.UTF_8).contains("Invalid JetStream request JSON"));
  }
}
