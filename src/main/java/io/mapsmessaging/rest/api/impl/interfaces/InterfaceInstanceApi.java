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
import io.mapsmessaging.rest.responses.EndPointDetailResponse;
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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Interface Management")
@Path(URI_PATH+"/server/interfaces/{endpoint}")
public class InterfaceInstanceApi extends BaseInterfaceApi {

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get end point configurations",
      description = "Get the end point configuration specifed by the name. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterfaceInfoDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Endpoint not found"),
      }
  )
  public InterfaceInfoDTO getEndPoint(@PathParam("endpoint") String uniqueId) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), uniqueId);
    InterfaceInfoDTO cachedResponse = getFromCache(key, InterfaceInfoDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if(endPointManager.getUniqueId().toString().equals(uniqueId)) {
        InterfaceInfoDTO response = InterfaceInfoHelper.fromEndPointManager(endPointManager);
        putToCache(key, response);
        return response;
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return null;
  }

  @GET
  @Path("/connections")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get end point connections",
      description = "Get current connections on this endpoint. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = EndPointDetailResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public EndPointDetailResponse getEndPointConnections(@PathParam("endpoint") String uniqueId) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), uniqueId);
    EndPointDetailResponse cachedResponse = getFromCache(key, EndPointDetailResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    List<EndPointSummaryDTO> endPointDetails = MessageDaemon.getInstance()
        .getSubSystemManager()
        .getNetworkManager()
        .getAll()
        .stream()
        .filter(endPointManager -> isMatch(uniqueId, endPointManager))
        .flatMap(endPointManager -> endPointManager.getEndPointServer()
            .getActiveEndPoints()
            .stream()
            .map(endPoint -> EndPointHelper.buildSummaryDTO(endPointManager.getName(), endPoint)))
        .collect(Collectors.toList());

    EndPointDetailResponse response = new EndPointDetailResponse(endPointDetails);
    putToCache(key, response);
    return response;
  }

  @PUT
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update end point configuration",
      description = "Update the configuration supplied for the named endpoint.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Endpoint not found"),
      }
  )
  public StatusResponse updateInterfaceConfiguration(@PathParam("endpoint") String uniqueId, EndPointServerConfigDTO config) throws IOException {
    hasAccess(RESOURCE);
    if (NetworkManagerConfig.getInstance().update(config)) {
      NetworkManagerConfig.getInstance().save();
      return new StatusResponse("Success");
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return new StatusResponse("Failed");
  }

  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Controls the specific end point",
      description = "Applies the requested state to all configured interface endpoints.",
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
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StatusResponse manageSpecificInterface(@PathParam("endpoint") String uniqueId, @RequestBody RequestedAction requested, @Context HttpServletResponse response) {
    hasAccess(RESOURCE);
    STATE state = null;
    if(requested != null && requested.getState() != null) {
      if("stopped".equalsIgnoreCase(requested.getState())) {
        state = STATE.STOPPED;
      }
      else if("started".equalsIgnoreCase(requested.getState())) {
        state = STATE.START;
      }
      else if("paused".equalsIgnoreCase(requested.getState())) {
        state = STATE.PAUSED;
      }
      else if("resumed".equalsIgnoreCase(requested.getState())) {
        state = STATE.RESUME;
      }
    }
    if(state != null && lookup(uniqueId, state) != null) {
      return new StatusResponse("Success");
    }
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    return new StatusResponse("Unknown state");
  }


  @GET
  @Path("/status")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get end point status",
      description = "Get the current status and metrics for the specified end point.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterfaceStatusDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public InterfaceStatusDTO getInterfaceStatus(@PathParam("endpoint") String uniqueId) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), uniqueId);
    InterfaceStatusDTO cachedResponse = getFromCache(key, InterfaceStatusDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(uniqueId, endPointManager)) {
        InterfaceStatusDTO response = InterfaceStatusHelper.fromServer(endPointManager.getEndPointServer());
        putToCache(key, response);
        return response;
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return null;
  }

  private Response lookup(String uniqueId, STATE state) {
    List<EndPointManager> endPointManagers =
        MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(uniqueId, endPointManager)) {
        return handleRequest(state, endPointManager);
      }
    }
    return null;
  }

  private Response handleRequest(STATE newState, EndPointManager endPointManager) {

    try {
      if (newState == STATE.START && endPointManager.getState() == STATE.STOPPED) {
        endPointManager.start();
        return Response.ok().build();
      } else if (newState == STATE.STOPPED
          && (endPointManager.getState() == STATE.START
          || endPointManager.getState() == STATE.PAUSED)) {
        endPointManager.close();
        return Response.ok().build();
      } else if (newState == STATE.RESUME && endPointManager.getState() == STATE.PAUSED) {
        endPointManager.resume();
        return Response.ok().build();
      } else if (newState == STATE.PAUSED && endPointManager.getState() == STATE.START) {
        endPointManager.pause();
        return Response.ok().build();
      }
    } catch (IOException e) {
      return Response.serverError().entity(e).build();
    }
    return Response.noContent().build();
  }
}
