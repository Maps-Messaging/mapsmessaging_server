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

package io.mapsmessaging.rest.api.impl.interfaces;


import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.InterfaceInfo;
import io.mapsmessaging.rest.responses.InterfaceDetailResponse;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfaceManagementApi extends BaseRestApi {

  @GET
  @Path("/server/interfaces")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Retrieve a list of all configured interfaces")
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
    return new InterfaceDetailResponse(request, protocols, global);
  }


  @PUT
  @Path("/server/interfaces/stopAll")
  //@ApiOperation(value = "Stops all all configured interfaces")
  public Response stopAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().stopAll();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/interfaces/startAll")
  //@ApiOperation(value = "Starts all all configured interfaces")
  public Response startAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().startAll();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/interfaces/pauseAll")
  //@ApiOperation(value = "Pauses all all configured interfaces")
  public Response pauseAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().pauseAll();
    return Response.ok().build();
  }


  @PUT
  @Path("/server/interfaces/resumeAll")
  //@ApiOperation(value = "Resumes all all configured interfaces")
  public Response resumeAllInterfaces() {
    MessageDaemon.getInstance().getNetworkManager().resumeAll();
    return Response.ok().build();
  }
}
