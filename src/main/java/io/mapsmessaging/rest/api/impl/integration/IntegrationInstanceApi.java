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

package io.mapsmessaging.rest.api.impl.integration;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.EndPointHelper;
import io.mapsmessaging.dto.helpers.IntegrationInfoHelper;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.cache.CacheKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Tag(name = "Server integration Management")
@Path(URI_PATH)
public class IntegrationInstanceApi extends IntegrationBaseRestApi {

  @GET
  @Path("/server/integration/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get integration by name",
      description = "Retrieves the configuration on the inter-server integration connection. Requires authentication if enabled in the configuration."
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
  @Path("/server/integration/{name}/connection")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get integration status by name",
      description = "Retrieves the current status on the inter-server integration connection. Requires authentication if enabled in the configuration."
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

  @PUT
  @Path("/server/integration/{name}/stop")
  @Operation(
      summary = "Stop integration by name",
      description = "Stops the inter-server connection specified by the name, if started else nothing changes. Requires authentication if enabled in the configuration."
  )
  public Response stopIntegration(@PathParam("name") String name) {
    hasAccess(RESOURCE);
    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    endPointConnection.stop();
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/integration/{name}/start")
  @Operation(
      summary = "Start integration by name",
      description = "Starts the inter-server connection specified by the name, if stopped else nothing changes. Requires authentication if enabled in the configuration."
  )
  public Response startIntegration(@PathParam("name") String name) {
    hasAccess(RESOURCE);
    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    endPointConnection.start();
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/integration/{name}/resume")
  @Operation(
      summary = "Resume integration by name",
      description = "Resumes the inter-server connection specified by the name, if paused else nothing changes. Requires authentication if enabled in the configuration."
  )
  public Response resumeIntegration(@PathParam("name") String name) {
    hasAccess(RESOURCE);
    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    endPointConnection.resume();
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/integration/{name}/pause")
  @Operation(
      summary = "Pause integration by name",
      description = "Pauses the inter-server connection specified by the name, if started else nothing changes. Requires authentication if enabled in the configuration."
  )
  public Response pauseIntegration(@PathParam("name") String name) {
    hasAccess(RESOURCE);
    EndPointConnection endPointConnection = locateInstance(name);
    if (endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    endPointConnection.pause();
    return Response.noContent().build();
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
