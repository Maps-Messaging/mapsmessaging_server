/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.rest.api.impl;


import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.rest.data.Destination;
import io.mapsmessaging.rest.responses.DestinationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Destination Management")
@Path(URI_PATH)
public class DestinationManagementApi extends BaseDestinationApi {

  @GET
  @Path("/server/destination/{destination}")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public DestinationResponse getDestination(@PathParam("destination") String destinationName) throws ExecutionException, InterruptedException, TimeoutException, IOException {
    Destination destination = lookupDestination(destinationName);
    return new DestinationResponse(request, destination);
  }

  @GET
  @Path("/server/destination")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get all the destination configuration")
  public DestinationResponse getAllDestinations() throws IOException, ExecutionException, InterruptedException, TimeoutException {
    List<String> destinations = MessageDaemon.getInstance().getDestinationManager().getAll();
    List<Destination> results  = new ArrayList<>();
    for(String name:destinations){
      results.add(lookupDestination(name));
    }
    return new DestinationResponse(request, results);
  }

  protected Destination lookupDestination(String name) throws IOException, ExecutionException, InterruptedException, TimeoutException {
    DestinationImpl destinationImpl = super.lookup(name);
    if(destinationImpl == null){
      return null;
    }
    return new Destination(destinationImpl);
  }
}
