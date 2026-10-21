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

package io.mapsmessaging.rest.api.impl.config;

import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ConfigManagementErrorResponseTest {

  @ParameterizedTest
  @MethodSource("failedOperations")
  void internal_failure_preserves_status_and_error_text(Supplier<Response> operation) {
    try (Response response = operation.get()) {
      assertEquals(500, response.getStatus());
      assertEquals("Server configuration error",
          assertInstanceOf(StatusResponse.class, response.getEntity()).getStatus());
    }
  }

  private static Stream<Supplier<Response>> failedOperations() {
    ConfigManagementApi api = new ConfigManagementApi() {
      @Override
      protected void hasAccess(String resource) {
        throw new IllegalStateException("access service unavailable");
      }
    };
    return Stream.of(api::getConfig, () -> api.getConfigSection("Discovery"),
        () -> api.updateConfigSection("Discovery", Map.of()));
  }
}
