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

package io.mapsmessaging.rest.api.impl.integration;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.integration.IntegrationStatusDTO;
import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.IntegrationListStatus;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Integration Status")
@Path(URI_PATH)
public class IntergrationStatusApi extends IntegrationBaseRestApi {

  public static IntegrationStatusDTO fromConnection(EndPointConnection connection) {
    IntegrationStatusDTO dto = new IntegrationStatusDTO();
    dto.setState(connection.getState().getName());
    dto.setInterfaceName(connection.getConfigName());
    dto.setBytesSent(connection.getTotalBytesSent());
    dto.setBytesReceived(connection.getTotalBytesRead());
    dto.setMessagesSent(connection.getTotalPacketsSent());
    dto.setMessagesReceived(connection.getTotalPacketsRead());
    dto.setErrors(connection.getTotalErrors());

    // Initialize statistics and timestamps if connection details are available
    Map<String, LinkedMovingAverageRecordDTO> stats = new LinkedHashMap<>();
    if (connection.getProtocol() != null && connection.getProtocol().getEndPoint() != null) {
      dto.setLastReadTime(connection.getProtocol().getEndPoint().getLastRead());
      dto.setLastWriteTime(connection.getProtocol().getEndPoint().getLastWrite());
    }

    // Populate statistics
    addToMap(stats, connection.getAverageBytesRead());
    addToMap(stats, connection.getAverageBytesSent());
    addToMap(stats, connection.getAveragePacketsRead());
    addToMap(stats, connection.getAveragePacketsSent());
    dto.setStatistics(stats);

    return dto;
  }

  private static void addToMap(
      Map<String, LinkedMovingAverageRecordDTO> stats, LinkedMovingAverageRecordDTO average) {
    if (average != null) {
      stats.put(average.getName(), average);
    }
  }

  @GET
  @Path("/server/integration/{endpoint}/status")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get inter-server status",
      description = "Retrieve the current status for the inter-server specified by name. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationStatusDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public IntegrationStatusDTO getIntegrationStatus(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), endpointName);
    IntegrationStatusDTO cachedResponse = getFromCache(key, IntegrationStatusDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    List<EndPointConnection> endPointManagers =
        MessageDaemon.getInstance()
            .getSubSystemManager()
            .getNetworkConnectionManager()
            .getEndPointConnectionList();
    for (EndPointConnection endPointConnection : endPointManagers) {
      if (endpointName.equals(endPointConnection.getConfigName())) {
        IntegrationStatusDTO response = fromConnection(endPointConnection);
        putToCache(key, response);
        return response;
      }
    }

    return null;
  }

  @GET
  @Path("/server/integration/status")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all inter-server status",
      description = "Retrieve all current statuses for the inter-server. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationListStatus.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public IntegrationListStatus getAllIntegrationStatus(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "state = PAUSED")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : "");
    IntegrationListStatus cachedResponse = getFromCache(key, IntegrationListStatus.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointConnection> endPointManagers =
        MessageDaemon.getInstance()
            .getSubSystemManager()
            .getNetworkConnectionManager()
            .getEndPointConnectionList();

    List<IntegrationStatusDTO> response =
        endPointManagers.stream()
            .map(IntergrationStatusApi::fromConnection)
            .filter(status -> parser == null || parser.evaluate(status))
            .collect(Collectors.toList());
    IntegrationListStatus status = new IntegrationListStatus(response);
    putToCache(key, status);
    return status;
  }
}
