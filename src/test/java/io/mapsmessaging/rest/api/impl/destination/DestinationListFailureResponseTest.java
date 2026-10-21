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

package io.mapsmessaging.rest.api.impl.destination;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.stream.Stream;
import javax.security.auth.login.LoginException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class DestinationListFailureResponseTest {

  @ParameterizedTest
  @MethodSource("failures")
  void failed_session_lookup_preserves_response_for_each_caught_type(Exception failure) {
    DestinationListManagementAPI api = new DestinationListManagementAPI() {
      @Override
      protected void hasAccess(String resource) {
        // Exercise failure mapping independently of access control.
      }

      @Override
      protected HttpSession getSession() {
        return mock(HttpSession.class);
      }

      @Override
      protected Session getAuthenticatedSession() throws LoginException, IOException {
        if (failure instanceof LoginException login) {
          throw login;
        }
        if (failure instanceof IOException io) {
          throw io;
        }
        throw (RuntimeException) failure;
      }
    };
    DestinationPageRequestDTO request = new DestinationPageRequestDTO();
    request.setPrefix("/");
    request.setPageSize(10);

    try (Response response = api.getDestinationPage(request, null)) {
      assertEquals(500, response.getStatus());
      assertEquals("Failed to build destination list: unavailable",
          assertInstanceOf(StatusResponse.class, response.getEntity()).getStatus());
    }
  }

  private static Stream<Exception> failures() {
    return Stream.of(new LoginException("unavailable"), new IOException("unavailable"),
        new IllegalStateException("unavailable"));
  }
}
