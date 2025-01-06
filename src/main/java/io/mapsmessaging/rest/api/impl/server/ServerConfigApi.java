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

package io.mapsmessaging.rest.api.impl.server;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import io.mapsmessaging.rest.cache.CacheKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

@Tag(name = "Server Config Management")
@Path(URI_PATH)
public class ServerConfigApi extends ServerBaseRestApi {

  @GET
  @Path("/server/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get server config",
      description = "Retrieves the current server configuration. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Server configuration returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public MessageDaemonConfigDTO getServerConfig() {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "serverConfig");
    MessageDaemonConfigDTO cachedResponse = getFromCache(key, MessageDaemonConfigDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    MessageDaemonConfigDTO response = MessageDaemon.getInstance().getMessageDaemonConfig();
    putToCache(key, response);
    return response;
  }

  @PUT
  @Path("/server/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update server config",
      description = "Updates and persists the server configuration. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Server config updated")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public Response updateServerConfig(MessageDaemonConfigDTO dto) throws IOException {
    hasAccess(RESOURCE);
    if (MessageDaemon.getInstance().getMessageDaemonConfig().update(dto)) {
      MessageDaemon.getInstance().getMessageDaemonConfig().save();
      removeFromCache(new CacheKey(uriInfo.getPath(), "serverConfig"));
    }
    return Response.ok().build();
  }
}
