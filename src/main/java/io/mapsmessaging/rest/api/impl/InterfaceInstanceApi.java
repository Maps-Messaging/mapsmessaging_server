package io.mapsmessaging.rest.api.impl;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.EndPointManager.STATE;
import io.mapsmessaging.rest.data.InterfaceInfo;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = URI_PATH + "/server/interface")
@Path(URI_PATH)
public class InterfaceInstanceApi {

  @GET
  @Path("/server/interface/{endpoint}")
  @Produces({MediaType.APPLICATION_JSON})
  public InterfaceInfo getInterface(@PathParam("endpoint") String endpointName) {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return new InterfaceInfo(endPointManager);
      }
    }
    return null;
  }


  @GET
  @Path("/server/interface/{endpoint}/stop")
  public Response stopInterface(@PathParam("endpoint") String endpointName) {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return handleRequest(STATE.STOPPED, endPointManager);
      }
    }
    return Response.noContent()
        .build();
  }

  @GET
  @Path("/server/interface/{endpoint}/start")
  public Response startInterface(@PathParam("endpoint") String endpointName) {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return handleRequest(STATE.START, endPointManager);
      }
    }
    return Response.noContent()
        .build();
  }


  @GET
  @Path("/server/interface/{endpoint}/resume")
  public Response resumeInterface(@PathParam("endpoint") String endpointName) {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return handleRequest(STATE.RESUME, endPointManager);
      }
    }
    return Response.noContent()
        .build();
  }

  @GET
  @Path("/server/interface/{endpoint}/pause")
  public Response pauseInterface(@PathParam("endpoint") String endpointName) {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return handleRequest(STATE.PAUSED, endPointManager);
      }
    }
    return Response.noContent()
        .build();
  }

  private boolean isMatch(String name, EndPointManager endPointManager){
    return (endPointManager.getEndPointServer().getConfig().getProperties().getProperty("name").equals(name));
  }

  private Response handleRequest(STATE newState, EndPointManager endPointManager) {

    try {
      if (newState == STATE.START && endPointManager.getState() == STATE.STOPPED) {
        endPointManager.start();
        return Response.ok()
            .build();
      } else if (newState == STATE.STOPPED &&
          (endPointManager.getState() == STATE.START || endPointManager.getState() == STATE.PAUSED)) {
        endPointManager.close();
        return Response.ok()
            .build();
      } else if (newState == STATE.RESUME && endPointManager.getState() == STATE.PAUSED) {
        endPointManager.resume();
        return Response.ok()
            .build();
      } else if (newState == STATE.PAUSED && endPointManager.getState() == STATE.START) {
        endPointManager.pause();
        return Response.ok()
            .build();
      }
    } catch (IOException e) {
      return Response.serverError()
          .entity(e)
          .build();
    }
    return Response.noContent()
        .build();
  }
}
