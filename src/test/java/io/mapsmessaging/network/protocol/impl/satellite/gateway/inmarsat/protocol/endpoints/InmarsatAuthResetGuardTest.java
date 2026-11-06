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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.endpoints;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InmarsatAuthResetGuardTest {

  @ParameterizedTest
  @CsvSource({"401,true,1", "401,false,0", "200,true,0", "403,true,0"})
  @SuppressWarnings("unchecked")
  void reset_requires_unauthorized_response_and_callback(
      int status, boolean configured, int expectedResets) throws Exception {
    HttpClient http = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.body()).thenReturn("{}");
    doReturn(response).when(http).send(any(), any());
    AtomicInteger resets = new AtomicInteger();
    BaseInmarsatClient client = new BaseInmarsatClient(URI.create("https://example.invalid/"),
        http, new Gson(), configured ? resets::incrementAndGet : null) {};
    HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.invalid/test")).build();

    if (status == 200) {
      assertDoesNotThrow(() -> client.sendVoid(request));
    } else {
      assertThrows(RuntimeException.class, () -> client.sendVoid(request));
    }
    assertEquals(expectedResets, resets.get());
    verify(http).send(any(), any());
  }
}
