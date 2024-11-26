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

package io.mapsmessaging.rest.api.impl.destination;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.DestinationStatusHelper;
import io.mapsmessaging.dto.rest.destination.DestinationStatusDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.DestinationStatusResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Tag(name = "Destination Management")
@Path(URI_PATH)
public class DestinationStatusApi extends BaseDestinationApi {
  @GET
  @Path("/server/destination/status/byname")
  @Produces({MediaType.APPLICATION_JSON})
  public DestinationStatusDTO getDestinationStats(@QueryParam("destinationName")String destinationName) throws ExecutionException, InterruptedException, TimeoutException {
    try {
      checkAuthentication();

      if (!hasAccess("destinations")) {
        throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
      }
      if(destinationName==null) {
        throw new WebApplicationException("Destination not found", Response.Status.NOT_FOUND);
      }
      // Create cache key
      CacheKey key = new CacheKey(uriInfo.getPath(), destinationName);

      // Try to retrieve from cache
      DestinationStatusDTO cachedResponse = getFromCache(key, DestinationStatusDTO.class);
      if (cachedResponse != null) {
        return cachedResponse;
      }

      // Fetch and cache response
      DestinationStatusDTO destination = lookupDestination(destinationName);
      if (destination == null) {
        throw new WebApplicationException("Destination not found", Response.Status.NOT_FOUND);
      }

      putToCache(key, destination);
      return destination;
    } catch (Throwable e) {
      e.printStackTrace();
      throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/server/destination/status")
  @Produces({MediaType.APPLICATION_JSON})
  public DestinationStatusResponse getAllStatsForDestinations(@QueryParam("filter") String filter) throws ExecutionException, InterruptedException, TimeoutException, ParseException {
    checkAuthentication();

    if (!hasAccess("destinations")) {
      throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
    }

    // Create cache key
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? ""+filter.hashCode() : "");

    // Try to retrieve from cache
    DestinationStatusResponse cachedResponse = getFromCache(key, DestinationStatusResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<String> destinations = MessageDaemon.getInstance().getDestinationManager().getAll();
    List<DestinationStatusDTO> results = new ArrayList<>();
    for (String name : destinations) {
      DestinationStatusDTO destinationStatus = lookupDestination(name);
      if (parser == null || parser.evaluate(destinationStatus)) {
        results.add(destinationStatus);
      }
    }

    DestinationStatusResponse response = new DestinationStatusResponse(request, results);
    putToCache(key, response);
    return response;
  }

  protected DestinationStatusDTO lookupDestination(String name) throws ExecutionException, InterruptedException, TimeoutException {
    DestinationImpl destinationImpl = super.lookup(name);
    if(destinationImpl == null){
      return null;
    }
    return DestinationStatusHelper.createDestinationStatus(destinationImpl);
  }

}