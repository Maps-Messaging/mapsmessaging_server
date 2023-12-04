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
import io.mapsmessaging.network.EndPointManager.STATE;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.InterfaceInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

//@Api(value = URI_PATH + "/server/interface", tags="Server Interface Management")
@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfaceInstanceApi extends BaseRestApi {

  @GET
  @Path("/server/interface/{endpoint}")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the endpoint current status and configuration")
  public InterfaceInfo getInterface(@PathParam("endpoint") String endpointName) {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return new InterfaceInfo(endPointManager);
      }
    }
    return null;
  }


  @PUT
  @Path("/server/interface/{endpoint}/stop")
  //@ApiOperation(value = "Stops the specified endpoint and closes existing connections")
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

  @PUT
  @Path("/server/interface/{endpoint}/start")
  //@ApiOperation(value = "Starts the specified endpoint")
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


  @PUT
  @Path("/server/interface/{endpoint}/resume")
  //@ApiOperation(value = "Resumes the specified endpoint if the endpoint had been paused")
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

  @PUT
  @Path("/server/interface/{endpoint}/pause")
  //@ApiOperation(value = "Pauses the specified endpoint, existing connections are maintained but no new connections can be made")
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
