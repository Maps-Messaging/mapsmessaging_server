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

package io.mapsmessaging.rest.api.impl.integration;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.EndPointHelper;
import io.mapsmessaging.dto.helpers.IntegrationInfoHelper;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import io.mapsmessaging.dto.rest.integration.IntegrationStatusDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.interfaces.RequestedAction;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Integration Management")
@Path(URI_PATH + "/server/integration/{name}")
public class IntegrationInstanceManagementApi extends IntegrationBaseRestApi {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get integration by name",
      description = "Retrieves the configuration on the inter-server integration connection. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = IntegrationInfoDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Integration name was not found",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response getByNameIntegration(@PathParam("name") String name) {
    hasAccess(RESOURCE);

    if (name == null || name.isBlank()) {
      return badRequest("Name is required");
    }

    CacheKey cacheKey = new CacheKey(uriInfo.getPath(), name);

    IntegrationInfoDTO cachedResponse = getFromCache(cacheKey, IntegrationInfoDTO.class);
    if (cachedResponse != null) {
      return ok(cachedResponse);
    }

    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      return notFound("Integration not found");
    }

    IntegrationInfoDTO response;
    try {
      response = IntegrationInfoHelper.fromEndPointConnection(endPointConnection);
    } catch (RuntimeException ex) {
      return internalServerError("Failed to build integration info");
    }

    putToCache(cacheKey, response);
    return ok(response);
  }

  @GET
  @Path("/connection")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get integration connection status by name",
      description = "Retrieves the current connection summary for the inter-server integration connection. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = EndPointSummaryDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Integration name was not found",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response getIntegrationConnection(@PathParam("name") String name) {
    hasAccess(RESOURCE);

    if (name == null || name.isBlank()) {
      return badRequest("Name is required");
    }

    CacheKey cacheKey = new CacheKey(uriInfo.getPath(), name);

    EndPointSummaryDTO cachedResponse = getFromCache(cacheKey, EndPointSummaryDTO.class);
    if (cachedResponse != null) {
      return ok(cachedResponse);
    }

    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      return notFound("Integration not found");
    }

    EndPointSummaryDTO response;
    try {
      if (endPointConnection.getEndPoint() != null) {
        response = EndPointHelper.buildSummaryDTO(name, endPointConnection.getEndPoint());
      } else {
        response = new EndPointSummaryDTO();
      }
    } catch (RuntimeException ex) {
      return internalServerError("Failed to build endpoint summary");
    }

    putToCache(cacheKey, response);
    return ok(response);
  }

  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Manages inter-server connection",
      description = "Handles state for the inter-server connection",
      requestBody = @RequestBody(
          description = "Requested action to apply to inter-server connection",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = RequestedAction.class)
          )
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Integration name was not found",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response handleIntegrationActionRequest(
      @PathParam("name") String name,
      RequestedAction requestedAction
  ) {
    hasAccess(RESOURCE);

    if (name == null || name.isBlank()) {
      return badRequest("Name is required");
    }

    if (requestedAction == null) {
      return badRequest("Request body is required");
    }

    String requestedState = requestedAction.getState();
    if (requestedState == null || requestedState.isBlank()) {
      return badRequest("State is required");
    }

    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      return notFound("Integration not found");
    }

    boolean processed = false;

    try {
      if ("stopped".equalsIgnoreCase(requestedState)) {
        endPointConnection.stop();
        processed = true;
      } else if ("started".equalsIgnoreCase(requestedState)) {
        endPointConnection.start();
        processed = true;
      } else if ("paused".equalsIgnoreCase(requestedState)) {
        endPointConnection.pause();
        processed = true;
      } else if ("resumed".equalsIgnoreCase(requestedState)) {
        endPointConnection.resume();
        processed = true;
      }
    } catch (RuntimeException ex) {
      return internalServerError("Failed to apply requested action");
    }

    if (processed) {
      return ok(new StatusResponse("Success"));
    }

    return badRequest("Unknown action");
  }

  @GET
  @Path("/status")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get inter-server status",
      description = "Retrieve the current status for the inter-server specified by name. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = IntegrationStatusDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Integration name was not found",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response getIntegrationStatus(@PathParam("name") String name) {
    hasAccess(RESOURCE);

    if (name == null || name.isBlank()) {
      return badRequest("Name is required");
    }

    CacheKey cacheKey = new CacheKey(uriInfo.getPath(), name);

    IntegrationStatusDTO cachedResponse = getFromCache(cacheKey, IntegrationStatusDTO.class);
    if (cachedResponse != null) {
      return ok(cachedResponse);
    }

    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      return notFound("Integration not found");
    }

    IntegrationStatusDTO response;
    try {
      response = fromConnection(endPointConnection);
    } catch (RuntimeException ex) {
      return internalServerError("Failed to build integration status");
    }

    putToCache(cacheKey, response);
    return ok(response);
  }

  private EndPointConnection locateInstance(String name) {
    List<EndPointConnection> endPointConnections =
        MessageDaemon.getInstance()
            .getSubSystemManager()
            .getNetworkConnectionManager()
            .getEndPointConnectionList();

    if (endPointConnections == null) {
      return null;
    }

    for (EndPointConnection endPointConnection : endPointConnections) {
      if (endPointConnection == null) {
        continue;
      }
      String configName = endPointConnection.getConfigName();
      if (configName != null && configName.equalsIgnoreCase(name)) {
        return endPointConnection;
      }
    }

    return null;
  }
}
