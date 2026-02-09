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

package io.mapsmessaging.rest.api.impl.interfaces;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.NetworkManagerConfig;
import io.mapsmessaging.dto.helpers.EndPointHelper;
import io.mapsmessaging.dto.helpers.InterfaceInfoHelper;
import io.mapsmessaging.dto.helpers.InterfaceStatusHelper;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.dto.rest.interfaces.InterfaceInfoDTO;
import io.mapsmessaging.dto.rest.interfaces.InterfaceStatusDTO;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.EndPointManager.STATE;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Interface Management")
@Path(URI_PATH + "/server/interface/{endpoint}")
public class InterfaceInstanceApi extends BaseInterfaceApi {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get end point configurations",
      description = "Get the end point configuration specifed by the name. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterfaceInfoDTO.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Endpoint not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getEndPoint(@PathParam("endpoint") String endpoint) {
    hasAccess(RESOURCE);

    UUID endpointId = parseEndpointId(endpoint);
    if (endpointId == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid endpoint id"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    CacheKey key = new CacheKey(uriInfo.getPath(), endpointId.toString());
    InterfaceInfoDTO cachedResponse = getFromCache(key, InterfaceInfoDTO.class);
    if (cachedResponse != null) {
      return Response.ok(cachedResponse, MediaType.APPLICATION_JSON).build();
    }

    List<EndPointManager> endPointManagers =
        MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (endpointId.equals(endPointManager.getUniqueId())) {
        InterfaceInfoDTO responseBody = InterfaceInfoHelper.fromEndPointManager(endPointManager);
        putToCache(key, responseBody);
        return Response.ok(responseBody, MediaType.APPLICATION_JSON).build();
      }
    }

    return Response.status(Response.Status.NOT_FOUND)
        .entity(new StatusResponse("Endpoint not found"))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  @GET
  @Path("/connections")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get end point connections",
      description = "Get current connections on this endpoint. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = EndPointSummaryDTO[].class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Endpoint not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getEndPointConnections(@PathParam("endpoint") String endpoint) {
    hasAccess(RESOURCE);

    UUID endpointId = parseEndpointId(endpoint);
    if (endpointId == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid endpoint id"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    CacheKey key = new CacheKey(uriInfo.getPath(), endpointId.toString());
    EndPointSummaryDTO[] cachedResponse = getFromCache(key, EndPointSummaryDTO[].class);
    if (cachedResponse != null) {
      return Response.ok(cachedResponse, MediaType.APPLICATION_JSON).build();
    }

    EndPointManager endPointManager = findEndPointManager(endpointId);
    if (endPointManager == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("Endpoint not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    EndPointSummaryDTO[] endPointDetails = endPointManager.getEndPointServer()
        .getActiveEndPoints()
        .stream()
        .map(endPoint -> EndPointHelper.buildSummaryDTO(endPointManager.getName(), endPoint))
        .toList()
        .toArray(new EndPointSummaryDTO[0]);

    putToCache(key, endPointDetails);
    return Response.ok(endPointDetails, MediaType.APPLICATION_JSON).build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Update end point configuration",
      description = "Update the configuration supplied for the named endpoint.",
      requestBody = @RequestBody(
          description = "End point configuration to apply",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = EndPointServerConfigDTO.class)
          )
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Endpoint not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response updateInterfaceConfiguration(@PathParam("endpoint") String endpoint, EndPointServerConfigDTO config) throws IOException {
    hasAccess(RESOURCE);

    UUID endpointId = parseEndpointId(endpoint);
    if (endpointId == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid endpoint id"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    if (config == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Request body is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    boolean updated = NetworkManagerConfig.getInstance().update(config);
    if (updated) {
      NetworkManagerConfig.getInstance().save();
      return Response.ok(new StatusResponse("Success"), MediaType.APPLICATION_JSON).build();
    }

    return Response.status(Response.Status.NOT_FOUND)
        .entity(new StatusResponse("Endpoint not found"))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Controls the specific end point",
      description = "Applies the requested state to the configured interface endpoint.",
      requestBody = @RequestBody(
          description = "Requested action to apply to the interface endpoint",
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
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Endpoint not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response manageSpecificInterface(@PathParam("endpoint") String endpoint, @RequestBody RequestedAction requested) {
    hasAccess(RESOURCE);

    UUID endpointId = parseEndpointId(endpoint);
    if (endpointId == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid endpoint id"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    if (requested == null || requested.getState() == null || requested.getState().trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("State is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    STATE state = mapRequestedState(requested.getState());
    if (state == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Unknown state"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    EndPointManager endPointManager = findEndPointManager(endpointId);
    if (endPointManager == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("Endpoint not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    return applyState(state, endPointManager);
  }

  @GET
  @Path("/status")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get end point status",
      description = "Get the current status and metrics for the specified end point.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterfaceStatusDTO.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Endpoint not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getInterfaceStatus(@PathParam("endpoint") String endpoint) {
    hasAccess(RESOURCE);

    UUID endpointId = parseEndpointId(endpoint);
    if (endpointId == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid endpoint id"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    CacheKey key = new CacheKey(uriInfo.getPath(), endpointId.toString());
    InterfaceStatusDTO cachedResponse = getFromCache(key, InterfaceStatusDTO.class);
    if (cachedResponse != null) {
      return Response.ok(cachedResponse, MediaType.APPLICATION_JSON).build();
    }

    EndPointManager endPointManager = findEndPointManager(endpointId);
    if (endPointManager == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("Endpoint not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    InterfaceStatusDTO responseBody = InterfaceStatusHelper.fromServer(endPointManager.getEndPointServer());
    putToCache(key, responseBody);
    return Response.ok(responseBody, MediaType.APPLICATION_JSON).build();
  }

  private UUID parseEndpointId(String endpoint) {
    if (endpoint == null) {
      return null;
    }
    String trimmed = endpoint.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    try {
      return UUID.fromString(trimmed);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private EndPointManager findEndPointManager(UUID endpointId) {
    List<EndPointManager> endPointManagers =
        MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (endpointId.equals(endPointManager.getUniqueId())) {
        return endPointManager;
      }
    }
    return null;
  }

  private STATE mapRequestedState(String requestedState) {
    String state = requestedState.trim();
    if ("stopped".equalsIgnoreCase(state)) {
      return STATE.STOPPED;
    }
    if ("started".equalsIgnoreCase(state)) {
      return STATE.START;
    }
    if ("paused".equalsIgnoreCase(state)) {
      return STATE.PAUSED;
    }
    if ("resumed".equalsIgnoreCase(state)) {
      return STATE.RESUME;
    }
    return null;
  }

  private Response applyState(STATE newState, EndPointManager endPointManager) {
    try {
      boolean changed = false;

      if (newState == STATE.START && endPointManager.getState() == STATE.STOPPED) {
        endPointManager.start();
        changed = true;
      } else if (newState == STATE.STOPPED
          && (endPointManager.getState() == STATE.START || endPointManager.getState() == STATE.PAUSED)) {
        endPointManager.close();
        changed = true;
      } else if (newState == STATE.RESUME && endPointManager.getState() == STATE.PAUSED) {
        endPointManager.resume();
        changed = true;
      } else if (newState == STATE.PAUSED && endPointManager.getState() == STATE.START) {
        endPointManager.pause();
        changed = true;
      }

      if (changed) {
        return Response.ok(new StatusResponse("Success"), MediaType.APPLICATION_JSON).build();
      }
      return Response.ok(new StatusResponse("No change"), MediaType.APPLICATION_JSON).build();
    } catch (IOException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Failed to apply state"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }
}
