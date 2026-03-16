/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Connection Management")
@Path(URI_PATH + "/server/connections")
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
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
      }
  )
  public EndPointSummaryDTO[] getAllConnections(
      @Parameter(
          description = "Optional filter string",
          schema = @Schema(type = "String", example = "totalOverflow > 10 OR totalUnderflow > 5")
      )
      @QueryParam("filter") String filter
  ) {
    hasAccess(RESOURCE);

    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isBlank()) ? "" + filter.hashCode() : "");
    EndPointSummaryDTO[] cachedResponse = getFromCache(key, EndPointSummaryDTO[].class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    ParserExecutor parserExecutor = null;
    if (filter != null && !filter.isBlank()) {
      try {
        parserExecutor = SelectorParser.compile(filter);
      } catch (ParseException parseException) {
        throw new WebApplicationException(
            Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new StatusResponse("Invalid filter: " + parseException.getMessage()))
                .build()
        );
      }
    }

    List<EndPointSummaryDTO> total = new ArrayList<>(SessionTracker.getConnections());
    List<EndPointSummaryDTO> endPointDetails = generateList(parserExecutor, MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll());
    total.addAll(endPointDetails);
    EndPointSummaryDTO[] array = total.toArray(new EndPointSummaryDTO[0]);
    putToCache(key, array);
    return array;
  }

  private List<EndPointSummaryDTO> generateList(ParserExecutor finalParserExecutor, List<EndPointManager> endPointManagers){
    List<EndPointSummaryDTO> endPointDetails = new ArrayList<>();
    for(EndPointManager endPointManager : endPointManagers){
      for(EndPoint endPoint : endPointManager.getEndPointServer().getActiveEndPoints()) {
        EndPointSummaryDTO summaryDTO = EndPointHelper.buildSummaryDTO(endPointManager.getName(), endPoint);
        if(finalParserExecutor == null || finalParserExecutor.evaluate(summaryDTO)) {
          endPointDetails.add(summaryDTO);
        }
      }
    }
    return endPointDetails;
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
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(
              responseCode = "404",
              description = "Connection not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public EndPointDetailsDTO getConnectionDetails(@PathParam("connectionId") String stringId) {
    hasAccess(RESOURCE);

    long connectionId = parseConnectionId(stringId);
    CacheKey key = new CacheKey(uriInfo.getPath(), "" + connectionId);

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

    throw new WebApplicationException(
        Response.status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(new StatusResponse("Connection not found"))
            .build()
    );
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
              description = "Close connection was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(
              responseCode = "404",
              description = "Connection not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public StatusResponse closeSpecificConnection(@PathParam("connectionId") String stringId) {
    hasAccess(RESOURCE);

    long connectionId = parseConnectionId(stringId);

    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      for (EndPoint endPoint : endPointManager.getEndPointServer().getActiveEndPoints()) {
        if (endPoint.getId() == connectionId) {
          try {
            endPoint.close();
            return new StatusResponse("success");
          } catch (IOException exception) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new StatusResponse("Connection close issue: " + exception.getMessage()))
                    .build()
            );
          }
        }
      }
    }

    throw new WebApplicationException(
        Response.status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(new StatusResponse("Connection not found"))
            .build()
    );
  }

  private long parseConnectionId(String stringId) {
    if (stringId == null || stringId.isBlank()) {
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .type(MediaType.APPLICATION_JSON)
              .entity(new StatusResponse("connectionId is required"))
              .build()
      );
    }
    try {
      return Long.parseLong(stringId);
    } catch (NumberFormatException exception) {
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .type(MediaType.APPLICATION_JSON)
              .entity(new StatusResponse("Invalid connectionId"))
              .build()
      );
    }
  }

  private EndPointDetailsDTO buildConnectionDetails(String adapterName, EndPoint endPoint) {
    return EndPointHelper.buildDetailsDTO(adapterName, endPoint);
  }
}
