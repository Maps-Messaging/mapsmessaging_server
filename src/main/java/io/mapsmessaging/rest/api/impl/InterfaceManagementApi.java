package io.mapsmessaging.rest.api.impl;


import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.api.BaseRestApi;
import io.mapsmessaging.rest.data.InterfaceDetailResponse;
import io.mapsmessaging.rest.data.InterfaceInfo;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = URI_PATH + "/server/interfaces")
@Path(URI_PATH)
public class InterfaceManagementApi extends BaseRestApi {

  @GET
  @Path("/server/interfaces")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Retrieve a list of all configured interfaces")
  public InterfaceDetailResponse getAllInterfaces() {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    List<InterfaceInfo> protocols = new ArrayList<>();
    ConfigurationProperties global = null;
    for (EndPointManager endPointManager : endPointManagers) {
      InterfaceInfo protocol = new InterfaceInfo(endPointManager);
      protocols.add(protocol);
      if(global == null){
        global = endPointManager.getEndPointServer().getConfig().getProperties().getGlobal();
      }
    }
    InterfaceDetailResponse interfaceDetailResponse = new InterfaceDetailResponse();
    interfaceDetailResponse.setData(protocols);
    interfaceDetailResponse.setGlobalConfig(global);
    return interfaceDetailResponse;
  }


  @PUT
  @Path("/server/interfaces/stopAll")
  @ApiOperation(value = "Stops all all configured interfaces")
  public Response stopAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().stopAll();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/interfaces/startAll")
  @ApiOperation(value = "Starts all all configured interfaces")
  public Response startAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().startAll();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/interfaces/pauseAll")
  @ApiOperation(value = "Pauses all all configured interfaces")
  public Response pauseAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().pauseAll();
    return Response.ok().build();
  }


  @PUT
  @Path("/server/interfaces/resumeAll")
  @ApiOperation(value = "Resumes all all configured interfaces")
  public Response resumeAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().resumeAll();
    return Response.ok().build();
  }
}
