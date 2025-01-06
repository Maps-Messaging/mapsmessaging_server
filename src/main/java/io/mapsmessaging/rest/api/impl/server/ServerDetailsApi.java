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
  //  @ApiOperation(value = "Retrieve the server statistics")
  public String restartServer() {
    hasAccess(RESOURCE);
    shutdown(8);
    return "{\"status\":\"Restarting\"}";
  }

  @PUT
  @Path("/server/shutdown")
  @Produces({MediaType.APPLICATION_JSON})
  //  @ApiOperation(value = "Retrieve the server statistics")
  public String shutdownServer() {
    hasAccess(RESOURCE);
    shutdown(0);
    return "{\"status\":\"Shutting down\"}";
  }

  private void shutdown(int exitCode) {
    ServerRunner.getExitRunner().deletePidFile(exitCode);
  }
}
