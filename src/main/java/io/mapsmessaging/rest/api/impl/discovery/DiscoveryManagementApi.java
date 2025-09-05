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

package io.mapsmessaging.rest.api.impl.discovery;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.discovery.DiscoveredServersDTO;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Discovery Management")
@Path(URI_PATH)
public class DiscoveryManagementApi extends DiscoveryBaseRestApi {

  @PUT
  @Path("/server/discovery/start")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Start Discovery agent",
      description = "Starts the mDNS discovery sub-system. Requires authentication if enabled in the configuration.",
      security = {@SecurityRequirement(name = "basicAuth")},
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Start server discovery was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StatusResponse startDiscovery() {
    hasAccess(RESOURCE);
    MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().start();
    return new StatusResponse("success");
  }

  @PUT
  @Path("/server/discovery/stop")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Stop Discovery daemon",
      description = "Stops the mDNS discovery sub-system. Requires authentication if enabled in the configuration.",
      security = {@SecurityRequirement(name = "basicAuth")},
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Stop server discovery was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public boolean stopDiscovery() {
    hasAccess(RESOURCE);
    MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().stop();
    return true;
  }

  @GET
  @Path("/server/discovery")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get discovered servers",
      description = "Retrieve a list of all currently discovered servers, can be filtered with the optional filter. Requires authentication if enabled in the configuration.",
      security = {@SecurityRequirement(name = "basicAuth")},
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Update discovery configuration was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = DiscoveredServers.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public DiscoveredServers getAllDiscoveredServers(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "schemaSupport = TRUE OR systemTopicPrefix IS NOT NULL")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), ((filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : ""));

    DiscoveredServers cachedResponse = getFromCache(key, DiscoveredServers.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : SelectorParser.compile("true");
    List<DiscoveredServersDTO> result =
        MessageDaemon.getInstance()
            .getSubSystemManager()
            .getServerConnectionManager()
            .getServers()
            .stream()
            .filter(parser::evaluate)
            .collect(Collectors.toList());
    DiscoveredServers discoveredServers = new DiscoveredServers();
    discoveredServers.setList(result);
    putToCache(key, discoveredServers);
    return discoveredServers;
  }

  @Data
  @NoArgsConstructor
  private static class DiscoveredServers{
    private List<DiscoveredServersDTO> list;
  }
}
