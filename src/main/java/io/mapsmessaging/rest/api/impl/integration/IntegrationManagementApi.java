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
import io.mapsmessaging.dto.helpers.IntegrationInfoHelper;
import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import io.mapsmessaging.dto.rest.integration.IntegrationStatusDTO;
import io.mapsmessaging.network.NetworkConnectionManager;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Integration Management")
@Path(URI_PATH + "/server/integrations")
public class IntegrationManagementApi extends IntegrationBaseRestApi {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all inter-server connections",
      description = "Retrieves a list of all inter-server configurations. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = IntegrationInfoDTO[].class)
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
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response getAllIntegrations(
      @Parameter(
          description = "Optional filter string",
          schema = @Schema(type = "string", example = "state = PAUSED")
      )
      @QueryParam("filter") String filter
  ) {
    hasAccess(RESOURCE);

    List<IntegrationInfoDTO> protocols = new ArrayList<>();
    String cacheDiscriminator = "";
    if (filter != null && !filter.isBlank()) {
      cacheDiscriminator = filter;
    }

    CacheKey cacheKey = new CacheKey(uriInfo.getPath(), cacheDiscriminator);

    IntegrationInfoDTO[] cachedResponse = getFromCache(cacheKey, IntegrationInfoDTO[].class);
    if (cachedResponse != null) {
      return ok(cachedResponse);
    }

    ParserExecutor parserExecutor = null;
    if (filter != null && !filter.isBlank()) {
      try {
        parserExecutor = SelectorParser.compile(filter);
      } catch (ParseException ex) {
        return badRequest("Invalid filter");
      } catch (RuntimeException ex) {
        return internalServerError("Failed to compile filter");
      }
    }

    NetworkConnectionManager networkConnectionManager;
    try {
      networkConnectionManager = MessageDaemon.getInstance()
          .getSubSystemManager()
          .getNetworkConnectionManager();
    } catch (RuntimeException ex) {
      return internalServerError("Unable to resolve network connection manager");
    }

    List<EndPointConnection> endPointConnections;
    try {
      endPointConnections = networkConnectionManager.getEndPointConnectionList();
    } catch (RuntimeException ex) {
      return internalServerError("Failed to resolve endpoint connection list");
    }

    if (endPointConnections == null) {
      endPointConnections = new ArrayList<>();
    }


    for (EndPointConnection endPointConnection : endPointConnections) {
      if (endPointConnection == null) {
        continue;
      }

      IntegrationInfoDTO integrationInfoDTO;
      try {
        integrationInfoDTO = IntegrationInfoHelper.fromEndPointConnection(endPointConnection);
      } catch (RuntimeException ex) {
        return internalServerError("Failed to build integration info list");
      }

      if (parserExecutor == null || parserExecutor.evaluate(integrationInfoDTO)) {
        protocols.add(integrationInfoDTO);
      }
    }
    IntegrationInfoDTO[] response = protocols.toArray(new IntegrationInfoDTO[0]);
    putToCache(cacheKey, response);
    return ok(response);
  }

  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Manages all inter-server connections",
      description = "Handles state for all inter-server connections",
      requestBody = @RequestBody(
          description = "Requested action to apply to all inter-server connections",
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
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response handleIntegrationActionRequest(RequestedAction requestedAction) {
    hasAccess(RESOURCE);

    if (requestedAction == null) {
      return badRequest("Request body is required");
    }

    String requestedState = requestedAction.getState();
    if (requestedState == null || requestedState.isBlank()) {
      return badRequest("State is required");
    }

    NetworkConnectionManager networkConnectionManager;
    try {
      networkConnectionManager = MessageDaemon.getInstance()
          .getSubSystemManager()
          .getNetworkConnectionManager();
    } catch (RuntimeException ex) {
      return internalServerError("Unable to resolve network connection manager");
    }

    boolean processed = false;

    try {
      if ("stopped".equalsIgnoreCase(requestedState)) {
        networkConnectionManager.stop();
        processed = true;
      } else if ("started".equalsIgnoreCase(requestedState)) {
        networkConnectionManager.start();
        processed = true;
      } else if ("paused".equalsIgnoreCase(requestedState)) {
        networkConnectionManager.pause();
        processed = true;
      } else if ("resumed".equalsIgnoreCase(requestedState)) {
        networkConnectionManager.resume();
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
      summary = "Get all inter-server status",
      description = "Retrieve all current statuses for the inter-server. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = IntegrationStatusDTO[].class)
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
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response getAllIntegrationStatus(
      @Parameter(
          description = "Optional filter string",
          schema = @Schema(type = "string", example = "state = PAUSED")
      )
      @QueryParam("filter") String filter
  ) {
    hasAccess(RESOURCE);

    String filterHash = "";
    if (filter != null && !filter.isBlank()) {
      filterHash = String.valueOf(filter.hashCode());
    }

    CacheKey cacheKey = new CacheKey(uriInfo.getPath(), filterHash);

    IntegrationStatusDTO[] cachedResponse = getFromCache(cacheKey, IntegrationStatusDTO[].class);
    if (cachedResponse != null) {
      return ok(cachedResponse);
    }

    ParserExecutor parserExecutor = null;
    if (filter != null && !filter.isBlank()) {
      try {
        parserExecutor = SelectorParser.compile(filter);
      } catch (ParseException ex) {
        return badRequest("Invalid filter");
      } catch (RuntimeException ex) {
        return internalServerError("Failed to compile filter");
      }
    }

    NetworkConnectionManager networkConnectionManager;
    try {
      networkConnectionManager = MessageDaemon.getInstance()
          .getSubSystemManager()
          .getNetworkConnectionManager();
    } catch (RuntimeException ex) {
      return internalServerError("Unable to resolve network connection manager");
    }

    List<EndPointConnection> endPointConnections;
    try {
      endPointConnections = networkConnectionManager.getEndPointConnectionList();
    } catch (RuntimeException ex) {
      return internalServerError("Failed to resolve endpoint connection list");
    }

    if (endPointConnections == null) {
      endPointConnections = new ArrayList<>();
    }

    List<IntegrationStatusDTO> statuses = new ArrayList<>();
    for (EndPointConnection endPointConnection : endPointConnections) {
      if (endPointConnection == null) {
        continue;
      }

      IntegrationStatusDTO statusDTO;
      try {
        statusDTO = fromConnection(endPointConnection);
      } catch (RuntimeException ex) {
        return internalServerError("Failed to build integration status list");
      }

      if (parserExecutor == null || parserExecutor.evaluate(statusDTO)) {
        statuses.add(statusDTO);
      }
    }

    IntegrationStatusDTO[] response = statuses.toArray(new IntegrationStatusDTO[0]);

    putToCache(cacheKey, response);
    return ok(response);
  }

}
