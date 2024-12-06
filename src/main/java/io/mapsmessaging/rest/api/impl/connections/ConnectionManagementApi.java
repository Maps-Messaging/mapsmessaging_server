/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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
 *
 */

package io.mapsmessaging.rest.api.impl.connections;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.EndPointHelper;
import io.mapsmessaging.dto.rest.endpoint.EndPointDetailsDTO;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.rest.api.impl.destination.BaseDestinationApi;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.EndPointDetailResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Connection Management")
@Path(URI_PATH)
public class ConnectionManagementApi extends BaseDestinationApi {

  @GET
  @Path("/server/connections")
  @Produces({MediaType.APPLICATION_JSON})
  public EndPointDetailResponse getAllConnections(@QueryParam("filter") String filter) throws ParseException {
    if (!hasAccess("connections")) {
      throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
    }

    // Create cache key
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? ""+filter.hashCode() : "");

    // Try to retrieve from cache
    EndPointDetailResponse cachedResponse = getFromCache(key, EndPointDetailResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();

    List<EndPointSummaryDTO> endPointDetails =
        endPointManagers.stream()
            .flatMap(endPointManager ->
                endPointManager.getEndPointServer().getActiveEndPoints().stream().map(endPoint -> EndPointHelper.buildSummaryDTO(endPointManager.getName(), endPoint)))
            .filter(endPointDetail -> parser == null || parser.evaluate(endPointDetail))
            .collect(Collectors.toList());

    EndPointDetailResponse response = new EndPointDetailResponse(endPointDetails);
    putToCache(key, response);
    return response;
  }

  @GET
  @Path("/server/connection")
  @Produces({MediaType.APPLICATION_JSON})
  public EndPointDetailsDTO getConnectionDetails(@QueryParam("connectionId")  String connectionId) {
    if (!hasAccess("connections")) {
      throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
    }

    CacheKey key = new CacheKey(uriInfo.getPath(), connectionId);

    // Try to retrieve from cache
    EndPointDetailsDTO cachedResponse = getFromCache(key, EndPointDetailsDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for(EndPointManager endPointManager : endPointManagers) {
      for(EndPoint endPoint: endPointManager.getEndPointServer().getActiveEndPoints()){
        if(endPoint.getName().equals(connectionId)) {
          EndPointDetailsDTO dto = buildConnectionDetails(endPointManager.getName(), endPoint);
          putToCache(key, dto);
          return dto;
        }
      }
    }
    throw new WebApplicationException("Connection not found", Response.Status.NOT_FOUND);
  }


  @PUT
  @Path("/server/connection")
  @Produces({MediaType.APPLICATION_JSON})
  public Response closeSpecificConnection(@QueryParam("connectionId")  String connectionId) {
    if (!hasAccess("connections")) {
      throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
    }
    // Fetch and cache response
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for(EndPointManager endPointManager : endPointManagers) {
      for(EndPoint endPoint: endPointManager.getEndPointServer().getActiveEndPoints()){
        if(endPoint.getName().equals(connectionId)) {
          try {
            endPoint.close();
            return Response.ok().build();
          } catch (IOException e) {
            throw new WebApplicationException("Connection close issue:"+e.getMessage(), Response.Status.BAD_REQUEST);
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
