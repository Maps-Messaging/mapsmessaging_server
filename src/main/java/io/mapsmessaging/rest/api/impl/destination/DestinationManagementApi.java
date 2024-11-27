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
import io.mapsmessaging.dto.rest.destination.DestinationDTO;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.DestinationResponse;
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
public class DestinationManagementApi extends BaseDestinationApi {

  @GET
  @Path("/server/destination")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public DestinationDTO getDestination(@QueryParam("destinationName")String destinationName) throws ExecutionException, InterruptedException, TimeoutException {
    checkAuthentication();
    if (!hasAccess("destinations")) {
      response.setStatus(403);
      return null;
    }

    CacheKey key = new CacheKey(uriInfo.getPath(), destinationName);

    // Try to retrieve from cache
    DestinationDTO cachedResponse = getFromCache(key, DestinationDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    DestinationDTO destination = DestinationStatusHelper.createDestination(lookup(destinationName));
    if(destination == null) {
      throw new WebApplicationException("Destination not found", Response.Status.NOT_FOUND);
    }
    putToCache(key, destination);
    return destination;
  }

  @GET
  @Path("/server/destinations")
  @Produces({MediaType.APPLICATION_JSON})
  public DestinationResponse getAllDestinations(
      @QueryParam("filter") String filter,
      @QueryParam("size") @DefaultValue("40") int size,
      @QueryParam("sortBy") @DefaultValue("Published") String sortBy)
      throws ExecutionException, InterruptedException, TimeoutException, ParseException {

    checkAuthentication();
    if (!hasAccess("destinations")) {
      response.setStatus(403);
      return null;
    }

    // Create cache key
    CacheKey key = new CacheKey(uriInfo.getPath(), ((filter != null && !filter.isEmpty()) ? ""+filter.hashCode() : "")+":"+sortBy+":"+size );

    // Try to retrieve from cache
    DestinationResponse cachedResponse = getFromCache(key, DestinationResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }


    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<String> destinations = MessageDaemon.getInstance().getDestinationManager().getAll();
    List<DestinationDTO> results = new ArrayList<>();

    for (String name : destinations) {
      DestinationDTO destination = DestinationStatusHelper.createDestination(lookup(name));
      if (parser == null || parser.evaluate(destination)) {
        if (destination != null) {
          results.add(destination);
        }
      }
    }

    // Sort by specified field
    results.sort((d1, d2) -> {
      switch (sortBy) {
        case "Name":
          return d2.getName().compareTo(d1.getName());
        case "Published":
          return Long.compare(d2.getPublishedMessages(), d1.getPublishedMessages());
        case "Delivered":
          return Long.compare(d2.getDeliveredMessages(), d1.getDeliveredMessages());
        case "Stored":
          return Long.compare(d2.getStoredMessages(), d1.getStoredMessages());
        case "Pending":
          return Long.compare(d2.getPendingMessages(), d1.getPendingMessages());
        case "Delayed":
          return Long.compare(d2.getDelayedMessages(), d1.getDelayedMessages());
        case "Expired":
          return Long.compare(d2.getExpiredMessages(), d1.getExpiredMessages());
        default:
          throw new IllegalArgumentException("Invalid sortBy parameter: " + sortBy);
      }
    });


    // Limit the size of the returned results
    if (size > 0 && size < results.size()) {
      results = results.subList(0, size);
    }
    for(DestinationDTO destination : results) {
      System.out.println(destination.getName()+":"+destination.getStoredMessages());
    }
    DestinationResponse destinationResponse =  new DestinationResponse(request, results);
    putToCache(key, destinationResponse);
    return destinationResponse;
  }
}
