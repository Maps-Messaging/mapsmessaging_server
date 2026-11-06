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

package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.SubSystemManager;
import io.mapsmessaging.network.NetworkConnectionManager;
import io.mapsmessaging.network.NetworkManager;
import io.mapsmessaging.rest.api.impl.integration.IntegrationInstanceManagementApi;
import io.mapsmessaging.rest.api.impl.interfaces.InterfaceInstanceApi;
import io.mapsmessaging.rest.api.impl.ml.ModelStoreApi;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestResourceValidationResponseTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "invalid"})
  void invalid_endpoint_ids_preserve_error_response(String endpoint) throws IOException {
    InterfaceApi api = new InterfaceApi();
    assertResponse(api.getEndPoint(endpoint), 400, "Invalid endpoint id");
    assertResponse(api.getEndPointConnections(endpoint), 400, "Invalid endpoint id");
    assertResponse(api.updateInterfaceConfiguration(endpoint, null), 400, "Invalid endpoint id");
    assertResponse(api.manageSpecificInterface(endpoint, null), 400, "Invalid endpoint id");
    assertResponse(api.getInterfaceStatus(endpoint), 400, "Invalid endpoint id");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void missing_integration_names_preserve_error_response(String name) {
    IntegrationApi api = new IntegrationApi();
    assertResponse(api.getByNameIntegration(name), 400, "Name is required");
    assertResponse(api.getIntegrationConnection(name), 400, "Name is required");
    assertResponse(api.handleIntegrationActionRequest(name, null), 400, "Name is required");
    assertResponse(api.getIntegrationStatus(name), 400, "Name is required");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void missing_model_names_preserve_error_response(String name) throws IOException {
    ModelApi api = new ModelApi();
    assertResponse(api.uploadModel(name, null), 400, "Model name must not be blank");
    assertResponse(api.getModel(name), 400, "Model name must not be blank");
    assertResponse(api.modelExists(name), 400, "Model name must not be blank");
    assertResponse(api.deleteModel(name), 400, "Model name must not be blank");
  }

  @Test
  void missing_endpoints_preserve_not_found_response() {
    MessageDaemon daemon = mock(MessageDaemon.class);
    SubSystemManager systems = mock(SubSystemManager.class);
    NetworkManager network = mock(NetworkManager.class);
    when(daemon.getSubSystemManager()).thenReturn(systems);
    when(systems.getNetworkManager()).thenReturn(network);
    when(network.getAll()).thenReturn(List.of());
    try (MockedStatic<MessageDaemon> singleton = mockStatic(MessageDaemon.class)) {
      singleton.when(MessageDaemon::getInstance).thenReturn(daemon);
      InterfaceApi api = new InterfaceApi();
      String endpoint = UUID.randomUUID().toString();
      assertResponse(api.getEndPoint(endpoint), 404, "Endpoint not found");
      assertResponse(api.getEndPointConnections(endpoint), 404, "Endpoint not found");
      assertResponse(api.getInterfaceStatus(endpoint), 404, "Endpoint not found");
    }
  }

  @Test
  void missing_integrations_preserve_not_found_response() {
    MessageDaemon daemon = mock(MessageDaemon.class);
    SubSystemManager systems = mock(SubSystemManager.class);
    NetworkConnectionManager connections = mock(NetworkConnectionManager.class);
    when(daemon.getSubSystemManager()).thenReturn(systems);
    when(systems.getNetworkConnectionManager()).thenReturn(connections);
    when(connections.getEndPointConnectionList()).thenReturn(List.of());
    try (MockedStatic<MessageDaemon> singleton = mockStatic(MessageDaemon.class)) {
      singleton.when(MessageDaemon::getInstance).thenReturn(daemon);
      IntegrationApi api = new IntegrationApi();
      assertResponse(api.getByNameIntegration("missing"), 404, "Integration not found");
      assertResponse(api.getIntegrationConnection("missing"), 404, "Integration not found");
      assertResponse(api.getIntegrationStatus("missing"), 404, "Integration not found");
    }
  }

  @Test
  void unavailable_model_store_preserves_not_supported_response() throws IOException {
    MessageDaemon daemon = mock(MessageDaemon.class);
    SubSystemManager systems = mock(SubSystemManager.class);
    when(daemon.getSubSystemManager()).thenReturn(systems);
    try (MockedStatic<MessageDaemon> singleton = mockStatic(MessageDaemon.class)) {
      singleton.when(MessageDaemon::getInstance).thenReturn(daemon);
      ModelApi api = new ModelApi();
      assertResponse(api.uploadModel("model", new ByteArrayInputStream(new byte[]{1})),
          406, "Failure, ML not supported");
      assertResponse(api.getModel("model"), 406, "Failure, ML not supported");
      assertResponse(api.deleteModel("model"), 406, "Failure, ML not supported");
      assertResponse(api.listModels(), 406, "Failure, ML not supported");
      verify(api.servletResponse, times(4)).setStatus(406);
    }
  }

  private static void assertResponse(Response response, int status, String message) {
    try (response) {
      assertEquals(status, response.getStatus());
      assertEquals(message, assertInstanceOf(StatusResponse.class, response.getEntity()).getStatus());
    }
  }

  private static UriInfo uri() {
    UriInfo uri = mock(UriInfo.class);
    when(uri.getPath()).thenReturn("test/resource");
    return uri;
  }

  private static class InterfaceApi extends InterfaceInstanceApi {
    private InterfaceApi() {
      uriInfo = uri();
    }

    @Override
    protected void hasAccess(String resource) {
      // Isolate response contracts from the authentication service.
    }

    @Override
    protected <T> T getFromCache(CacheKey key, Class<T> type) {
      return null;
    }
  }

  private static class IntegrationApi extends IntegrationInstanceManagementApi {
    private IntegrationApi() {
      uriInfo = uri();
    }

    @Override
    protected void hasAccess(String resource) {
      // Isolate response contracts from the authentication service.
    }

    @Override
    protected <T> T getFromCache(CacheKey key, Class<T> type) {
      return null;
    }
  }

  private static class ModelApi extends ModelStoreApi {
    private final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

    private ModelApi() {
      response = servletResponse;
    }

    @Override
    protected void hasAccess(String resource) {
      // Isolate response contracts from the authentication service.
    }
  }
}
