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
 *
 */

package io.mapsmessaging.rest.api.impl.interfaces;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfaceInstanceApi extends BaseInterfaceApi {

  @GET
  @Path("/server/interface/{endpoint}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get end point configurations",
      description = "Get the end point configuration specifed by the name. Requires authentication if enabled in the configuration."
  )
  public InterfaceInfoDTO getEndPoint(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), endpointName);
    InterfaceInfoDTO cachedResponse = getFromCache(key, InterfaceInfoDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        InterfaceInfoDTO response = InterfaceInfoHelper.fromEndPointManager(endPointManager);
        putToCache(key, response);
        return response;
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return null;
  }

  @GET
  @Path("/server/interface/{endpoint}/connections")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get end point connections",
      description = "Get current connections on this endpoint. Requires authentication if enabled in the configuration."
  )
  public EndPointDetailResponse getEndPointConnections(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), endpointName);
    EndPointDetailResponse cachedResponse = getFromCache(key, EndPointDetailResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    List<EndPointSummaryDTO> endPointDetails = MessageDaemon.getInstance()
        .getSubSystemManager()
        .getNetworkManager()
        .getAll()
        .stream()
        .filter(endPointManager -> isMatch(endpointName, endPointManager))
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
  @Path("/server/interface/{endpoint}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update end point configuration",
      description = "Update the configuration supplied for the named endpoint."
  )
  public boolean updateInterfaceConfiguration(@PathParam("endpoint") String endpointName, EndPointServerConfigDTO config) throws IOException {
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
      summary = "Stop the end point",
      description = "Stops the specified end point from accepting new connections and closes connections."
  )
  public Response stopInterface(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    Response response = lookup(endpointName, STATE.STOPPED);
    if (response != null) {
      return response;
    }
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/interface/{endpoint}/start")
  @Operation(
      summary = "Start the end point",
      description = "Starts the specified end point accepting new connections."
  )
  public Response startInterface(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    Response response = lookup(endpointName, STATE.START);
    if (response != null) {
      return response;
    }
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/interface/{endpoint}/resume")
  @Operation(
      summary = "Resume the end point",
      description = "Resume the specified end point accepting new connections."
  )
  public Response resumeInterface(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    Response response = lookup(endpointName, STATE.RESUME);
    if (response != null) {
      return response;
    }
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/interface/{endpoint}/pause")
  @Operation(
      summary = "Pause the end point",
      description = "Pauses the specified end point from accepting new connections."
  )
  public Response pauseInterface(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    Response response = lookup(endpointName, STATE.PAUSED);
    if (response != null) {
      return response;
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
