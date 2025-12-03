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
import io.mapsmessaging.dto.helpers.EndPointHelper;
import io.mapsmessaging.dto.helpers.IntegrationInfoHelper;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import io.mapsmessaging.dto.rest.integration.IntegrationStatusDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.interfaces.RequestedAction;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Integration Management")
@Path(URI_PATH+"/server/integration/{name}")
public class IntegrationInstanceManagementApi extends IntegrationBaseRestApi {
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get integration by name",
      description = "Retrieves the configuration on the inter-server integration connection. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationInfoDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Integration name was not found"),
      }
  )
  public IntegrationInfoDTO getByNameIntegration(@PathParam("name") String name) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), name);
    IntegrationInfoDTO cachedResponse = getFromCache(key, IntegrationInfoDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      throw new WebApplicationException("Integration not found", Response.Status.NOT_FOUND);
    }

    IntegrationInfoDTO response = IntegrationInfoHelper.fromEndPointConnection(endPointConnection);
    putToCache(key, response);
    return response;
  }

  @GET
  @Path("/connection")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get integration status by name",
      description = "Retrieves the current status on the inter-server integration connection. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = EndPointSummaryDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Integration name was not found")
      }
  )
  public EndPointSummaryDTO getIntegrationConnection(@PathParam("name") String name) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), name);
    EndPointSummaryDTO cachedResponse = getFromCache(key, EndPointSummaryDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      throw new WebApplicationException("Integration not found", Response.Status.NOT_FOUND);
    }

    EndPointSummaryDTO response =
        (endPointConnection.getEndPoint() != null)
            ? EndPointHelper.buildSummaryDTO(name, endPointConnection.getEndPoint())
            : new EndPointSummaryDTO();

    putToCache(key, response);
    return response;
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
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StatusResponse handleIntegrationActionRequest(@PathParam("name") String name, @RequestBody RequestedAction requested, @Context HttpServletResponse response) {
    hasAccess(RESOURCE);
    boolean processed = false;
    if (requested != null && requested.getState() != null) {
      EndPointConnection endPointConnection = locateInstance(name);
      if (endPointConnection == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return new StatusResponse("No such connection");
      }
      if ("stopped".equalsIgnoreCase(requested.getState())) {
        endPointConnection.stop();
        processed = true;
      } else if ("started".equalsIgnoreCase(requested.getState())) {
        endPointConnection.start();
        processed = true;
      } else if ("paused".equalsIgnoreCase(requested.getState())) {
        endPointConnection.pause();
        processed = true;
      } else if ("resumed".equalsIgnoreCase(requested.getState())) {
        endPointConnection.resume();
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
      summary = "Get inter-server status",
      description = "Retrieve the current status for the inter-server specified by name. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationStatusDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public IntegrationStatusDTO getIntegrationStatus(@PathParam("name") String endpointName) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), endpointName);
    IntegrationStatusDTO cachedResponse = getFromCache(key, IntegrationStatusDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    IntegrationStatusDTO response = null;
    EndPointConnection endPointConnection = locateInstance(endpointName);
    if(endPointConnection != null) {
      response = fromConnection(endPointConnection);
      putToCache(key, response);
    }
    return response;
  }


  private EndPointConnection locateInstance(String name) {
    List<EndPointConnection> list =
        MessageDaemon.getInstance()
            .getSubSystemManager()
            .getNetworkConnectionManager()
            .getEndPointConnectionList();
    for (EndPointConnection endPointConnection : list) {
      if (endPointConnection.getConfigName().equalsIgnoreCase(name)) {
        return endPointConnection;
      }
    }
    return null;
  }
}
