package io.mapsmessaging.rest.api.impl;


import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.data.InterfaceDetailResponse;
import io.mapsmessaging.rest.data.InterfaceInfo;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = URI_PATH + "/server/interfaces")
@Path(URI_PATH)
public class InterfaceManagementApi {

  @GET
  @Path("/server/interfaces")
  @Produces({MediaType.APPLICATION_JSON})
  public InterfaceDetailResponse getAllInterfaces() {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    List<InterfaceInfo> protocols = new ArrayList<>();
    for (EndPointManager endPointManager : endPointManagers) {
      InterfaceInfo protocol = new InterfaceInfo(endPointManager);
      protocols.add(protocol);
    }
    InterfaceDetailResponse response = new InterfaceDetailResponse();
    response.setData(protocols);
    return response;
  }


  @GET
  @Path("/server/interfaces/stopAll")
  public Response stopAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().stopAll();
    return Response.ok().build();
  }

  @GET
  @Path("/server/interfaces/startAll")
  public Response startAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().startAll();
    return Response.ok().build();
  }

  @GET
  @Path("/server/interfaces/pauseAll")
  public Response pauseAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().pauseAll();
    return Response.ok().build();
  }


  @GET
  @Path("/server/interfaces/resumeAll")
  public Response resumeAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().resumeAll();
    return Response.ok().build();
  }


}
