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

package io.mapsmessaging.rest.api.impl.destination;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.DestinationStatusHelper;
import io.mapsmessaging.dto.rest.destination.DestinationDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.DestinationDetailsResponse;
import io.mapsmessaging.rest.responses.DestinationResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Destination Management")
@Path(URI_PATH+"/server/destination")
public class DestinationManagementApi extends BaseDestinationApi {

  @GET
  @Path("/detail")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve detailed information about a destination",
      description = "Fetch detailed information for a specific destination identified by its name. Authentication is required if the server configuration mandates it. Cached results are returned if available to enhance performance.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get destination details was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = DestinationDetailsResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Destination not found"),
      }
  )
  public DestinationDetailsResponse getDestinationDetails(
      @Parameter(
          description = "The name of the destination for which details are requested",
          required = true,
          schema = @Schema(type = "string", example = "destination-01")
      )
      @QueryParam("destinationName") String destinationName
  ) throws ExecutionException, InterruptedException, TimeoutException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), destinationName);
    DestinationDetailsResponse cachedResponse = getFromCache(key, DestinationDetailsResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    DestinationImpl destinationImpl = lookup(destinationName);
    if (destinationImpl == null) {
      throw new WebApplicationException("Destination not found", Response.Status.NOT_FOUND);
    }
    DestinationDetailsResponse result = new DestinationDetailsResponse();
    result.setDestination(DestinationStatusHelper.createDestination(destinationImpl));
    result.setSubscriptionList(destinationImpl.getSubscriptionStates());

    putToCache(key, result);
    return result;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve a list of all destinations with optional filtering and sorting",
      description = "Fetch a paginated list of all known destinations. You can filter the list using a selector string, limit the number of returned entries using the 'size' parameter, and sort the results by attributes such as Name, Published Messages, or Stored Messages. Cached results are returned if available to enhance performance. Authentication is required if the server configuration mandates it.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get all destinations was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = DestinationResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public DestinationResponse getAllDestinations(
      @Parameter(
          description = "An optional filter string for selecting specific destinations. The filter should be a valid expression that complies with the selector syntax.",
          schema = @Schema(type = "string", example = "type = 'topic' AND storedMessages > 50")
      )
      @QueryParam("filter") String filter,
      @Parameter(
          description = "The maximum number of destinations to return in the response. A default value is used if this parameter is not provided.",
          schema = @Schema(type = "int", example = "100", defaultValue = "40")
      )
      @QueryParam("size") @DefaultValue("40") int size,
      @Parameter(
          description = "The attribute by which the list of destinations should be sorted before returning. Possible values include Name, Published, Delivered, Stored, Pending, Delayed, and Expired.",
          schema = @Schema(type = "string", example = "Published", defaultValue = "Published", allowableValues = {"Name", "Published", "Delivered", "Stored", "Pending", "Delayed", "Expired"})
      )
      @QueryParam("sortBy") @DefaultValue("Published") String sortBy
  ) throws ExecutionException, InterruptedException, TimeoutException, ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), ((filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : "") + ":" + sortBy + ":" + size);

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
      if (destination != null && (parser == null || parser.evaluate(destination))) {
        results.add(destination);
      }
    }
    sortDestinationList(results, sortBy);

    // Limit the size of the returned results
    if (size > 0 && size < results.size()) {
      results = results.subList(0, size);
    }

    DestinationResponse destinationResponse = new DestinationResponse(results);
    putToCache(key, destinationResponse);
    return destinationResponse;
  }

  private void sortDestinationList(List<DestinationDTO> destinations, String sortBy) {
    destinations.sort(
        (d1, d2) -> {
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
  }
}
