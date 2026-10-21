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

package io.mapsmessaging.network.auth;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpClientLifetimeTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void auth0_operation_closes_its_own_client_on_success_or_io_failure(boolean failure) throws Exception {
    TokenGenerator generator = new Auth0TokenGenerator().getInstance(Map.of(
        "domain", "example.invalid", "client_id", "client", "client_secret", "secret", "grant_type", "client_credentials"));
    HttpClient client = client(failure, "{\"access_token\":\"token\"}");
    try (var factory = mockStatic(HttpClient.class)) {
      factory.when(HttpClient::newHttpClient).thenReturn(client);
      if (failure) assertThrows(IOException.class, generator::generate);
      else assertEquals("token", generator.generate());
      verify(client).close();
    }
  }

  @SuppressWarnings("unchecked")
  private HttpClient client(boolean failure, String body) throws Exception {
    HttpClient client = mock(HttpClient.class);
    if (failure) {
      when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("network failure"));
    } else {
      HttpResponse<String> response = mock(HttpResponse.class);
      when(response.statusCode()).thenReturn(200);
      when(response.body()).thenReturn(body);
      when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }
    return client;
  }
}
