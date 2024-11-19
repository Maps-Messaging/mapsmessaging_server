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
import io.mapsmessaging.dto.rest.destination.DestinationDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.rest.responses.DestinationResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Tag(name = "Destination Management")
@Path(URI_PATH)
public class DestinationManagementApi extends BaseDestinationApi {

  @GET
  @Path("/server/destination/{destination}")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public DestinationResponse getDestination(@PathParam("destination") String destinationName) throws ExecutionException, InterruptedException, TimeoutException, IOException {
    checkAuthentication();
    if (!hasAccess("destinations")) {
      response.setStatus(403);
      return null;
    }
    DestinationDTO destination = lookupDestination(destinationName);
    return new DestinationResponse(request, destination);
  }

  @GET
  @Path("/server/destination")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get all the destination configuration")
  public DestinationResponse getAllDestinations(@QueryParam("filter") String filter) throws IOException, ExecutionException, InterruptedException, TimeoutException, ParseException {
    checkAuthentication();
    if (!hasAccess("destinations")) {
      response.setStatus(403);
      return null;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty())  ? SelectorParser.compile(filter) : null;
    List<String> destinations = MessageDaemon.getInstance().getDestinationManager().getAll();
    List<DestinationDTO> results  = new ArrayList<>();
    for(String name:destinations){
      DestinationDTO destination = lookupDestination(name);
      if(parser == null || parser.evaluate(destination)) {
        results.add(destination);
      }
    }
    return new DestinationResponse(request, results);
  }

  protected DestinationDTO lookupDestination(String name) throws IOException, ExecutionException, InterruptedException, TimeoutException {
    DestinationImpl destinationImpl = super.lookup(name);
    if(destinationImpl == null){
      return null;
    }
    return new DestinationDTO(
        destinationImpl.getFullyQualifiedNamespace(),
        destinationImpl.getResourceType().getName(),
        destinationImpl.getStoredMessages(),
        destinationImpl.getDelayedMessages(),
        destinationImpl.getPendingTransactions(),
        destinationImpl.getSchema().getUniqueId()
    );
  }
}
