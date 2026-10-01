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

package io.mapsmessaging.state.rest.twins;

import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TwinConfigurationErrorResponseTest {

  @Test
  void read_failure_preserves_status_and_error_text() {
    TwinConfigurationStore store = mock(TwinConfigurationStore.class);
    when(store.getCoreConfig()).thenThrow(new IllegalStateException("unavailable"));
    TestApi api = new TestApi(store);

    try (Response response = api.getCoreConfig()) {
      assertError(response, "Server twin configuration error");
    }
  }

  @Test
  void save_failure_preserves_status_and_error_text() throws IOException {
    TwinConfigurationStore store = mock(TwinConfigurationStore.class);
    TwinCoreConfigDTO config = new TwinCoreConfigDTO();
    doThrow(new IOException("write failed")).when(store).updateCoreConfig(config);
    TestApi api = new TestApi(store);

    try (Response response = api.updateCoreConfig(config)) {
      assertError(response, "Unable to save twin configuration");
    }
  }

  @Test
  void unexpected_update_failure_preserves_status_and_error_text() throws IOException {
    TwinConfigurationStore store = mock(TwinConfigurationStore.class);
    TwinCoreConfigDTO config = new TwinCoreConfigDTO();
    doThrow(new IllegalStateException("unavailable")).when(store).updateCoreConfig(config);
    TestApi api = new TestApi(store);

    try (Response response = api.updateCoreConfig(config)) {
      assertError(response, "Server twin configuration error");
    }
  }

  @Test
  void deleting_drone_invalidates_original_drone_and_mavlink_paths() {
    TestApi api = new TestApi(mock(TwinConfigurationStore.class));

    try (Response response = api.deleteDrone("drone-alpha")) {
      assertEquals(204, response.getStatus());
      assertEquals(List.of(URI_PATH + "/server/twin/config/drone-info",
          URI_PATH + "/server/twin/config/mavlink"), api.invalidatedPaths);
    }
  }

  private static void assertError(Response response, String expected) {
    assertEquals(500, response.getStatus());
    assertEquals(expected, assertInstanceOf(StatusResponse.class, response.getEntity()).getStatus());
  }

  private static class TestApi extends TwinConfigurationApi {
    private final TwinConfigurationStore configurationStore;
    private final List<String> invalidatedPaths = new ArrayList<>();

    private TestApi(TwinConfigurationStore configurationStore) {
      this.configurationStore = configurationStore;
    }

    @Override
    protected void hasAccess(String resource) {
      // Exercise response mapping independently of the authentication service.
    }

    @Override
    TwinConfigurationStore store() {
      return configurationStore;
    }

    @Override
    protected void removeUriFromCache(String path) {
      invalidatedPaths.add(path);
    }
  }
}
