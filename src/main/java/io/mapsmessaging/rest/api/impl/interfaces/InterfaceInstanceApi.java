/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.rest.api.impl.interfaces;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.NetworkManagerConfig;
import io.mapsmessaging.dto.helpers.EndPointHelper;
import io.mapsmessaging.dto.helpers.InterfaceInfoHelper;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.dto.rest.interfaces.InterfaceInfoDTO;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.EndPointManager.STATE;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.EndPointDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfaceInstanceApi extends BaseInterfaceApi {

  @GET
  @Path("/server/interface/{endpoint}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get endpoint configuration",
      description = "Returns the endpoint configuration for the specified name. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Endpoint configuration returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Endpoint not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public InterfaceInfoDTO getEndPoint(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), endpointName);
    InterfaceInfoDTO cachedResponse = getFromCache(key, InterfaceInfoDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    List<EndPointManager> endPointManagers =
        MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        InterfaceInfoDTO responseDto = InterfaceInfoHelper.fromEndPointManager(endPointManager);
        putToCache(key, responseDto);
        return responseDto;
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return null;
  }

  @GET
  @Path("/server/interface/{endpoint}/connections")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get endpoint connections",
      description = "Returns current connections on the specified endpoint. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Connections returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Endpoint not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public EndPointDetailResponse getEndPointConnections(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), endpointName);
    EndPointDetailResponse cachedResponse = getFromCache(key, EndPointDetailResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    List<EndPointSummaryDTO> endPointDetails =
        MessageDaemon.getInstance()
            .getSubSystemManager()
            .getNetworkManager()
            .getAll()
            .stream()
            .filter(endPointManager -> isMatch(endpointName, endPointManager))
            .flatMap(endPointManager ->
                endPointManager
                    .getEndPointServer()
                    .getActiveEndPoints()
                    .stream()
                    .map(endPoint -> EndPointHelper.buildSummaryDTO(endPointManager.getName(), endPoint))
            )
            .collect(Collectors.toList());

    EndPointDetailResponse responseDto = new EndPointDetailResponse(endPointDetails);
    putToCache(key, responseDto);
    return responseDto;
  }

  @PUT
  @Path("/server/interface/{endpoint}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update endpoint configuration",
      description = "Updates the configuration for the named endpoint. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Endpoint configuration updated")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Endpoint not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public boolean updateInterfaceConfiguration(
      @PathParam("endpoint") String endpointName,
      EndPointServerConfigDTO config
  ) throws IOException {
    hasAccess(RESOURCE);
    if (endpointName.equals(config.getName()) && NetworkManagerConfig.getInstance().update(config)) {
      NetworkManagerConfig.getInstance().save();
      return true;
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return false;
  }

  @PUT
  @Path("/server/interface/{endpoint}/stop")
  @Operation(
      summary = "Stop endpoint",
      description = "Stops the specified endpoint from accepting new connections. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Endpoint stopped")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Endpoint not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public Response stopInterface(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    Response result = lookup(endpointName, STATE.STOPPED);
    if (result != null) {
      return result;
    }
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/interface/{endpoint}/start")
  @Operation(
      summary = "Start endpoint",
      description = "Starts the specified endpoint, allowing new connections. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Endpoint started")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Endpoint not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public Response startInterface(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    Response result = lookup(endpointName, STATE.START);
    if (result != null) {
      return result;
    }
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/interface/{endpoint}/resume")
  @Operation(
      summary = "Resume endpoint",
      description = "Resumes the specified endpoint if it is paused, allowing new connections. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Endpoint resumed")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Endpoint not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public Response resumeInterface(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    Response result = lookup(endpointName, STATE.RESUME);
    if (result != null) {
      return result;
    }
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/interface/{endpoint}/pause")
  @Operation(
      summary = "Pause endpoint",
      description = "Pauses the specified endpoint, stopping new connections. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Endpoint paused")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Endpoint not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public Response pauseInterface(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    Response result = lookup(endpointName, STATE.PAUSED);
    if (result != null) {
      return result;
    }
    return Response.noContent().build();
  }

  private Response lookup(String endpointName, STATE state) {
    List<EndPointManager> endPointManagers =
        MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
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
