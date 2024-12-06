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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfacesStatusApi extends BaseInterfaceApi {

  @GET
  @Path("/server/interface/{endpoint}/status")
  @Produces({MediaType.APPLICATION_JSON})
  public InterfaceStatusDTO getInterfaceStatus(@PathParam("endpoint") String endpointName) {
    checkAuthentication();

    if (!hasAccess("interfaces")) {
      throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
    }

    // Create cache key
    CacheKey key = new CacheKey(uriInfo.getPath(), endpointName);

    // Try to retrieve from cache
    InterfaceStatusDTO cachedResponse = getFromCache(key, InterfaceStatusDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        InterfaceStatusDTO response = InterfaceStatusHelper.fromServer(endPointManager.getEndPointServer());
        putToCache(key, response);
        return response;
      }
    }

    return null;
  }

  @GET
  @Path("/server/interface/status")
  @Produces({MediaType.APPLICATION_JSON})
  public InterfaceStatusResponse getAllInterfaceStatus(@QueryParam("filter") String filter) throws ParseException {
    checkAuthentication();

    if (!hasAccess("interfaces")) {
      throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
    }

    // Create cache key
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? ""+filter.hashCode() : "");

    // Try to retrieve from cache
    InterfaceStatusResponse cachedResponse = getFromCache(key, InterfaceStatusResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();

    List<InterfaceStatusDTO> list = endPointManagers.stream()
        .map(endPointManager -> InterfaceStatusHelper.fromServer(endPointManager.getEndPointServer()))
        .filter(status -> parser == null || parser.evaluate(status))
        .collect(Collectors.toList());

    InterfaceStatusResponse response = new InterfaceStatusResponse(list);
    putToCache(key, response);
    return response;
  }

}
