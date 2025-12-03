/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.helpers.IntegrationInfoHelper;
import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import io.mapsmessaging.dto.rest.integration.IntegrationStatusDTO;
import io.mapsmessaging.network.NetworkConnectionManager;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.interfaces.RequestedAction;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.IntegrationDetailResponse;
import io.mapsmessaging.rest.responses.IntegrationListStatus;
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Integration Management")
@Path(URI_PATH+"/server/integrations")
public class IntegrationManagementApi extends IntegrationBaseRestApi {

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all inter-server connections",
      description = "Retrieves a list of all inter-server configurations. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationDetailResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public IntegrationDetailResponse getAllIntegrations(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "state = PAUSED")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), filter == null ? "" : filter);
    IntegrationDetailResponse cachedResponse = getFromCache(key, IntegrationDetailResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointConnection> endPointManagers =
        MessageDaemon.getInstance()
            .getSubSystemManager()
            .getNetworkConnectionManager()
            .getEndPointConnectionList();
    ConfigurationProperties global = null;
    List<IntegrationInfoDTO> protocols =
        endPointManagers.stream()
            .map(IntegrationInfoHelper::fromEndPointConnection)
            .filter(info -> parser == null || parser.evaluate(info))
            .toList();
    IntegrationDetailResponse res = new IntegrationDetailResponse(protocols, global);
    putToCache(key, res);
    return res;
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
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StatusResponse handleIntegrationActionRequest(
      RequestedAction requested,
      @Parameter(hidden = true) @Context HttpServletResponse response
  ) {
    hasAccess(RESOURCE);
    boolean processed = false;
    if (requested != null && requested.getState() != null) {
      NetworkConnectionManager networkConnectionManager =
          MessageDaemon.getInstance()
              .getSubSystemManager()
              .getNetworkConnectionManager();

      if ("stopped".equalsIgnoreCase(requested.getState())) {
        networkConnectionManager.stop();
        processed = true;
      } else if ("started".equalsIgnoreCase(requested.getState())) {
        networkConnectionManager.start();
        processed = true;
      } else if ("paused".equalsIgnoreCase(requested.getState())) {
        networkConnectionManager.pause();
        processed = true;
      } else if ("resumed".equalsIgnoreCase(requested.getState())) {
        networkConnectionManager.resume();
        processed = true;
      }
    }
    if (processed) {
      return new StatusResponse("Success");
    }
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    return new StatusResponse("Unknown action");
  }


  @GET
  @Path("/status")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all inter-server status",
      description = "Retrieve all current statuses for the inter-server. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationListStatus.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public IntegrationListStatus getAllIntegrationStatus(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "state = PAUSED")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : "");
    IntegrationListStatus cachedResponse = getFromCache(key, IntegrationListStatus.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<IntegrationStatusDTO> response =
        MessageDaemon.getInstance()
            .getSubSystemManager()
            .getNetworkConnectionManager()
            .getEndPointConnectionList()
            .stream()
            .map(this::fromConnection)
            .filter(status -> parser == null || parser.evaluate(status))
            .collect(Collectors.toList());

    IntegrationListStatus status = new IntegrationListStatus(response);
    putToCache(key, status);
    return status;
  }
}
