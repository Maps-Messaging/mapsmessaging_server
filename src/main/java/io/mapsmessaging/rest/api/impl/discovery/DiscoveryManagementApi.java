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
import io.mapsmessaging.network.discovery.DiscoveryManager;
import io.mapsmessaging.rest.api.impl.interfaces.RequestedAction;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Discovery Management")
@Path(URI_PATH+"/server/discovery")
public class DiscoveryManagementApi extends DiscoveryBaseRestApi {
  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Manages the discovery manager",
      description = "Manages the state of the discovery manager",
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
  public StatusResponse handleDiscoveryActionRequest(@RequestBody RequestedAction requested, @Context HttpServletResponse response) {
    hasAccess(RESOURCE);
    boolean processed = false;
    if (requested != null && requested.getState() != null) {
      DiscoveryManager discoveryManager = MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager();
      if (discoveryManager == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return new StatusResponse("No such connection");
      }
      if ("stopped".equalsIgnoreCase(requested.getState())) {
        discoveryManager.stop();
        processed = true;
      } else if ("started".equalsIgnoreCase(requested.getState())) {
        discoveryManager.start();
        processed = true;
      }
    }
    if (processed) {
      return new StatusResponse("Success");
    }
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    return new StatusResponse("Unknown action");
  }


  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get discovered servers",
      description = "Retrieve a list of all currently discovered servers, can be filtered with the optional filter. Requires authentication if enabled in the configuration.",
      security = {@SecurityRequirement(name = "basicAuth")},
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Update discovery configuration was successful",
              content =  @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = DiscoveredServersDTO[].class)
              )
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public DiscoveredServersDTO[] getAllDiscoveredServers(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "schemaSupport = TRUE OR systemTopicPrefix IS NOT NULL")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), ((filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : ""));

    DiscoveredServersDTO[] cachedResponse = getFromCache(key, DiscoveredServersDTO[].class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : SelectorParser.compile("true");
    DiscoveredServersDTO[] array = MessageDaemon.getInstance()
        .getSubSystemManager()
        .getServerConnectionManager()
        .getServers()
        .stream()
        .filter(parser::evaluate).toArray(DiscoveredServersDTO[]::new);
    putToCache(key, array);
    return array;
  }

}
