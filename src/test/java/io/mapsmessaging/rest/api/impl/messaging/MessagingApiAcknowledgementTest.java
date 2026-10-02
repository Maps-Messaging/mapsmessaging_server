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

package io.mapsmessaging.rest.api.impl.messaging;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.rest.api.impl.messaging.impl.RestMessageListener;
import io.mapsmessaging.rest.api.impl.messaging.impl.SessionState;
import io.mapsmessaging.rest.handler.SessionTracker;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.responses.TransactionData;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessagingApiAcknowledgementTest {

  private static final String DESTINATION = "/transactions";
  private final String sessionId = UUID.randomUUID().toString();
  private final SubscribedEventManager manager = mock(SubscribedEventManager.class);
  private final RestMessageListener listener = new RestMessageListener();
  private TestApi api;

  @BeforeEach
  void set_up() {
    HttpSession httpSession = mock(HttpSession.class);
    when(httpSession.getId()).thenReturn(sessionId);
    api = new TestApi(httpSession);
    listener.registerEventManager(DESTINATION, null, manager);
    SessionTracker.getSessionStates().setSessionState(sessionId, new SessionState(null, listener));
  }

  @AfterEach
  void clean_up() {
    SessionTracker.getSessionStates().removeSessionState(sessionId);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void successful_batch_processes_all_ids_before_returning_200(boolean commit) {
    Response response = process(commit, DESTINATION, List.of(101L, 102L, 103L));

    assertEquals(200, response.getStatus());
    assertNotNull(((StatusResponse) response.getEntity()).getStatus());
    if (commit) {
      verify(manager).ackReceived(101L);
      verify(manager).ackReceived(102L);
      verify(manager).ackReceived(103L);
    } else {
      verify(manager).rollbackReceived(101L);
      verify(manager).rollbackReceived(102L);
      verify(manager).rollbackReceived(103L);
    }
    verifyNoMoreInteractions(manager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void empty_batch_returns_400_without_processing(boolean commit) {
    assertEquals(400, process(commit, DESTINATION, List.of()).getStatus());
    verifyNoInteractions(manager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void unknown_subscription_returns_400_without_processing(boolean commit) {
    assertEquals(400, process(commit, "/unknown", List.of(101L, 102L)).getStatus());
    verifyNoInteractions(manager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void engine_failure_propagates_without_success_response(boolean commit) {
    IllegalStateException failure = new IllegalStateException("engine failure");
    if (commit) {
      doThrow(failure).when(manager).ackReceived(102L);
    } else {
      doThrow(failure).when(manager).rollbackReceived(102L);
    }
    assertSame(failure, assertThrows(IllegalStateException.class,
        () -> process(commit, DESTINATION, List.of(101L, 102L, 103L))));
    if (commit) {
      verify(manager).ackReceived(101L);
      verify(manager).ackReceived(102L);
    } else {
      verify(manager).rollbackReceived(101L);
      verify(manager).rollbackReceived(102L);
    }
    verifyNoMoreInteractions(manager);
  }

  private Response process(boolean commit, String destination, List<Long> ids) {
    TransactionData data = new TransactionData(destination, ids);
    return commit ? api.commitMessages(data) : api.abortMessages(data);
  }

  private static final class TestApi extends MessagingApi {
    private final HttpSession httpSession;

    private TestApi(HttpSession httpSession) {
      this.httpSession = httpSession;
    }

    @Override
    protected HttpSession getSession() {
      return httpSession;
    }

    @Override
    protected void hasAccess(String resource) {
      // Authorization is outside this focused transaction test; retain the real listener and session registry.
    }
  }
}
