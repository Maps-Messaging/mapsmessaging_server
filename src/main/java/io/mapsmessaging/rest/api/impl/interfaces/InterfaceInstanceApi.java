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

package io.mapsmessaging.rest.api.impl.interfaces;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.InterfaceInfoHelper;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.interfaces.InterfaceInfoDTO;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.EndPointManager.STATE;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.rest.responses.EndPointDetailResponse;
import io.mapsmessaging.rest.responses.EndPointDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfaceInstanceApi extends BaseInterfaceApi {

  @GET
  @Path("/server/interface/{endpoint}")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the endpoint current status and configuration")
  public InterfaceInfoDTO getInterface(@PathParam("endpoint") String endpointName) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }

    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return InterfaceInfoHelper.fromEndPointManager(endPointManager);
      }
    }
    return null;
  }

  @PUT
  @Path("/server/interface/{endpoint}")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the endpoint current status and configuration")
  public boolean updateInterfaceConfiguration(@PathParam("endpoint") String endpointName, EndPointServerConfigDTO config) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return false;
    }

    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        InterfaceInfoDTO infoDTO = InterfaceInfoHelper.fromEndPointManager(endPointManager);
        return InterfaceInfoHelper.updateConfig(endPointManager, infoDTO);
      }
    }
    return false;
  }


  @GET
  @Path("/server/interface/{endpoint}/connections")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the endpoint current status and configuration")
  public EndPointDetailResponse getInterfaceConnections(@PathParam("endpoint") String endpointName) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }

    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    List<EndPointDetails> endPointDetails = new ArrayList<>();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        for (EndPoint endPoint : endPointManager.getEndPointServer().getActiveEndPoints()) {
          endPointDetails.add(new EndPointDetails(endPointManager.getName(), endPoint));
        }
      }
    }

    return new EndPointDetailResponse(request, endPointDetails);
  }

  @PUT
  @Path("/server/interface/{endpoint}/stop")
  //@ApiOperation(value = "Stops the specified endpoint and closes existing connections")
  public Response stopInterface(@PathParam("endpoint") String endpointName) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    Response response = lookup(endpointName, STATE.STOPPED);
    if(response != null) {
      return response;
    }
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/interface/{endpoint}/start")
  //@ApiOperation(value = "Starts the specified endpoint")
  public Response startInterface(@PathParam("endpoint") String endpointName) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    Response response = lookup(endpointName, STATE.START);
    if(response != null) {
      return response;
    }
    return Response.noContent().build();

  }


  @PUT
  @Path("/server/interface/{endpoint}/resume")
  //@ApiOperation(value = "Resumes the specified endpoint if the endpoint had been paused")
  public Response resumeInterface(@PathParam("endpoint") String endpointName) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    Response response = lookup(endpointName, STATE.RESUME);
    if(response != null) {
      return response;
    }
    return Response.noContent().build();

  }

  @PUT
  @Path("/server/interface/{endpoint}/pause")
  //@ApiOperation(value = "Pauses the specified endpoint, existing connections are maintained but no new connections can be made")
  public Response pauseInterface(@PathParam("endpoint") String endpointName) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    Response response = lookup(endpointName, STATE.PAUSED);
    if(response != null) {
      return response;
    }
    return Response.noContent().build();
  }

  private Response lookup(String endpointName, STATE state){
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return handleRequest(state, endPointManager);
      }
    }
    return null;
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
