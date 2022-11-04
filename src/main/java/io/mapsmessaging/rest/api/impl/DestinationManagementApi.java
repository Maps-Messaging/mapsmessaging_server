package io.mapsmessaging.rest.api.impl;


import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.rest.data.Destination;
import io.mapsmessaging.rest.responses.DestinationResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = URI_PATH + "/server/destination", tags="Destination Management")
@Path(URI_PATH)
public class DestinationManagementApi extends BaseDestinationApi {

  @GET
  @Path("/server/destination/{destination}")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get the specific destination details")
  public DestinationResponse getDestination(@PathParam("destination") String destinationName) throws ExecutionException, InterruptedException, TimeoutException, IOException {
    Destination destination = lookupDestination(destinationName);
    return new DestinationResponse(request, destination);
  }

  @GET
  @Path("/server/destination")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get all the destination configuration")
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
