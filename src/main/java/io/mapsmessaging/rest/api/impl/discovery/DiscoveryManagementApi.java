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

package io.mapsmessaging.rest.api.impl.discovery;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.discovery.DiscoveredServersDTO;
import io.mapsmessaging.network.discovery.DiscoveryManager;
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
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Discovery Management")
@Path(URI_PATH + "/server/discovery")
public class DiscoveryManagementApi extends DiscoveryBaseRestApi {

  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Manages the discovery manager",
      description = "Manages the state of the discovery manager",
      requestBody = @RequestBody(
          description = "Requested action to apply to the discovery manager",
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
              description = "Discovery manager not found",
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
  public Response handleDiscoveryActionRequest(RequestedAction requestedAction) {
    hasAccess(RESOURCE);

    if (requestedAction == null) {
      return badRequest("Request body is required");
    }

    String requestedState = requestedAction.getState();
    if (requestedState == null || requestedState.isBlank()) {
      return badRequest("State is required");
    }

    DiscoveryManager discoveryManager;
    try {
      discoveryManager = MessageDaemon.getInstance()
          .getSubSystemManager()
          .getDiscoveryManager();
    } catch (RuntimeException ex) {
      return internalServerError("Unable to resolve discovery manager");
    }

    if (discoveryManager == null) {
      return notFound("Discovery manager is not configured");
    }

    if ("stopped".equalsIgnoreCase(requestedState)) {
      try {
        discoveryManager.stop();
      } catch (RuntimeException ex) {
        return internalServerError("Failed to stop discovery manager");
      }
      return ok(new StatusResponse("Success"));
    }

    if ("started".equalsIgnoreCase(requestedState)) {
      try {
        discoveryManager.start();
      } catch (RuntimeException ex) {
        return internalServerError("Failed to start discovery manager");
      }
      return ok(new StatusResponse("Success"));
    }

    return badRequest("Unknown action");
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get discovered servers",
      description =
          "Retrieve a list of all currently discovered servers, can be filtered with the optional filter. "
              + "Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = DiscoveredServersDTO[].class)
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
  public Response getAllDiscoveredServers(
      @Parameter(
          description = "Optional filter string",
          schema = @Schema(type = "string", example = "schemaSupport = TRUE OR systemTopicPrefix IS NOT NULL")
      )
      @QueryParam("filter") String filter
  ) {
    hasAccess(RESOURCE);

    String filterHash = "";
    if (filter != null && !filter.isBlank()) {
      filterHash = String.valueOf(filter.hashCode());
    }

    CacheKey cacheKey = new CacheKey(uriInfo.getPath(), filterHash);

    DiscoveredServersDTO[] cachedResponse = getFromCache(cacheKey, DiscoveredServersDTO[].class);
    if (cachedResponse != null) {
      return ok(cachedResponse);
    }

    ParserExecutor parserExecutor;
    try {
      if (filter != null && !filter.isBlank()) {
        parserExecutor = SelectorParser.compile(filter);
      } else {
        parserExecutor = SelectorParser.compile("true");
      }
    } catch (ParseException ex) {
      return badRequest("Invalid filter");
    } catch (RuntimeException ex) {
      return internalServerError("Failed to compile filter");
    }

    DiscoveredServersDTO[] discoveredServers;
    try {
      discoveredServers = MessageDaemon.getInstance()
          .getSubSystemManager()
          .getServerConnectionManager()
          .getServers()
          .stream()
          .filter(parserExecutor::evaluate)
          .toArray(DiscoveredServersDTO[]::new);
    } catch (RuntimeException ex) {
      return internalServerError("Failed to resolve discovered servers");
    }

    putToCache(cacheKey, discoveredServers);
    return ok(discoveredServers);
  }
}
