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

package io.mapsmessaging.rest.api.impl.interfaces;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.InterfaceStatusHelper;
import io.mapsmessaging.dto.rest.interfaces.InterfaceStatusDTO;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.InterfaceStatusResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfacesStatusApi extends BaseInterfaceApi {

  @GET
  @Path("/server/interface/{endpoint}/status")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get endpoint status",
      description = "Get the current status and metrics for the specified endpoint. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Endpoint status returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Endpoint not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public InterfaceStatusDTO getInterfaceStatus(@PathParam("endpoint") String endpointName) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), endpointName);
    InterfaceStatusDTO cachedResponse = getFromCache(key, InterfaceStatusDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    List<EndPointManager> endPointManagers =
        MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        InterfaceStatusDTO responseDTO =
            InterfaceStatusHelper.fromServer(endPointManager.getEndPointServer());
        putToCache(key, responseDTO);
        return responseDTO;
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return null;
  }

  @GET
  @Path("/server/interface/status")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all endpoint statuses",
      description = "Retrieves the status of all endpoints, optionally filtered. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Endpoint statuses returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public InterfaceStatusResponse getAllInterfaceStatus(
      @Parameter(
          description = "Optional filter string",
          schema = @Schema(type = "string", example = "state = 'started'")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key =
        new CacheKey(
            uriInfo.getPath(),
            (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : ""
        );

    InterfaceStatusResponse cachedResponse = getFromCache(key, InterfaceStatusResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    ParserExecutor parser =
        (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers =
        MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();

    List<InterfaceStatusDTO> list =
        endPointManagers.stream()
            .map(endPointManager -> InterfaceStatusHelper.fromServer(endPointManager.getEndPointServer()))
            .filter(status -> parser == null || parser.evaluate(status))
            .collect(Collectors.toList());

    InterfaceStatusResponse responseDTO = new InterfaceStatusResponse(list);
    putToCache(key, responseDTO);
    return responseDTO;
  }
}
