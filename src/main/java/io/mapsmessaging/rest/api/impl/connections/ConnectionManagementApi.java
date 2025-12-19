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

package io.mapsmessaging.rest.api.impl.connections;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.EndPointHelper;
import io.mapsmessaging.dto.rest.endpoint.EndPointDetailsDTO;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.rest.api.impl.destination.BaseDestinationApi;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.handler.SessionTracker;
import io.mapsmessaging.rest.responses.EndPointDetailResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Connection Management")
@Path(URI_PATH+"/server/connections")
public class ConnectionManagementApi extends BaseDestinationApi {

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all connections",
      description = "Retrieve a list of all current connections to the server, can be filtered with the optional filter string. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get all connections was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = EndPointSummaryDTO[].class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public EndPointSummaryDTO[] getAllConnections(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "totalOverflow > 10 OR totalUnderflow > 5")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : "");
    EndPointSummaryDTO[] cachedResponse = getFromCache(key, EndPointSummaryDTO[].class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();

    List<EndPointSummaryDTO> total = new ArrayList<>(SessionTracker.getConnections());
    List<EndPointSummaryDTO> endPointDetails =
        endPointManagers.stream()
            .flatMap(endPointManager -> endPointManager.getEndPointServer().getActiveEndPoints().stream()
                .map(endPoint -> EndPointHelper.buildSummaryDTO(endPointManager.getName(), endPoint))
            )
            .filter(endPointDetail -> parser == null || parser.evaluate(endPointDetail))
            .toList();
    total.addAll(endPointDetails);
    EndPointSummaryDTO[] array = total.toArray(new EndPointSummaryDTO[0]);
    putToCache(key, array);
    return array;
  }

  @GET
  @Path("/{connectionId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get connection details for the specified id",
      description = "Retrieve the details of the specified connection id. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get specific connection details was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = EndPointDetailsDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Connection not found"),
      }
  )
  public EndPointDetailsDTO getConnectionDetails(@PathParam("connectionId") String stringId) {
    hasAccess(RESOURCE);
    long connectionId = Long.parseLong(stringId);
    CacheKey key = new CacheKey(uriInfo.getPath(), ""+connectionId);

    // Try to retrieve from cache
    EndPointDetailsDTO cachedResponse = getFromCache(key, EndPointDetailsDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      for (EndPoint endPoint : endPointManager.getEndPointServer().getActiveEndPoints()) {
        if (endPoint.getId() == connectionId) {
          EndPointDetailsDTO dto = buildConnectionDetails(endPointManager.getName(), endPoint);
          putToCache(key, dto);
          return dto;
        }
      }
    }
    EndPointDetailsDTO dto = SessionTracker.getConnection(connectionId);
    if (dto != null) {
      putToCache(key, dto);
      return dto;
    }
    throw new WebApplicationException("Connection not found", Response.Status.NOT_FOUND);
  }

  @DELETE
  @Path("/{connectionId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Close a connection",
      description = "Requests the connection specified be closed. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Close connection  was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Connection not found"),
      }
  )
  public StatusResponse closeSpecificConnection(@PathParam("connectionId") String stringId) {
    hasAccess(RESOURCE);
    long connectionId = Long.parseLong(stringId);
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      for (EndPoint endPoint : endPointManager.getEndPointServer().getActiveEndPoints()) {
        if (endPoint.getId() == connectionId) {
          try {
            endPoint.close();
            return new StatusResponse("success");
          } catch (IOException e) {
            throw new WebApplicationException("Connection close issue:" + e.getMessage(), Response.Status.BAD_REQUEST);
          }
        }
      }
    }
    throw new WebApplicationException("Connection not found", Response.Status.NOT_FOUND);
  }

  private EndPointDetailsDTO buildConnectionDetails(String adapterName, EndPoint endPoint) {
    return EndPointHelper.buildDetailsDTO(adapterName, endPoint);
  }
}
