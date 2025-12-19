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

package io.mapsmessaging.rest.api.impl.server;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.ServerStatisticsHelper;
import io.mapsmessaging.dto.helpers.StatusMessageHelper;
import io.mapsmessaging.dto.rest.ServerInfoDTO;
import io.mapsmessaging.dto.rest.ServerStatisticsDTO;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.ServerStatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Management")
@Path(URI_PATH+"/server/details")
public class ServerDetailsApi extends ServerBaseRestApi {

  @GET
  @Path("/info")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get server build information",
      description = "Retrieves detailed information about the server build, such as version and configuration details. Uses caching for improved performance.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = ServerInfoDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
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
  @Path("/stats")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get server statistics",
      description = "Retrieves server usage statistics, including metrics such as CPU usage, memory usage, and active connections. Uses caching for improved performance.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = ServerStatisticsDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public ServerStatisticsDTO getStats() {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "serverStats");
    ServerStatisticsDTO cachedResponse = getFromCache(key, ServerStatisticsDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    // Fetch and cache response
    ServerStatisticsDTO dto = ServerStatisticsHelper.create();
    putToCache(key, dto);
    return dto;
  }

}
