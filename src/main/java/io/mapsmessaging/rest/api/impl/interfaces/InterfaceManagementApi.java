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

package io.mapsmessaging.rest.api.impl.interfaces;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.InterfaceInfoHelper;
import io.mapsmessaging.dto.helpers.InterfaceStatusHelper;
import io.mapsmessaging.dto.rest.interfaces.InterfaceInfoDTO;
import io.mapsmessaging.dto.rest.interfaces.InterfaceStatusDTO;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.NetworkManager;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.InterfaceDetailResponse;
import io.mapsmessaging.rest.responses.InterfaceStatusResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Interface Management")
@Path(URI_PATH+"/server/interfaces")
public class InterfaceManagementApi extends BaseInterfaceApi {

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all end point details",
      description = "get all end point configuration details, filtered with the optional filter.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterfaceDetailResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public InterfaceDetailResponse getAllInterfaces(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "state = 'started'")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : "");
    InterfaceDetailResponse cachedResponse = getFromCache(key, InterfaceDetailResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers =
        MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();

    List<InterfaceInfoDTO> protocols =
        endPointManagers.stream()
            .map(InterfaceInfoHelper::fromEndPointManager)
            .filter(protocol -> parser == null || parser.evaluate(protocol))
            .collect(Collectors.toList());

    InterfaceDetailResponse response = new InterfaceDetailResponse(protocols);

    // Cache the response
    putToCache(key, response);
    return response;
  }

  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Manages all end points",
      description = "Manages actions on all endpoints.",
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
  public StatusResponse handleInterfaceActionRequest(RequestedAction requested, @Context HttpServletResponse response) {
    hasAccess(RESOURCE);
    boolean processed = false;
    if(requested != null && requested.getState() != null) {
      NetworkManager networkManager = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager();
      if("stopped".equalsIgnoreCase(requested.getState())) {
        networkManager.stopAll();
        processed = true;
      }
      else if("started".equalsIgnoreCase(requested.getState())) {
        networkManager.startAll();
        processed = true;
      }
      else if("paused".equalsIgnoreCase(requested.getState())) {
        networkManager.pauseAll();
        processed = true;
      }
      else if("resumed".equalsIgnoreCase(requested.getState())) {
        networkManager.resumeAll();
        processed = true;
      }
    }
    if(processed) {
      return new StatusResponse("Success");
    }
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    return new StatusResponse("Unknown action");
  }

  @GET
  @Path("/status")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all end point status",
      description = "Get all end point statuses and metrics, fitlered with the optional filter.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterfaceStatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )

  public InterfaceStatusResponse getAllInterfaceStatus(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "state = 'started'")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : "");
    InterfaceStatusResponse cachedResponse = getFromCache(key, InterfaceStatusResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();

    List<InterfaceStatusDTO> list =
        endPointManagers.stream()
            .map(endPointManager -> InterfaceStatusHelper.fromServer(endPointManager.getEndPointServer()))
            .filter(status -> parser == null || parser.evaluate(status))
            .toList();

    InterfaceStatusResponse response = new InterfaceStatusResponse(list);
    putToCache(key, response);
    return response;
  }
}
