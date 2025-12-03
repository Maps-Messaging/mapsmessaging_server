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
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Config Management")
@Path(URI_PATH+"/server/config")
public class ServerConfigApi extends ServerBaseRestApi {

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve server configuration",
      description = "Fetches the current server configuration settings as a JSON object. Uses caching for improved performance.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageDaemonConfigDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
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
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update server configuration",
      description = "Updates the server configuration with the provided settings. Saves changes to disk and clears relevant cache entries to ensure consistency.",
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
  public StatusResponse updateServerConfig(MessageDaemonConfigDTO dto) throws IOException {
    hasAccess(RESOURCE);
    if (MessageDaemon.getInstance().getMessageDaemonConfig().update(dto)) {
      MessageDaemon.getInstance().getMessageDaemonConfig().save();
      removeFromCache(new CacheKey(uriInfo.getPath(), "serverConfig"));
    }
    return new StatusResponse("Success");
  }
}
