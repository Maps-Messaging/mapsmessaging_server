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
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumerPaginationTest {

  @Test
  void emptyNamesRequestUsesDefaultPagination() throws Exception {
    PayloadFrame frame = request();
    SessionState session = session();
    PayloadFrame response = (PayloadFrame) new NamesHandler().handle(frame, null, session);
    JsonObject body = com.google.gson.JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
    assertEquals(1024, body.get("limit").getAsInt());
    assertEquals(0, body.get("total").getAsInt());
  }

  @Test
  void oversizedListPageIsCappedAt256() throws Exception {
    PayloadFrame frame = request();
    SessionState session = session();
    JsonObject json = new JsonObject();
    json.addProperty("limit", Integer.MAX_VALUE);
    PayloadFrame response = (PayloadFrame) new ListHandler().handle(frame, json, session);
    JsonObject body = com.google.gson.JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
    assertEquals(256, body.get("limit").getAsInt());
    assertEquals(0, body.get("total").getAsInt());
  }

  private PayloadFrame request() {
    PayloadFrame frame = mock(PayloadFrame.class);
    when(frame.getSubject()).thenReturn("$JS.API.CONSUMER.NAMES.stream");
    when(frame.getReplyTo()).thenReturn("_INBOX.page");
    return frame;
  }

  private SessionState session() {
    SessionState session = mock(SessionState.class);
    JetStreamRequestManager requests = mock(JetStreamRequestManager.class);
    when(session.getJetStreamRequestManager()).thenReturn(requests);
    when(session.getNamedConsumers()).thenReturn(Map.of());
    when(requests.getSid("_INBOX.page")).thenReturn("1");
    return session;
  }
}
