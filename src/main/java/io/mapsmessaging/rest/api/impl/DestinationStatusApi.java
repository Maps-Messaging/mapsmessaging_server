package io.mapsmessaging.rest.api.impl;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.rest.data.DestinationStatus;
import io.mapsmessaging.rest.responses.DestinationStatusResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Api(value = URI_PATH + "/server/destination/status", tags="Destination Status Management")
@Path(URI_PATH)
public class DestinationStatusApi extends BaseDestinationApi {

  @GET
  @Path("/server/destination/status/{destination}")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get the specific destination details")
  public DestinationStatusResponse getDestination(@PathParam("destination") String destinationName) throws ExecutionException, InterruptedException, TimeoutException {
    DestinationStatus destination = lookupDestination(destinationName);
    return new DestinationStatusResponse(request, destination);
  }

  @GET
  @Path("/server/destination/status")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get all the destination configuration")
  public DestinationStatusResponse getAllDestinations() throws ExecutionException, InterruptedException, TimeoutException {
    List<String> destinations = MessageDaemon.getInstance().getDestinationManager().getAll();
    List<DestinationStatus> results = new ArrayList<>();
    for (String name : destinations) {
      results.add(lookupDestination(name));
    }
    return new DestinationStatusResponse(request, results);
  }
  protected DestinationStatus lookupDestination(String name) throws ExecutionException, InterruptedException, TimeoutException {
    DestinationImpl destinationImpl = super.lookup(name);
    if(destinationImpl == null){
      return null;
    }
    return new DestinationStatus(destinationImpl);
  }

}