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

package io.mapsmessaging.rest.api.impl.server;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.ServerRunner;
import io.mapsmessaging.dto.helpers.ServerStatisticsHelper;
import io.mapsmessaging.dto.helpers.StatusMessageHelper;
import io.mapsmessaging.dto.rest.ServerInfoDTO;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.ServerStatisticsResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Tag(name = "Server Management")
@Path(URI_PATH)
public class ServerDetailsApi extends ServerBaseRestApi {

  @GET
  @Path("/server/details/info")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get server build information",
      description = "Retrieves detailed information about the server build, such as version and configuration details. Uses caching for improved performance."
  )
  public ServerInfoDTO getBuildInfo() {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "buildInfo");
    ServerInfoDTO cachedResponse = getFromCache(key, ServerInfoDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    ServerInfoDTO response = StatusMessageHelper.fromMessageDaemon(MessageDaemon.getInstance());
    putToCache(key, response);
    return response;
  }

  @GET
  @Path("/server/details/stats")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get server statistics",
      description = "Retrieves server usage statistics, including metrics such as CPU usage, memory usage, and active connections. Uses caching for improved performance."
  )
  public ServerStatisticsResponse getStats() {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "serverStats");
    ServerStatisticsResponse cachedResponse = getFromCache(key, ServerStatisticsResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    ServerStatisticsResponse response =
        new ServerStatisticsResponse(ServerStatisticsHelper.create());
    putToCache(key, response);
    return response;
  }

  @GET
  @Path("/server/status")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get server subsystem status",
      description = "Retrieves the current status of all server subsystems, including their operational state (e.g., OK, Warning, or Error). Uses caching for improved performance."
  )
  public List<SubSystemStatusDTO> getServerStatus() {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "serverStats");
    List<SubSystemStatusDTO> cachedResponse = getFromCache(key, List.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    List<SubSystemStatusDTO> response = MessageDaemon.getInstance().getSubSystemStatus();
    putToCache(key, response);
    return response;
  }

  @PUT
  @Path("/server/restart")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Restart the server",
      description = "Restarts the server gracefully, preserving any necessary state before the restart operation begins."
  )
  public StatusResponse restartServer() {
    hasAccess(RESOURCE);
    shutdown(8);
    return new StatusResponse("Restarting");
  }

  @PUT
  @Path("/server/shutdown")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Shut down the server",
      description = "Shuts down the server gracefully. Any necessary cleanup or state preservation is performed before shutting down."
  )
  public StatusResponse shutdownServer() {
    hasAccess(RESOURCE);
    shutdown(0);
    return new StatusResponse("Shutting down");
  }

  private void shutdown(int exitCode) {
    ServerRunner.getExitRunner().deletePidFile(exitCode);
  }
}
